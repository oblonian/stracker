# stracker

A self tracker for Android. It records your day on its own — sleep, phone use
per app, movement, places and the path between them — and lets you log the
rest (headache, meal, period, habits…) with one tap. Everything shows up on a
month calendar; tap a day for its timeline. All data stays on the phone.

Started from [Ostrava](https://github.com/oblonian/Ostrava), an open-source
Strava-style tracker, keeping its Compose design system, Room storage,
background service and CI. Design: `docs/superpowers/specs/`. Plan:
`docs/superpowers/plans/`.

## Building

Requires JDK 17 and the Android SDK (compileSdk 35).

```bash
./gradlew testDebugUnitTest   # unit, Room and screenshot tests
./gradlew assembleDebug       # APK at app/build/outputs/apk/debug/
```

CI builds a debug APK on every push and publishes it, with rendered
screenshots of every screen, on the `apk-dist` branch. Builds are signed
with the committed sideload key so they install over one another.
