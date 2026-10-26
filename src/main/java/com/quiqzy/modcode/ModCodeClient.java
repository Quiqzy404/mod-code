package com.quiqzy.modcode;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public final class ModCodeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("modcode");
    private static final URI SCRIPT_URI = URI.create(
            "https://raw.githubusercontent.com/Quiqzy404/mod-code/main/scripts/main.js");
    private static final int MAX_SCRIPT_BYTES = 256 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ScheduledExecutorService SCRIPT_POLLER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "modcode-script-poller");
        thread.setDaemon(true);
        return thread;
    });
    private static final ContextFactory SCRIPT_CONTEXTS = new ContextFactory() {
        @Override
        protected Context makeContext() {
            Context context = super.makeContext();
            context.setOptimizationLevel(-1);
            context.setInstructionObserverThreshold(10_000);
            return context;
        }

        @Override
        protected void observeInstructionCount(Context context, int instructionCount) {
            throw new EvaluatorException("Script exceeded its execution limit");
        }
    };
    private volatile String lastSuccessfulHash = "";

    @Override
    public void onInitializeClient() {
        LOGGER.info("Mod Code is running. Checking GitHub for script updates every 20 seconds.");
        SCRIPT_POLLER.scheduleWithFixedDelay(this::checkForScript, 0, 20, TimeUnit.SECONDS);
    }

    private void checkForScript() {
        try {
            HttpRequest request = HttpRequest.newBuilder(SCRIPT_URI)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "ModCode-Fabric/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                LOGGER.warn("Could not fetch GitHub script (HTTP {}).", response.statusCode());
                return;
            }

            byte[] bytes = response.body();
            if (bytes.length > MAX_SCRIPT_BYTES) {
                LOGGER.warn("GitHub script is too large (limit: {} bytes).", MAX_SCRIPT_BYTES);
                return;
            }
            String source = new String(bytes, StandardCharsets.UTF_8);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (hash.equals(lastSuccessfulHash)) return;

            runScript(source);
            lastSuccessfulHash = hash;
            LOGGER.info("Loaded updated GitHub script ({} bytes).", bytes.length);
        } catch (Exception exception) {
            LOGGER.warn("Could not load or run the GitHub script.", exception);
        }
    }

    private void runScript(String source) {
        SCRIPT_CONTEXTS.call(context -> {
            context.setLanguageVersion(Context.VERSION_ES6);
            context.setClassShutter(className -> false);
            Scriptable scope = context.initStandardObjects();
            scope.put("print", scope, new BaseFunction() {
                @Override
                public Object call(Context ignored, Scriptable currentScope, Scriptable thisObject, Object[] args) {
                    String message = args.length == 0 ? "" : Context.toString(args[0]);
                    LOGGER.info("[GitHub script] {}", message);
                    return Undefined.instance;
                }
            });
            context.evaluateString(scope, source, SCRIPT_URI.toString(), 1, null);
            return null;
        });
    }
}
