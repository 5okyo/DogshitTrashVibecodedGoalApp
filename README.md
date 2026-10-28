# Nihongo Tracker

Personal Android app (Kotlin, Jetpack Compose, Room) for tracking Japanese study goals. Plain text-list UI on a pure black background. All data stays on the phone.

## What it does
- **Anki** (home screen): tap once a day to keep the streak, tap again to undo. Shows current and longest streak.
- **Yesterday**: tap to mark yesterday's Anki as done if you forgot (tap again to undo).
- **Log**: pick a medium (Anki, Reading, Listening, Watching, Games, Speaking, Textbook, Other), enter time and/or 文字. Time has **hours / min / sec** boxes; use any of them (`90` in min alone is 1h 30m, same as `1` hour + `30` min).
- **Goals**: metric (文字 count, minutes, or Anki days) × medium (or all) × period (daily, or last 7 days) × target, with a thin progress bar. Minute goals sum your logged seconds.
- **History**: every log, with remove.

## Get the APK (no Android Studio needed)
Every push to `main` runs the **Build APK** GitHub Action (`.github/workflows/build-apk.yml`), which builds a debug APK and publishes it as a GitHub Release (`build-<run number>`).
1. Open the repo's **Releases** page (or the Actions run) and download `nihongo-tracker.apk`.
2. Open it on your phone and install (allow "install unknown apps" for your browser/file manager when asked).

The repo includes a fixed debug keystore (`app/debug.keystore`, debug-only, not a secret), so every build is signed with the same key and a new APK installs over the old one without losing data.

## Auto-update (no server)
GitHub Releases is the update server. On launch the app asks GitHub for the latest release; if its build number is higher than the installed one, an **Update** line appears at the top of the home screen.
Tap it, the app downloads the APK and opens Android's installer, then you tap Install (Android always requires that tap).
The first time, Android sends you to a settings page to allow installs from this app; allow it, go back, and tap Update again.
- Needs the repo to stay public (the app reads the release without logging in).
- The very first install has to be manual (above); only builds that contain the updater can update themselves.
- Local builds (`./gradlew assembleDebug`) have build number 1, so they will always offer an update.

## Build locally
Open the folder in Android Studio (JDK 17), or run `./gradlew assembleDebug`. Needs Android 8.0 (API 26) or newer to run.

## Status
Written but **not yet compiled or run** by its author's tooling (no Android SDK was available). The first Actions run is the first real compile, so expect to fix a few small errors; the build log will point to them.

## Known simplifications
- Home-screen numbers refresh when data changes, not at midnight while the app sits open (the Anki buttons themselves always use the real current date).
- Weekly goals use a rolling 7-day window, not calendar weeks.
- Logs can't be edited (remove and re-add). Only today and yesterday can be marked for Anki.
- Database version 2 stores time in seconds; an automatic migration converts any version 1 data (minutes × 60).
