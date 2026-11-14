# stracker

A self tracker for Android. It records your day on its own — sleep, phone use
per app, movement, the path you took and the places you stayed — and lets you
log the rest (headache, meal, period, habits…) with one tap. Everything shows
up on a month calendar; tap a day for its timeline. **All data stays on the
phone.**

Started from [Ostrava](https://github.com/oblonian/Ostrava), an open-source
Strava-style tracker, keeping its Compose design system, Room storage,
background service, screenshot tests and CI.

## Screens

Rendered by the test suite on every push; the latest set is in the
`apk-dist` branch under `screenshots/`.

| Calendar | Day | Quick log |
|---|---|---|
| `calendar.png` | `day.png` | `quicklog.png` |

- **Calendar** — the home screen. Each day carries a thin bar of how its hours
  split up (sleep, work, phone, travel, exercise, eating), small dots for
  things you logged, and a band for multi-day episodes such as a period.
  Today is outlined. That's all that is on the screen.
- **Day** — a vertical 24-hour timeline from one wake-up to the next, with
  totals for sleep, phone, work and steps. Guessed blocks are lighter with a
  dashed edge; blocks you logged or corrected have a solid edge. Tap a block
  or a pin to edit it; long-press the timeline to log something at that time;
  tap the phone total for the per-app list; the map icon shows where you went.
- **Quick log** — one tap logs a moment (headache, meal, coffee…) at the
  current time. Buttons for stretches (exercise, study, nap, sick, period)
  start on the first tap and end on the second. "Add details" and "Undo" are
  offered right after. Long-press a button to edit or hide it.
- **Settings** — working hours, the sleep window, named places, the quick-log
  buttons, permission status with a Fix button for each, backup and restore.

## How tracking works

A single quiet foreground service runs all day:

| Signal | Where it comes from |
|---|---|
| Screen on/off, app in front | Android's usage-event log, read every 15 min and on screen-off |
| Still / walking / running / cycling / in vehicle | Activity Recognition transitions (the OS wakes the app only on a change) |
| Steps | the step-counter sensor, batched |
| Path | GPS, **only while you are moving** — a still phone never holds the GPS |

Rules turn those into the day (all thresholds live in
`domain/Thresholds.kt`):

- **Sleep** is the longest overnight stretch with no screen use and no
  movement (≥ 3 h; interruptions ≤ 10 min stay inside). Missing motion data
  counts as still, so a collector the phone killed overnight still yields a
  sleep block, marked as a guess. A night's sleep belongs to the day you wake
  up, so calendar days run wake-up to wake-up.
- **Work** is time inside your working hours with the phone idle and you not
  moving — and, once you tag a place as *work*, only while you are there.
- **Travel** is vehicle/bicycle time, or walking that moved you more than
  400 m. **Exercise** is running, or walking for 20 min or more.
- Anything the app got wrong you can retype, trim or delete; corrections are
  kept separately and survive every re-run of the rules.

## Permissions

| Permission | For |
|---|---|
| Location, *all the time* | the path and place visits |
| Physical activity | movement type and steps |
| Usage access (a settings page) | time per app |
| Notifications | the one silent notice Android requires while tracking |
| Ignore battery optimisation (asked once) | realme/ColorOS stops background apps at night otherwise |

Nothing else. No network use except OpenStreetMap tiles when you open a
day's map. There are no accounts, no sync, no analytics, and the only
notification is the static "stracker is tracking" notice.

## Battery

Measured on the development phone (realme 9 Pro+, Android 14) over a normal
day: _to be filled in after the first 24 h on the device — target < 3 %._
If it comes out higher, lower the GPS rates in `collect/LocationGate.kt`
(walking 10 s / 10 m, vehicle 5 s).

## Building

Requires JDK 17 and the Android SDK (compileSdk 35).

```bash
./gradlew testDebugUnitTest   # unit, Room, worker and screenshot tests
./gradlew assembleDebug       # APK at app/build/outputs/apk/debug/
```

CI builds a debug APK on every push and publishes it, with the rendered
screenshots, on the `apk-dist` branch (`stracker-debug.apk`). Builds are
signed with the committed sideload key so they install over one another;
use your own key for a store release.

## Backup

Settings → *Back up to a file* writes one JSON file with every table and
setting; *Restore from a file* replaces everything on the phone with it.
No automatic backups.

## Design and plan

- Design: `docs/superpowers/specs/2026-11-14-stracker-design.md`
  (approved mockups in `docs/superpowers/specs/mockups/`)
- Plan: `docs/superpowers/plans/2026-11-14-stracker-v1.md`
- Ostrava's tips/reminder engine is parked in `app/src/legacy/` (not
  compiled) for a later "patterns" version.
