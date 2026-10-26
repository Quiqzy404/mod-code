# Mod Code — Fabric 1.21.11

A client-side Fabric mod for Minecraft 1.21.11. It downloads compiled Java classes published under `runtime-classes/` from this public GitHub repository and checks for changes every 20 seconds while the game is open.

## Extend the mod

Add Java files as usual under `src/main/java/com/quiqzy/modcode/` (or a subpackage). Each new class that should run needs a `public static void run()` method. For example:

```java
package com.quiqzy.modcode;

public final class MyFeature {
    public static void run() {
        System.out.println("It runs from GitHub!");
    }
}
```

You can also add a class implementing Fabric's `ClientModInitializer`; the loader calls `onInitializeClient()` for it. Commit and push your Java changes. GitHub Actions compiles all Java files except the loader itself and publishes their `.class` files in `runtime-classes/`. The installed mod downloads the updated classes and runs them without replacing its JAR. Classes with no `run()` method are still loaded for use as helpers by another downloaded class.

This live loader is for Java code. Other files such as textures, sounds, mixin configs, or Fabric metadata are not automatically activated by downloading them; they need their own resource loader or a new mod JAR. Downloaded Java code runs with the same permissions as Minecraft, so only use code you trust. The repository must stay public so the game can fetch files without credentials.

## Automatic builds

GitHub Actions builds the project on every push and pull request. To download the latest build, open the repository's **Actions** tab, select the newest successful **Build Fabric mod** run, and download the `modcode-fabric-1.21.11` artifact. Extract the ZIP and put `modcode-1.0.0.jar` in the `mods` folder for your Fabric 1.21.11 profile. Fabric API is required.

A changed or added Java class is included when GitHub builds the next JAR. Minecraft cannot load newly compiled mod classes from the repository into an already-running game: install the new JAR and restart Minecraft to use Java-code changes.

## Build locally

Requires JDK 21. Run `./gradlew build` (Windows: `gradlew.bat build`). The JAR is created at `build/libs/modcode-1.0.0.jar`.

## Add mod code

Changes to `ModCodeClient.java`, Gradle configuration, or Fabric metadata require a new mod JAR and game restart. Ordinary added or changed Java feature classes under `src/main/java/com/quiqzy/modcode/` are published for live loading.
