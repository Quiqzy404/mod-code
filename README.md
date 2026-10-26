# Mod Code — Fabric 1.21.11

A client-side Fabric mod for Minecraft 1.21.11. It downloads all compiled Java classes published under `runtime-classes/` from this public GitHub repository, loads them, and calls every top-level class's `public static void run()` method. It checks for new compiled classes every 20 seconds while the game is open.

## Add or change Java classes

Put Java source files under `dynamic-src/com/quiqzy/modcode/dynamic/`. Each class you want the mod to start needs a `public static void run()` method. For example:

```java
package com.quiqzy.modcode.dynamic;

public final class MyFeature {
    public static void run() {
        System.out.println("It runs from GitHub!");
    }
}
```

Commit and push the source file. GitHub Actions compiles the dynamic classes and publishes the `.class` files in `runtime-classes/`. The already-installed mod downloads and runs the updated classes without rebuilding or restarting the mod JAR. A class in this folder can use Minecraft/Fabric APIs available in the project compile classpath. Classes with no `run()` method are loaded as helpers but are not started themselves.

**Security:** downloaded Java classes execute with the same permissions as Minecraft and the mod. Only use this with code you trust and keep the repository public so the game can fetch it without credentials.

## Automatic builds

GitHub Actions builds the project on every push and pull request. To download the latest build, open the repository's **Actions** tab, select the newest successful **Build Fabric mod** run, and download the `modcode-fabric-1.21.11` artifact. Extract the ZIP and put `modcode-1.0.0.jar` in the `mods` folder for your Fabric 1.21.11 profile. Fabric API is required.

A changed or added Java class is included when GitHub builds the next JAR. Minecraft cannot load newly compiled mod classes from the repository into an already-running game: install the new JAR and restart Minecraft to use Java-code changes.

## Build locally

Requires JDK 21. Run `./gradlew build` (Windows: `gradlew.bat build`). The JAR is created at `build/libs/modcode-1.0.0.jar`.

## Add mod code

`src/main/java` is for changes to the loader itself. Changes there require a new mod JAR and a game restart. The dynamic classes belong in `dynamic-src`.
