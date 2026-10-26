# Mod Code — Fabric 1.21.11

A client-side Fabric mod for Minecraft 1.21.11. The installed mod checks `scripts/main.js` on GitHub every 20 seconds and runs it when its contents change. This lets you change supported script actions without rebuilding the mod JAR or restarting Minecraft.

## Change the live script

Edit `scripts/main.js` and push it to the `main` branch. The current safe script API is `print("message")`, which writes to the Minecraft log. For example, `print("gigachad");` logs `[GitHub script] gigachad`. The script is sandboxed: it cannot load arbitrary Java classes or call Minecraft APIs directly. New script capabilities must be added to the mod itself and need a new JAR once.

The code comes from your public GitHub repository. Only edit it with code you trust. Script changes are picked up while Minecraft is open; Java class changes still need a rebuilt JAR and game restart.

## Automatic builds

GitHub Actions builds the project on every push and pull request. To download the latest build, open the repository's **Actions** tab, select the newest successful **Build Fabric mod** run, and download the `modcode-fabric-1.21.11` artifact. Extract the ZIP and put `modcode-1.0.0.jar` in the `mods` folder for your Fabric 1.21.11 profile. Fabric API is required.

A changed or added Java class is included when GitHub builds the next JAR. Minecraft cannot load newly compiled mod classes from the repository into an already-running game: install the new JAR and restart Minecraft to use Java-code changes.

## Build locally

Requires JDK 21. Run `./gradlew build` (Windows: `gradlew.bat build`). The JAR is created at `build/libs/modcode-1.0.0.jar`.

## Add mod code

Put Java code under `src/main/java/com/quiqzy/modcode/`. To make another class run, call it from `ModCodeClient` or register it as a Fabric entrypoint in `src/main/resources/fabric.mod.json`. Commit and push your changes; the workflow will build a new downloadable JAR automatically.
