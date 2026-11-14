package com.oblonian.stracker.domain

import java.time.Instant

/** Screen-on stretches become PHONE episodes; app foreground events split them per app. */
object PhoneUsage {

    private const val MIN_APP_MILLIS = 60_000L

    fun episodes(screen: List<ScreenEvent>, window: DayWindow): List<Episode> =
        onIntervals(screen, window).map { (start, end) ->
            Episode(type = EpisodeType.PHONE, start = start, end = end, day = window.day, source = Source.AUTO)
        }

    /** Foreground time per package while the screen was on, largest first, under a minute dropped. */
    fun perApp(apps: List<AppUsageEvent>, screen: List<ScreenEvent>, window: DayWindow): List<AppUsage> {
        val on = onIntervals(screen, window)
        val totals = HashMap<String, Long>()
        val openSince = HashMap<String, Instant>()
        for (e in apps.sortedBy { it.time }) {
            if (e.foreground) {
                openSince.putIfAbsent(e.packageName, e.time)
            } else {
                val from = openSince.remove(e.packageName) ?: continue
                totals.merge(e.packageName, overlap(from, e.time, on), Long::plus)
            }
        }
        for ((pkg, from) in openSince) totals.merge(pkg, overlap(from, window.end, on), Long::plus)
        return totals.filterValues { it >= MIN_APP_MILLIS }
            .map { (pkg, ms) -> AppUsage(pkg, ms) }
            .sortedByDescending { it.foregroundMillis }
    }

    /** Screen-on [start, end) pairs clipped to the window. */
    private fun onIntervals(screen: List<ScreenEvent>, window: DayWindow): List<Pair<Instant, Instant>> {
        val out = ArrayList<Pair<Instant, Instant>>()
        var onSince: Instant? = null
        var first = true
        for (e in screen.sortedBy { it.time }) {
            if (e.on) {
                if (onSince == null) onSince = e.time
            } else {
                val start = onSince ?: if (first) window.start else null
                if (start != null && e.time > start) out += clip(start, e.time, window)
                onSince = null
            }
            first = false
        }
        onSince?.let { out += clip(it, window.end, window) }
        return out.filter { it.second > it.first }
    }

    private fun clip(a: Instant, b: Instant, w: DayWindow) = maxOf(a, w.start) to minOf(b, w.end)

    private fun overlap(from: Instant, to: Instant, on: List<Pair<Instant, Instant>>): Long =
        on.sumOf { (s, e) ->
            val a = maxOf(from, s)
            val b = minOf(to, e)
            if (b > a) b.toEpochMilli() - a.toEpochMilli() else 0L
        }
}
