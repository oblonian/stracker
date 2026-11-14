package com.oblonian.stracker.domain

import java.time.LocalDate

/**
 * Sleep is the long stretch at night when the phone is untouched and the owner isn't moving.
 * Missing motion data counts as still, so a collector that was killed overnight (realme does
 * this) still yields a sleep block, marked low confidence.
 */
object SleepInference {

    fun mainSleep(day: LocalDate, signals: Signals, settings: InferenceSettings): Episode? {
        val (from, to) = DayBoundaries.sleepSearchRange(day, settings)
        val range = Span(from, to)
        val anyData = signals.screen.any { it.time >= from && it.time < to } ||
            signals.motion.any { it.time >= from && it.time < to }
        if (!anyData) return null

        val states = Intervals.motionStates(signals.motion, range)
        val awake = Intervals.screenOn(signals.screen, range) +
            states.filter { it.motion != Motion.STILL && it.motion != Motion.UNKNOWN }.map { it.span }
        val quiet = Intervals.mergeGaps(Intervals.subtract(range, awake), Thresholds.SLEEP_GAP_MAX)
        val best = quiet.filter { it.millis >= Thresholds.SLEEP_MIN.toMillis() }.maxByOrNull { it.millis } ?: return null

        val unknown = states.filter { it.motion == Motion.UNKNOWN }.map { it.span }
        val confidence = if (Intervals.coveredMillis(best, unknown) * 2 > best.millis) Confidence.LOW else Confidence.HIGH
        return Episode(type = EpisodeType.SLEEP, start = best.start, end = best.end, day = day, source = Source.AUTO, confidence = confidence)
    }

    /**
     * Daytime stretches of at least [Thresholds.NAP_MIN] that are explicitly STILL with the screen
     * off, outside the main sleep and outside working hours. Always low confidence.
     */
    fun naps(window: DayWindow, signals: Signals, settings: InferenceSettings, mainSleep: Episode?): List<Episode> {
        val range = Span(window.start, window.end)
        val screenOn = Intervals.screenOn(signals.screen, range)
        val still = Intervals.motionStates(signals.motion, range).filter { it.motion == Motion.STILL }.map { it.span }
        val cuts = screenOn +
            listOfNotNull(mainSleep?.end?.let { Span(mainSleep.start, it) }) +
            WorkInference.workingHourSpans(range, settings)
        return still
            .flatMap { Intervals.subtract(it, cuts) }
            .filter { it.millis >= Thresholds.NAP_MIN.toMillis() }
            .map { Episode(type = EpisodeType.NAP, start = it.start, end = it.end, day = window.day, source = Source.AUTO, confidence = Confidence.LOW) }
    }
}
