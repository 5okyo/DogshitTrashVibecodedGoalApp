# Nihongo Tracker

Android app (Kotlin, Jetpack Compose, Room) for tracking Japanese study goals. All data stays on the phone.

## What it does
- **Log study**: medium (Anki, Reading, Listening, Watching, Games, Speaking, Textbook, Other), minutes, and 文字 count.
- **Anki streak**: enter "Anki reviews today"; any day with at least 1 review extends the streak. Shows current and longest streak.
- **Goals**: metric (文字 count, minutes, Anki reviews) × medium (or all) × period (daily, or the last 7 days) × target. Progress bars on the Today tab.
- **History**: every log, with delete. **Today** also shows minutes per medium for the last 7 days.

## Build and run
1. Open this folder in Android Studio (Ladybug or newer, JDK 17). It will download Gradle and the SDK.
2. Run on a device or emulator (Android 8.0 / API 26 or newer).
3. Unit tests for streaks and goal progress: `./gradlew test` (Android Studio generates the Gradle wrapper on first sync).

## Status
Written but **not yet compiled or run**: the environment it was written in has no Android SDK. Expect to fix a few small compile errors on first build.

## Open item: automatic Anki import
`AnkiSource.kt` defines the interface and `TrackerViewModel.importAnki()` already merges results, but no real source is connected, so Anki reviews are entered by hand for now. Before building one, verify what AnkiDroid exposes to other apps (per-day review history may not be available through its public API). Alternatives: import a "date,reviews" CSV, or read review history from an exported collection.

## Known simplifications
- "Today" is recalculated when data changes, not at midnight while the app is open.
- Weekly goals use a rolling 7-day window, not calendar weeks.
- Editing a log isn't supported yet (delete and re-add).
