# modcode

A small Fabric mod for Minecraft 1.21.11 that downloads a safe script from GitHub on startup and executes a tiny, controlled action set.

## Features
- Fabric project for Minecraft 1.21.11
- Loads `modcode.script.json` from the `main` branch of this repository
- Executes only trusted, allowlisted actions: `log` and `chat`
- Logs a clear warning instead of crashing if the remote script cannot be loaded
- No repeated background execution; the script is fetched once at startup

## Quick build

From the repository root:

```bash
./gradlew build
```

The finished mod JAR is created in:

```text
build/libs/
```

On a normal build, the file will look like:

```text
build/libs/modcode-1.0.0.jar
```

## How the mod loads the script

At startup, the mod fetches:

```text
https://raw.githubusercontent.com/Quiqzy404/mod-code/main/modcode.script.json
```

This file lives in the repository root and can be edited in GitHub without rebuilding the mod. After changing the JSON and restarting Minecraft, the new script will be loaded on the next launch.

## Allowed script actions

The script format is intentionally limited to harmless actions only:

```json
{
  "version": 1,
  "actions": [
    { "type": "log", "message": "modcode script loaded successfully" },
    { "type": "chat", "message": "Hello from modcode!" }
  ]
}
```

Allowed actions:
- `log`: writes to the Minecraft log
- `chat`: sends a message to the in-game chat

Not allowed:
- running Java code
- running JavaScript
- shell commands
- arbitrary file execution
- network requests to random endpoints besides the GitHub script URL

## Script update workflow

1. Open the file `modcode.script.json` in the GitHub repository.
2. Edit the JSON on the `main` branch.
3. Save the file.
4. Restart Minecraft.
5. The mod fetches the updated script on the next startup.

## Failure behavior

If the remote script cannot be downloaded, the mod does not crash Minecraft. It logs a warning and continues startup normally.

## Repository notes

This repository contains the Fabric mod project in the root folder and no longer includes stale Forge files.
