# Mod Code (Fabric 1.21.11)

A small client-side Fabric mod for Minecraft 1.21.11. At startup it downloads `modcode.script.json` from this repository's `main` branch and interprets only the allowlisted `log` and `chat` actions. It does not execute Java, JavaScript, shell commands, or downloaded classes.

## Build

Requires JDK 21. Run `./gradlew build` (Windows: `gradlew.bat build`). The jar is written to `build/libs/modcode-1.0.0.jar`. Install it together with Fabric API in the `mods` folder for Minecraft 1.21.11.

## Change what the mod does

Edit `modcode.script.json` on GitHub. Supported format:

```json
{
  "actions": [
    { "type": "log", "message": "Printed in the game log" },
    { "type": "chat", "message": "Shown in your client chat" }
  ]
}
```

The mod reads this file once each time Minecraft starts. Restart Minecraft to load changes. Maximum file size is 32 KiB, maximum 32 actions, and messages are limited to 240 characters. Unsupported action types are ignored.
