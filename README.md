# Field of Memories

A psychological horror mod for Minecraft 1.20.1 (Forge).

## Requirements
- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17

## Build
Install Gradle 8.8 and run `gradle build`. The mod JAR is created in `build/libs/`.

Every push to `main` and every pull request targeting `main` runs the GitHub Actions build. Download the compiled JAR from the workflow run's **Artifacts** section.

## Current implementation
- Sprinting, stamina, and jump mechanics
- Prone/fall state with recovery
- Permission-gated admin tool and commands

Movement and stamina are currently driven by client-side logic. Dedicated-server and multiplayer behavior still requires in-game verification.

## Planned
- Ambient horror events in dark areas at night
- Unsettling distant sounds and visual cues

Ambient horror event code is present as an inactive foundation and is not registered to the Forge event bus yet.

This project is an original horror setting. Features will be expanded in subsequent milestones.
