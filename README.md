# Nihongo Tracker

Personal Android app (Kotlin, Jetpack Compose, Room) for tracking Japanese study goals. All data stays on the phone.

## What it does
- **Log study**: medium (Anki, Reading, Listening, Watching, Games, Speaking, Textbook, Other), minutes, and 文字 count.
- **Anki streak**: one button, "I did Anki today". Tap it once a day to keep the streak; tap again to undo. Shows current and longest streak. There is no AnkiConnect/AnkiDroid integration.
- **Goals**: metric (文字 count, minutes, or Anki days) × medium (or all) × period (daily, or the last 7 days) × target. Progress bars on the Today tab.
- **History**: every log, with delete. **Today** also shows minutes per medium for the last 7 days.

## Build and run
1. Open this folder in Android Studio (Ladybug or newer, JDK 17). It will download Gradle and the SDK.
2. Run on a device or emulator (Android 8.0 / API 26 or newer).
3. Unit tests for streaks and goal progress: `./gradlew test` (Android Studio generates the Gradle wrapper on first sync).

## Status
Written but **not yet compiled or run**: it was written in an environment with no Android SDK. Expect to fix a few small compile errors on first build.

## Known simplifications
- "Today" is recalculated when data changes, not at midnight while the app is open.
- Weekly goals use a rolling 7-day window, not calendar weeks.
- Missed days can't be backfilled and logs can't be edited yet (delete and re-add).
