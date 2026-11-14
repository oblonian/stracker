package com.oblonian.stracker.domain

import java.time.Instant

/** Screen-on stretches become PHONE episodes; app foreground events split them per app. */
object PhoneUsage {

    private const val MIN_APP_MILLIS = 60_000L

    fun episodes(screen: List<ScreenEvent>, window: DayWindow): List<Episode> =
        Intervals.screenOn(screen, Span(window.start, window.end)).map { s ->
            Episode(type = EpisodeType.PHONE, start = s.start, end = s.end, day = window.day, source = Source.AUTO)
        }

    /** Foreground time per package while the screen was on, largest first, under a minute dropped. */
    fun perApp(apps: List<AppUsageEvent>, screen: List<ScreenEvent>, window: DayWindow): List<AppUsage> {
        val on = Intervals.screenOn(screen, Span(window.start, window.end))
        val totals = HashMap<String, Long>()
        val openSince = HashMap<String, Instant>()
        for (e in apps.sortedBy { it.time }) {
            if (e.foreground) {
                openSince.putIfAbsent(e.packageName, e.time)
            } else {
                val from = openSince.remove(e.packageName) ?: continue
                totals.merge(e.packageName, Intervals.coveredMillis(Span(from, e.time), on), Long::plus)
            }
        }
        for ((pkg, from) in openSince) totals.merge(pkg, Intervals.coveredMillis(Span(from, window.end), on), Long::plus)
        return totals.filterValues { it >= MIN_APP_MILLIS }
            .map { (pkg, ms) -> AppUsage(pkg, ms) }
            .sortedByDescending { it.foregroundMillis }
    }
}
