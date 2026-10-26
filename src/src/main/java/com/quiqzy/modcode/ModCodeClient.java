package com.quiqzy.modcode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ModCodeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("modcode");
    private static final URI SCRIPT_URI = URI.create(
            "https://raw.githubusercontent.com/Quiqzy404/mod-code/main/modcode.script.json");
    private static final int MAX_SCRIPT_BYTES = 32_768;
    private static final int MAX_ACTIONS = 32;
    private static final int MAX_MESSAGE_LENGTH = 240;
    private static final ConcurrentLinkedQueue<String> CHAT_QUEUE = new ConcurrentLinkedQueue<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            String message;
            while (client.player != null && (message = CHAT_QUEUE.poll()) != null) {
                client.player.sendMessage(Text.literal(message), false);
            }
        });
        LOGGER.info("Mod Code loaded. Fetching the restricted action script from GitHub.");
        Thread.ofVirtual().name("modcode-script-fetch").start(ModCodeClient::fetchAndRunScript);
    }

    private static void fetchAndRunScript() {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(SCRIPT_URI)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "ModCode/1.0 Minecraft-Mod")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                LOGGER.warn("Could not load GitHub script (HTTP {}).", response.statusCode());
                return;
            }
            byte[] body = response.body();
            if (body.length > MAX_SCRIPT_BYTES) {
                LOGGER.warn("GitHub script rejected: file exceeds {} bytes.", MAX_SCRIPT_BYTES);
                return;
            }
            runRestrictedJson(new String(body, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception exception) {
            LOGGER.warn("Could not load the GitHub script; Minecraft will continue normally.", exception);
        }
    }

    private static void runRestrictedJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray actions = root.getAsJsonArray("actions");
        if (actions == null || actions.size() > MAX_ACTIONS) {
            LOGGER.warn("GitHub script rejected: expected at most {} actions.", MAX_ACTIONS);
            return;
        }
        for (JsonElement element : actions) {
            if (!element.isJsonObject()) continue;
            JsonObject action = element.getAsJsonObject();
            String type = action.has("type") ? action.get("type").getAsString() : "";
            String message = action.has("message") ? action.get("message").getAsString() : "";
            if (message.length() > MAX_MESSAGE_LENGTH) {
                LOGGER.warn("Skipping script action with an overlong message.");
                continue;
            }
            switch (type) {
                case "log" -> LOGGER.info("[GitHub script] {}", message);
                case "chat" -> CHAT_QUEUE.add(message);
                default -> LOGGER.warn("Skipping unsupported script action type: {}", type);
            }
        }
    }
}
