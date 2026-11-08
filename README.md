# Nihongo Tracker

Personal Android app (Kotlin, Jetpack Compose, Room) for tracking Japanese study goals. Plain text-list UI on a pure black background. All data stays on the phone.

## What it does
- **Anki** (home screen): tap once a day to keep the streak, tap again to undo. Shows current and longest streak.
- **Yesterday**: tap to mark yesterday's Anki as done if you forgot (tap again to undo).
- **Total 文字** (home screen): all-time 文字 across every medium; tap it to open the heatmap.
- **Log**: pick a medium (Anki, Reading, Listening, Watching, Games, Speaking, Textbook, Other), enter time and/or 文字. Time has **hours / min / sec** boxes; use any of them (`90` in min alone is 1h 30m, same as `1` hour + `30` min).
- **Goals**: metric (文字 count, minutes, or Anki days) × medium (or all) × period (daily, or last 7 days) × target, with a thin progress bar. Minute goals sum your logged seconds.
- **Heatmap**: a grid per year like the Anki heatmap add-on (one column per week, Monday at the top). Switch between **Anki** (days you tapped the button), **Time** and **文字**; shades are quarters of your busiest day. Summary line: daily average (Time only), days active %, longest and current streak.
- **History**: every log. Tap one to edit it (medium, time, 文字, and the date with ‹ ›), or remove it.

## Get the APK (no Android Studio needed)
Every push to `main` runs the **Build APK** GitHub Action (`.github/workflows/build-apk.yml`), which builds a debug APK and publishes it as a GitHub Release (`build-<run number>`).
1. Open the repo's **Releases** page (or the Actions run) and download `nihongo-tracker.apk`.
2. Open it on your phone and install (allow "install unknown apps" for your browser/file manager when asked).

The repo includes a fixed debug keystore (`app/debug.keystore`, debug-only, not a secret), so every build is signed with the same key and a new APK installs over the old one without losing data.

## Auto-update 
GitHub Releases is the update server. Every time the app comes to the front it asks GitHub for the latest release; if its build number is higher than the installed one, an **Update** line appears at the top of the home screen. There is also a **Check for updates** line (it shows the installed build number and whether you are up to date) that re-checks when tapped.
Build 1 predates the updater, so if that is what you installed, update it by hand once.
Some launchers keep showing the old icon after an update until you restart the launcher or the phone.
Tap it, the app downloads the APK and opens Android's installer, then you tap Install (Android always requires that tap).
The first time, Android sends you to a settings page to allow installs from this app; allow it, go back, and tap Update again.
- Needs the repo to stay public (the app reads the release without logging in).
- The very first install has to be manual (above); only builds that contain the updater can update themselves.
- Local builds (`./gradlew assembleDebug`) have build number 1, so they will always offer an update.

## Build locally
Open the folder in Android Studio (JDK 17), or run `./gradlew assembleDebug`. Needs Android 8.0 (API 26) or newer to run.
