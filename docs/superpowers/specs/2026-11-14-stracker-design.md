# stracker — design

*Self tracker for Android. A map of your own life, kept on your own phone.*

Date: 2026-11-14. Status: reviewed by a builder check; awaiting owner approval.

## 1. Intent

The owner wants to see where their time actually goes, day after day, so that
the shape of their life — how they sleep, how much they're on the phone, how
much they really work, what repeats and what doesn't — becomes visible instead
of guessed at. The moment that triggered the idea: wondering about their own
sleep cycle, habits, screen time and productive hours, and wanting "the map of
my life, to see how repetitive my life is".

The app answers that by recording the day automatically (sleep, phone use,
movement, places, travel, work) and letting the owner add anything the phone
can't see (headache, fever, period, meals, washroom, habits) with one tap. The
result is shown as a month calendar where each day carries a small bar of how
its 24 hours split up; tapping a day shows its full timeline.

It is a single-user, personal tool. It is not a health product, not social,
not a cloud service.

## 2. Goals

1. A month calendar is the home screen. Each day shows a compact bar of its
   time split (sleep / phone / work / travel / exercise / eating / other) and
   small marks for logged events. The owner can scan a month and spot
   repetition or odd days by eye.
2. Tapping a day opens a 24-hour vertical timeline with coloured blocks for
   automatic episodes and pins for logged events, plus a header with the day's
   totals (sleep, phone, work, steps).
3. Automatic tracking, with nothing to start or stop:
   - **Phone use**: screen-on time, broken down **per app**.
   - **Movement**: still / walking / running / in vehicle, and step count.
   - **Location**: the **full path** throughout the day, plus visits to places
     the owner has named (home, lab, gym, …).
   - **Sleep**: inferred from the long nightly stretch with no phone use and
     no movement.
   - **Work**: inferred as *inside the owner's working hours* AND *phone idle*
     AND *not moving* (see §6.4).
   - **Travel**: inferred from movement type and location change.
4. One-tap logging of events and episodes from a quick-log sheet; details
   (severity, duration, note) are optional and can be added later. Buttons are
   user-configurable.
5. Every automatic block can be corrected (retyped, trimmed, deleted) with a
   tap, and the correction sticks.
6. All data stays on the phone. JSON backup/restore for moving to a new phone
   (inherited from Ostrava).
7. Invisible until opened: no nags, minimal battery cost, clean screens.

## 3. Non-goals (first version)

