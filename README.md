# Nihongo Tracker

Personal Android app (Kotlin, Jetpack Compose, Room) for tracking Japanese study goals. Plain dark text-list UI. All data stays on the phone.

## What it does
- **Anki** (home screen): tap once a day to keep the streak, tap again to undo. Shows current and longest streak.
- **Yesterday**: tap to mark yesterday's Anki as done if you forgot (tap again to undo).
- **Log**: pick a medium (Anki, Reading, Listening, Watching, Games, Speaking, Textbook, Other), enter minutes and/or 文字.
- **Goals**: metric (文字 count, minutes, or Anki days) × medium (or all) × period (daily, or last 7 days) × target, with a thin progress bar.
- **History**: every log, with remove.

## Get an APK without Android Studio
Every push to `main` runs the **Build APK** GitHub Action (`.github/workflows/build-apk.yml`).
1. Open the repo's **Actions** tab and click the latest run (or run it manually with *Run workflow*).
2. Download the `nihongo-tracker-debug-apk` artifact at the bottom of the run page and unzip it.
3. Copy `app-debug.apk` to your phone and install it (allow "install unknown apps" for your file manager or browser).

It is a debug-signed APK, which is fine for personal use. The repo includes a fixed debug keystore (`app/debug.keystore`, debug-only, not a secret), so every run is signed with the same key and a new APK installs over the old one without losing your data. Artifacts expire after 90 days, so re-run the workflow when needed.

## Build locally
Open the folder in Android Studio (JDK 17), or run `./gradlew assembleDebug`. Needs Android 8.0 (API 26) or newer to run.

## Status
Written but **not yet compiled or run** by its author's tooling (no Android SDK was available). The first Actions run is the first real compile, so expect to fix a few small errors; the build log will point to them.

## Known simplifications
- Home-screen numbers refresh when data changes, not at midnight while the app sits open (the Anki buttons themselves always use the real current date).
- Weekly goals use a rolling 7-day window, not calendar weeks.
- Logs can't be edited (remove and re-add). Only today and yesterday can be marked for Anki.
