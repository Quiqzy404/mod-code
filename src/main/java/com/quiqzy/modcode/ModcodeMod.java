package com.quiqzy.modcode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModcodeMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("modcode");
    private static final AtomicBoolean RUN_ONCE = new AtomicBoolean(false);
    public static final String SCRIPT_URL = "https://raw.githubusercontent.com/Quiqzy404/mod-code/main/modcode.script.json";

    @Override
    public void onInitialize() {
        LOGGER.info("modcode initialized");
        ScriptRunner.fetchAndRunOnce(SCRIPT_URL);
    }

    public static final class ScriptRunner {
        public static void fetchAndRunOnce(String scriptUrl) {
            if (!RUN_ONCE.compareAndSet(false, true)) {
                LOGGER.info("Script already executed once; skipping repeated run.");
                return;
            }

            try {
                String json = downloadScript(scriptUrl);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonArray actions = root.has("actions") && root.get("actions").isJsonArray()
                        ? root.getAsJsonArray("actions")
                        : null;

                if (actions == null) {
                    LOGGER.warn("Script {} has no valid 'actions' array. Nothing to execute.", scriptUrl);
                    return;
                }

                for (JsonElement actionElement : actions) {
                    executeAction(actionElement);
                }
            } catch (Exception e) {
                LOGGER.warn("Could not load or parse modcode script at {}. Starting Minecraft without script actions. {}", scriptUrl, e.getMessage());
            }
        }

        private static String downloadScript(String scriptUrl) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(scriptUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP status " + response.statusCode());
            }
            return response.body();
        }

        private static void executeAction(JsonElement actionElement) {
            if (!actionElement.isJsonObject()) {
                LOGGER.warn("Ignoring non-object script action: {}", actionElement);
                return;
            }

            JsonObject action = actionElement.getAsJsonObject();
            String type = getString(action, "type");
            String message = getString(action, "message");

            if (type == null || type.isBlank()) {
                LOGGER.warn("Skipping action without a valid 'type': {}", action);
                return;
            }

            switch (type) {
                case "log" -> LOGGER.info("[modcode script] {}", message);
                case "chat" -> {
                    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            client.inGameHud.getChatHud().addMessage(Text.literal(message));
                        } else {
                            LOGGER.info("[modcode script chat] {}", message);
                        }
                    } else {
                        LOGGER.info("[modcode script chat] {}", message);
                    }
                }
                default -> LOGGER.warn("Unsupported script action type '{}'. Allowed actions are 'log' and 'chat'.", type);
            }
        }

        private static String getString(JsonObject object, String key) {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
        }
    }
}