- **Automatic pattern detection or "insights"** ("you woke within 20 min of
  7:00 on 23 of 30 days"). The owner said the calendar is enough for now.
  Ostrava's insight/reminder engine stays in the codebase but is not wired to
  anything.
- **Wearables / other devices.** Phone only. Health Connect reading (steps,
  sleep from a watch) is a later version; keep the dependency so it is cheap
  to add.
- **Laptop/desktop productivity tracking.**
- **iOS.** The Ostrava `ios/` tree is dropped.
- **Strava-style sport features** (segments, personal records, best efforts,
  share cards, BLE heart-rate straps, interval workouts, audio cues, GPX
  import/export). Deleted from the fork. Walks/runs still appear as
  "exercise" episodes from movement detection.
- **Accounts, sync, sharing, cloud, analytics.** None.
- **Machine-learned classification.** First version is rule-based; the
  corrections are stored so a later version can learn from them.

## 4. Anti-goals — what would make the owner wince

1. **Battery drain.** A full day of tracking must not be noticeable on a
   normal day. See §6.2 for the rules that enforce this.
2. **A nagging app.** The only notification is the one Android forces for a
   background service, and it must be as quiet as Android allows (low
   importance, silent, no vibration, not re-posted). No reminders, no tips,
   no "you haven't logged today", no permission re-prompts after the first
   onboarding.
3. **A cluttered screen.** Calendar and day view stay sparse. No dashboards,
   no cards competing for attention, no numbers that aren't asked for. One
   accent colour per episode type, nothing else.

## 5. Constraints

- **Android only**, Kotlin + Jetpack Compose, minSdk 26, targetSdk 35
  (Ostrava's settings).
- **Owner's phone: realme 9 Pro+, Android 14 (realme UI), with Google Play
  Services.** Consequences:
  - Play Services is present, so the Activity Recognition Transition API is
    available. No fallback movement detector is needed in v1.
  - realme/ColorOS kills background services aggressively. The collector
    must survive this: (a) onboarding has one step that opens the app's
    battery page and asks for "Don't optimise" / allow background activity
    / auto-start, with a one-line reason (this is the one permitted
    exception to "no battery-optimisation request" in §6.2 — asked once,
    never re-prompted; status visible only in Settings); (b) the service is
    restarted by WorkManager keep-alive and on boot; (c) the sleep rule
    treats *missing data* overnight the same as STILL, so a killed collector
    still yields a sleep block.
  - API 34 rules apply: `FOREGROUND_SERVICE_HEALTH` / `health` FGS type are
    available; a `location`-type FGS cannot start unless location permission
    is already granted, so the service starts as `health` only and adds the
    `location` type once permission exists; `ACTIVITY_RECOGNITION` is a
    runtime permission.
- **Start from Ostrava** (`oblonian/Ostrava`, Android app ≈ 10.8k lines
  Kotlin). New repo `oblonian/stracker`; Ostrava itself is left untouched.
  Package `com.oblonian.stracker`, app name "stracker".
- **Builds happen on GitHub Actions**, not locally: the development laptop
  has JDK 17 but no Android SDK, 8 GB RAM and a small C: drive. Keep
  Ostrava's CI (unit tests + debug APK published to an `apk-dist` branch,
  signed with the committed sideload key) so the owner installs by
  downloading the APK. Do not require Android Studio for the build.
- **Data never leaves the device** except by explicit export.
- Keep Ostrava's design system, dark theme and Material 3 look, so the app
  feels finished from day one.

## 6. Design decisions

### 6.1 What is kept from Ostrava, what is dropped

Keep (adapt as needed):
- `tracking/TrackingService` foreground service + `TrackingStateHolder`:
  becomes the always-on collector service (§6.2).
- `domain/Gps.kt` Kalman smoothing and accuracy gating for the path.
- Room database + DataStore settings + `export/BackupManager` JSON
  backup/restore (reworked for the new schema).
- `ui/components/Charts.kt`, `Design.kt`, `StatComponents.kt`,
  `theme/`, onboarding flow, `di/AppContainer`.
- `ui/components/RouteMap.kt` (osmdroid) to show the day's path.
- `health/HealthConnectSync.kt` dependency (dormant).
- `domain/Insights.kt`, `Reminders.kt`, `notify/Reminders.kt` (dormant,
  not scheduled).
- CI workflow, sideload keystore, Roborazzi screenshot tests.

Drop: `ios/`, segments, best efforts, personal records, share studio,
heart-rate monitor, interval workouts, TTS audio cues, GPX import/export,
weekly distance goal, feed/record/stats/detail/share screens and their
view-models.

### 6.2 Collector service and battery rules

One foreground service (`foregroundServiceType="location|health"` as
required by API) runs from boot / app start and owns all collectors:

| Signal | Source | Rule |
|---|---|---|
| Screen on/off, app in foreground | `UsageStatsManager.queryEvents` | Polled, not listened: read the event log every 15 min via WorkManager and on screen-off; nothing runs while the screen is off. Needs the "Usage access" special permission. |
| Movement type | Activity Recognition **Transition API** (still / walking / running / on bicycle / in vehicle) | Event-driven; the OS wakes us only on change. |
| Steps | `TYPE_STEP_COUNTER` sensor, batched | Read on each transition and every 15 min. |
| Location | FusedLocationProvider | **Gated by movement**: no GPS requests while STILL. On leaving STILL, request high-accuracy updates (≈ every 10 s / 10 m while walking, 5 s while in vehicle). Back to STILL → one final fix, then stop. Apply Ostrava's Kalman filter and accuracy gate. |
| Sleep | derived | See §6.4. |

Hard rules: no wake-locks beyond what FusedLocation holds; no network use
except map tiles when the owner opens a day's map; use batched sensor
delivery; respect Doze (collectors must tolerate gaps and must never try to
defeat battery optimisation beyond the single onboarding step in §5; keep
Ostrava's `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for that step only).
Target: < 3 % of battery per day on
a typical day, measured with Android's battery stats on the owner's phone
before the first version is called done.

The service notification: lowest importance channel, silent, static text
("stracker is tracking"), never updated with live numbers.

### 6.3 Data model

Raw signals are kept as they arrive and are *never* rewritten; everything the
owner sees is derived from them plus explicit user edits.

Raw tables:
- `screen_events(time, type[ON|OFF], ...)`
- `app_usage_events(time, package, type[FOREGROUND|BACKGROUND])`
- `motion_transitions(time, activity, enterExit)`
- `step_samples(time, cumulativeCount)`
- `locations(time, lat, lon, alt, accuracy, speed)`

Derived / user-facing:
- `episodes(id, type, start, end, source[AUTO|MANUAL|AUTO_EDITED],
  placeId?, detailJson?)`
  Types: SLEEP, NAP, PHONE, WORK, TRAVEL, EXERCISE, EATING, SICK, PERIOD,
  CUSTOM(name). PERIOD and SICK are multi-day episodes.
- `events(id, type, time, detailJson?)` — single moments: HEADACHE, FEVER,
  MEAL, COFFEE, WASHROOM, WATER, CUSTOM(name). `detailJson` holds the
  optional severity / duration / note.
- `places(id, name, lat, lon, radiusM, colour)`
- `app_daily_usage(date, package, foregroundMillis)` — cached aggregate for
  the day view.
- `overrides(id, day, kind, payload)` — the owner's corrections to automatic
  episodes, stored separately so re-running inference never loses them.
- `quick_log_buttons(id, label, emoji, kind[EVENT|EPISODE], order, hidden)`

Settings (DataStore): working hours per weekday, sleep window hint
(e.g. 21:00–11:00 where sleep is looked for), unit preferences, onboarding
state.

### 6.4 Inference (rule-based, re-runnable per day)

Always `rawSignals + overrides → episodes`, re-runnable for any day.

**Which day owns a night's sleep (Claude's call):** diary convention — the
main sleep belongs to the day you *wake up*. The calendar bar for Thursday
shows Wednesday-night-to-Thursday-morning sleep plus Thursday's waking hours
up to the next main sleep's start; so a "day" on the calendar runs from one
wake-up to the next, not midnight to midnight. `episodes` carries a `day`
column set by this rule. Hour labels in the day view still show clock time.

**When inference runs:** on the first screen-on after the sleep window's end
hour (that's when yesterday is complete), at the end hour itself if the
phone hasn't been touched, and whenever a day view is opened (for that day).

- **PHONE**: each screen-on → screen-off interval. Per-app split from
  `app_usage_events`.
- **SLEEP**: longest stretch inside the sleep window with no screen-on and
  motion STILL (or no motion data at all — see §5) for ≥ 3 h; allow interruptions ≤ 10 min (bathroom, checking
  time) — they stay inside the sleep block. Shorter daytime stills ≥ 40 min
  with no phone use *outside* working hours → NAP (candidate, low confidence,
  shown lighter).
- **TRAVEL**: IN_VEHICLE / ON_BICYCLE stretches, or WALKING stretches that
  move > 400 m between two places. Short walking without displacement is
  not travel.
- **EXERCISE**: RUNNING any length; WALKING ≥ 20 min continuous; ON_BICYCLE
  shows as both travel and exercise only if the owner retypes it.
- **WORK**: inside working hours AND not PHONE AND motion STILL AND (at a
  place tagged "work" if any place is so tagged). Gaps ≤ 5 min are bridged.
  Shown with a dotted edge when confidence is low (e.g. no work place set).
- **Place visits**: location within `radiusM` of a place for ≥ 5 min; used
  to label blocks ("work (lab)", "home").
- **Unclassified** time is simply empty on the timeline. Nothing is forced
  into a category.

Confidence is shown, not hidden: inferred blocks are plain, inferred
low-confidence blocks are lighter/dotted, user-confirmed or manual blocks
have a solid edge.

### 6.5 Screens

Approved mockups are in `docs/superpowers/specs/mockups/` (`week-view.html`
option C = calendar; `day-and-log.html` = day view and quick-log sheet).
Build to them.

1. **Month calendar (home).** Standard 7-column grid, Monday first. Each day
   cell: date number, a 6 px horizontal bar split by episode type for that
   day's 24 h, and up to 3 tiny dots for logged events (colour per type).
   Swipe for previous/next month. Today is outlined. A legend sits below the
   grid. That's all that is on the screen.
2. **Day view.** Header: `‹ Wed 12 Nov ›` with swipe; a row of four totals
   (Sleep, Phone, Work, Steps). Body: vertical 24 h timeline, hour labels on
   the left, coloured episode blocks with short labels ("sleep 23:40–07:05",
   "work (lab)", "walk 2.1 km"), event pins on the right edge. Tapping a
   block or pin opens its detail. A single `+` floating button opens the
   quick-log sheet. A small map icon in the header opens the day's path on
   an OpenStreetMap view.
3. **Quick-log sheet.** Bottom sheet with a grid of big buttons (default
   set: headache, fever, period, sick, meal, coffee, washroom, water,
   exercise, study, nap, custom). One tap logs at *now* (or at the tapped
   time if the sheet was opened from a timeline long-press), the sheet
   closes, and a snackbar offers "add details" and "undo". Long-press a
   button to edit/hide it; a trailing "+" adds a new one.

   **Episode buttons (Claude's call):** a tap on an episode-kind button
   (exercise, study, nap, sick, period) *starts* an open-ended episode
   (`end = null`). The button shows an "ongoing" state (tinted, with the
   running time) and the timeline draws the block growing to *now*; tapping
   the button again ends it. An ongoing manual episode wins over inference
   for the time it covers. "Study" is a CUSTOM episode type counted into the
   work colour on the calendar bar. PERIOD and SICK are multi-day: a tap
   starts them, a second tap on a later day ends them; on the calendar they
   appear as a thin coloured band under the date number on every day they
   span, not in the 24 h bar.
4. **Episode / event detail.** Type, start, end (episodes), optional
   severity (1–5), note. Buttons: change type, delete, confirm (for AUTO).
   Changing an AUTO block writes an override.
5. **Phone usage breakdown.** Reached by tapping the Phone total in the day
   view: a list of apps with time and a horizontal bar each (reuse
   `HorizontalBar`).
6. **Settings.** Working hours (per weekday, with a copy-to-all), sleep
   window, places (list, add from current location or map tap, name, radius,
   tag "home"/"work"), quick-log buttons, permissions status with a fix
   button for each, backup/restore, about.
7. **Onboarding (first run).** Three screens: what the app does and that
   data stays on the phone; permissions one by one (location "all the time",
   physical activity, usage access, notifications) each with a one-line
   reason; working hours. Skippable; anything skipped shows as a quiet
   banner only in Settings, never elsewhere.

### 6.6 Permissions

| Permission | For |
|---|---|
| `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` | full-day path |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_HEALTH` | collector service |
| `ACTIVITY_RECOGNITION` | movement type, steps |
| `PACKAGE_USAGE_STATS` (special, via Settings) | per-app phone time |
| `POST_NOTIFICATIONS` | the one service notification |
| `RECEIVE_BOOT_COMPLETED` | restart collector after reboot |
| `INTERNET` | map tiles only |

| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | the one-time realme keep-alive step (§5) |

Removed from Ostrava: Bluetooth, vibrate, Health Connect write permissions
(reads may come back later).

### 6.7 Backup

Ostrava's JSON backup is kept and extended to the new schema: raw tables,
episodes, events, places, overrides, buttons and settings. Export via the
system share sheet / file picker; import replaces all data after a
confirmation. No automatic backups (that would be a nag).

## 7. Decisions made by the owner (verbatim intent)

- Purpose: sleep cycle, habits, phone time, productive work time; "the map
  of my life, to see how repetitive my life is".
- Home screen: a calendar showing the week's activities → month calendar
  with per-day summary bars, tap a day for its timeline (chose option C of
  three mockups).
- Data stays on the phone.
- Android; phone only for now, other devices later.
- Productive work inferred from working hours + phone not used + not moving;
  acceptable to defer if hard (it is not hard: see §6.4).
- Logging: one tap, details later if needed. Day view + quick-log sheet
  mockups approved.
- No automatic pattern detection in v1.
- Phone time broken down by app.
- Location: full path, not just places.
- Anti-goals: battery drain, nagging, clutter.
- New repo `oblonian/stracker`; Ostrava untouched.

## 8. Decisions made by Claude (owner may overrule)

- Movement-gated GPS (no fixes while still) as the way to make "full path"
  affordable on battery.
- Rule thresholds in §6.4 (3 h sleep minimum, 10 min interruption, 5 min
  work gap bridging, 20 min walk = exercise, etc.). All are constants in one
  file so they can be tuned.
- Raw signals kept forever and immutable; derived episodes recomputed.
- Episode/event type lists and default quick-log buttons.
- Package name `com.oblonian.stracker`.
- Keeping Ostrava's insight/reminder code dormant rather than deleting it.
- Confidence shown through block styling rather than numbers.
- Period and Sick are multi-day episodes; headache, fever, meal, coffee,
  washroom, water are moments.
- A night's sleep belongs to the wake-up day; calendar days run wake-up to
  wake-up (§6.4).
- Episode quick-log buttons start an open-ended episode, second tap ends it
  (§6.5).

## 9. Left to the builder

- Exact Room schema, migrations from a fresh v1 (no migration from Ostrava's
  database is needed — this is a new app).
- How the collector survives process death and reboot (WorkManager keep-alive
  vs. `START_STICKY`), within the battery rules.
- Rendering approach for the calendar bars and day timeline (Compose Canvas
  is the expected choice).
- Test strategy: inference rules should be pure functions with unit tests on
  synthetic signal sequences; screenshot tests for calendar and day view as
  Ostrava does with Roborazzi.
- Handling of the first days when there is little data (empty cells are
  fine; no placeholder text).
- Time zone changes and DST (days are local-time days; a day may be 23 or
  25 h).

## 10. Later versions (recorded so v1 leaves room)

- Pattern insights ("Tuesdays match Wednesdays 85 %"), reusing the dormant
  insight engine.
- Health Connect reads: watch sleep, steps, heart rate.
- Learning from overrides to improve work/sleep inference.
- Laptop activity as a work signal.
- Week-strip view (option B from the mockups) as an alternate calendar mode.
