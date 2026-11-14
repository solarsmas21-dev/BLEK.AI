# BLEK.AI

BLEK.AI is a personal Android AI chat app with a clean, minimal conversation interface.

## Current stage
V1 foundation: native Android project using Kotlin and Jetpack Compose. This first commit provides the chat UI shell; it does not yet connect to an AI provider.

## Planned architecture
- Online model through a small backend service (API keys must stay on the server, never inside the APK).
- Optional on-device model through a local inference runtime, subject to device capability and model licensing.
- Conversation history stored locally on the device.
- User-configurable assistant style and creativity.
- Clear safety boundaries and user controls.

## Open in Android Studio
1. Open this repository as a project.
2. Let Gradle sync.
3. Run the app on an Android device or emulator.

## Next milestones
1. Connect an online model through a protected backend.
2. Add local model support and an Online / Local / Auto selector.
3. Persist conversations locally.
4. Add settings, voice input, and APK release builds.

Never commit API keys, passwords, or other secrets.
