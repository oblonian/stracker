package com.oblonian.stracker.domain

import java.time.Duration
import java.time.LocalTime

/**
 * Every number the inference rules depend on, in one place so they can be tuned.
 * Values from the design spec (§6.4).
 */
object Thresholds {
    /** A quiet stretch must be at least this long to count as the night's sleep. */
    val SLEEP_MIN: Duration = Duration.ofHours(3)

    /** Interruptions up to this long (bathroom, checking the time) stay inside the sleep block. */
    val SLEEP_GAP_MAX: Duration = Duration.ofMinutes(10)

    /** A daytime quiet stretch at least this long, outside working hours, is a nap candidate. */
    val NAP_MIN: Duration = Duration.ofMinutes(40)

    /** Breaks in a work block up to this long are bridged. */
    val WORK_GAP_MAX: Duration = Duration.ofMinutes(5)

    /** Continuous walking at least this long counts as exercise. */
    val WALK_EXERCISE_MIN: Duration = Duration.ofMinutes(20)

    /** Walking that moves the owner further than this between places is travel. */
    const val TRAVEL_WALK_MIN_METERS: Double = 400.0

    /** Staying inside a place's radius at least this long is a visit. */
    val PLACE_VISIT_MIN: Duration = Duration.ofMinutes(5)

    /** Where in the clock the night's main sleep is looked for. */
    val SLEEP_WINDOW_START: LocalTime = LocalTime.of(21, 0)
    val SLEEP_WINDOW_END: LocalTime = LocalTime.of(11, 0)
}
