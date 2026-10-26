package com.quiqzy.modcode;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ModCodeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("modcode");
    private static final String RAW_BASE =
            "https://raw.githubusercontent.com/Quiqzy404/mod-code/main/runtime-classes/";
    private static final URI INDEX_URI = URI.create(RAW_BASE + "class-index.txt");
    private static final int MAX_INDEX_BYTES = 64 * 1024;
    private static final int MAX_CLASS_COUNT = 100;
    private static final int MAX_TOTAL_CLASS_BYTES = 4 * 1024 * 1024;
    private static final String DYNAMIC_PACKAGE = "com.quiqzy.modcode.dynamic.";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ScheduledExecutorService SCRIPT_POLLER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "modcode-script-poller");
        thread.setDaemon(true);
        return thread;
    });
    private volatile String lastLoadedHash = "";

    @Override
    public void onInitializeClient() {
        LOGGER.info("Mod Code is running. Checking GitHub for compiled Java classes every 20 seconds.");
        SCRIPT_POLLER.scheduleWithFixedDelay(this::checkForClasses, 0, 20, TimeUnit.SECONDS);
    }

    private void checkForClasses() {
        try {
            byte[] indexBytes = getBytes(INDEX_URI, MAX_INDEX_BYTES);
            if (indexBytes == null) return;
            String index = new String(indexBytes, StandardCharsets.UTF_8);
            String[] paths = index.lines().map(String::trim).filter(line -> !line.isEmpty()).toArray(String[]::new);
            if (paths.length > MAX_CLASS_COUNT) {
                LOGGER.warn("GitHub class list has too many entries (limit: {}).", MAX_CLASS_COUNT);
                return;
            }

            Map<String, byte[]> classes = new LinkedHashMap<>();
            int totalBytes = 0;
            for (String path : paths) {
                String className = classNameFor(path);
                if (className == null) {
                    LOGGER.warn("Ignoring class outside the allowed package: {}", path);
                    continue;
                }
                byte[] classBytes = getBytes(URI.create(RAW_BASE + path), MAX_TOTAL_CLASS_BYTES - totalBytes);
                if (classBytes == null) return;
                totalBytes += classBytes.length;
                if (totalBytes > MAX_TOTAL_CLASS_BYTES) {
                    LOGGER.warn("Downloaded classes exceed the {} byte limit.", MAX_TOTAL_CLASS_BYTES);
                    return;
                }
                classes.put(className, classBytes);
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            classes.forEach((name, bytes) -> {
                digest.update(name.getBytes(StandardCharsets.UTF_8));
                digest.update(bytes);
            });
            String hash = HexFormat.of().formatHex(digest.digest());
            if (hash.equals(lastLoadedHash)) return;

            lastLoadedHash = hash;
            MinecraftClient.getInstance().execute(() -> loadAndRun(classes));
        } catch (Exception exception) {
            LOGGER.warn("Could not download or run GitHub classes.", exception);
        }
    }

    private static byte[] getBytes(URI uri, int maxBytes) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "ModCode-Fabric/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            LOGGER.warn("Could not download {} (HTTP {}).", uri.getPath(), response.statusCode());
            return null;
        }
        if (response.body().length > maxBytes) {
            LOGGER.warn("Downloaded file exceeds the allowed size: {}", uri.getPath());
            return null;
        }
        return response.body();
    }

    private static String classNameFor(String path) {
        String prefix = DYNAMIC_PACKAGE.replace('.', '/') ;
        if (!path.startsWith(prefix) || !path.endsWith(".class") || path.contains("..")) return null;
        return path.substring(0, path.length() - ".class".length()).replace('/', '.');
    }

    private static void loadAndRun(Map<String, byte[]> classes) {
        if (classes.isEmpty()) {
            LOGGER.info("No dynamic classes are published in the repository yet.");
            return;
        }
        try {
            InMemoryClassLoader loader = new InMemoryClassLoader(classes, ModCodeClient.class.getClassLoader());
            for (String name : classes.keySet()) {
                if (name.contains("$")) continue;
                Class<?> loaded = loader.loadClass(name);
                try {
                    Method run = loaded.getMethod("run");
                    if (!Modifier.isStatic(run.getModifiers()) || run.getParameterCount() != 0) {
                        LOGGER.warn("{}.run() must be public static and have no parameters.", name);
                        continue;
                    }
                    run.invoke(null);
                    LOGGER.info("Ran downloaded class {}", name);
                } catch (NoSuchMethodException ignored) {
                    LOGGER.warn("Skipping {}: it has no public static void run() method.", name);
                } catch (ReflectiveOperationException exception) {
                    LOGGER.error("Could not run downloaded class {}", name, exception);
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Could not define downloaded classes.", exception);
        }
    }

    private static final class InMemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        private InMemoryClassLoader(Map<String, byte[]> classes, ClassLoader parent) {
            super(parent);
            this.classes = new HashMap<>(classes);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null && name.startsWith(DYNAMIC_PACKAGE) && classes.containsKey(name)) {
                    loaded = findClass(name);
                }
                if (loaded == null) loaded = super.loadClass(name, false);
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classes.get(name);
            if (bytes == null) throw new ClassNotFoundException(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
