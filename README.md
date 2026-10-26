# Mod Code — Fabric 1.21.11

A normal client-side Fabric mod project for Minecraft 1.21.11. Java source files in `src/main/java` are compiled into the mod JAR.

## Automatic builds

GitHub Actions builds the project on every push and pull request. To download the latest build, open the repository's **Actions** tab, select the newest successful **Build Fabric mod** run, and download the `modcode-fabric-1.21.11` artifact. Extract the ZIP and put `modcode-1.0.0.jar` in the `mods` folder for your Fabric 1.21.11 profile. Fabric API is required.

A changed or added Java class is included when GitHub builds the next JAR. Minecraft cannot load newly compiled mod classes from the repository into an already-running game: install the new JAR and restart Minecraft to use Java-code changes.

## Build locally

Requires JDK 21. Run `./gradlew build` (Windows: `gradlew.bat build`). The JAR is created at `build/libs/modcode-1.0.0.jar`.

## Add mod code

Put Java code under `src/main/java/com/quiqzy/modcode/`. To make another class run, call it from `ModCodeClient` or register it as a Fabric entrypoint in `src/main/resources/fabric.mod.json`. Commit and push your changes; the workflow will build a new downloadable JAR automatically.
