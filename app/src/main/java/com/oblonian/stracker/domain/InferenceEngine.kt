package com.oblonian.stracker.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Turns one day's raw signals into the episodes the owner sees. Pure and deterministic:
 * `signals + manual episodes + overrides → DayData`, so it can be re-run at any time.
 */
class InferenceEngine(private val settings: InferenceSettings) {

    fun inferDay(
        day: LocalDate,
        window: DayWindow,
        mainSleep: Episode?,
        signals: Signals,
        places: List<Place>,
        manual: List<Episode>,
        overrides: List<Override>,
        now: Instant,
        events: List<Event> = emptyList(),
    ): DayData {
        val phone = PhoneUsage.episodes(signals.screen, window)
        val intervals = MovementInference.motionIntervals(signals.motion, window)
        val visits = MovementInference.placeVisits(signals.locations, places, window)

        val auto = buildList {
            mainSleep?.let { add(it.copy(day = day, source = Source.AUTO)) }
            addAll(phone)
            addAll(SleepInference.naps(window, signals, settings, mainSleep))
            addAll(MovementInference.travel(intervals, signals.locations, visits, window, places))
            addAll(MovementInference.exercise(intervals, window))
            addAll(WorkInference.work(window, settings, phone, intervals, visits, places))
        }

        val manualSpans = manual.map { Span(it.start, it.end ?: now) }
        val clipped = auto.flatMap { e -> clipAround(e, manualSpans) }
        val corrected = applyOverrides(clipped, overrides)

        return DayData(
            window = window,
            episodes = (corrected + manual).sortedBy { it.start },
            events = events.sortedBy { it.time },
            appUsage = PhoneUsage.perApp(signals.apps, signals.screen, window),
            steps = Steps.forWindow(signals.steps, window),
            path = signals.locations.filter { it.time >= window.start && it.time < window.end },
        )
    }

    /** An automatic episode never shows underneath a manual one: cut it into the parts outside. */
    private fun clipAround(e: Episode, cuts: List<Span>): List<Episode> {
        val end = e.end ?: return listOf(e)
        return Intervals.subtract(Span(e.start, end), cuts).map { e.copy(start = it.start, end = it.end) }
    }

    private fun applyOverrides(episodes: List<Episode>, overrides: List<Override>): List<Episode> {
        if (overrides.isEmpty()) return episodes
        val byKey = overrides.associateBy { it.episodeKey }
        return episodes.mapNotNull { e ->
            val o = byKey[e.key()] ?: return@mapNotNull e
            if (o.deleted) return@mapNotNull null
            e.copy(
                type = o.newType ?: e.type,
                start = o.newStart ?: e.start,
                end = o.newEnd ?: e.end,
                source = Source.AUTO_EDITED,
                confidence = Confidence.HIGH,
            )
        }
    }
}
