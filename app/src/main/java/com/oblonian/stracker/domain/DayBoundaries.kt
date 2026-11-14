package com.oblonian.stracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Where one calendar day ends and the next begins. stracker follows the sleep-diary rule: a
 * night's main sleep belongs to the day you wake up, so a day runs from the start of its main
 * sleep to the start of the next one. Without a main sleep a day is local midnight to midnight.
 */
object DayBoundaries {

    fun windows(days: List<LocalDate>, mainSleeps: List<Episode>, zone: ZoneId): List<DayWindow> {
        val sleepByDay = mainSleeps.filter { it.type == EpisodeType.SLEEP && it.end != null }.associateBy { it.day }
        return days.map { day ->
            val sleep = sleepByDay[day]
            val next = sleepByDay[day.plusDays(1)]
            val start = sleep?.start ?: midnight(day, zone)
            val end = next?.start ?: midnight(day.plusDays(1), zone)
            DayWindow(day = day, start = start, end = maxOf(end, start), wake = sleep?.end)
        }
    }

    /** The span in which [day]'s main sleep is looked for: the previous evening to this morning. */
    fun sleepSearchRange(day: LocalDate, settings: InferenceSettings): Pair<Instant, Instant> {
        val from = day.minusDays(1).atTime(settings.sleepWindowStart).atZone(settings.zone).toInstant()
        val to = day.atTime(settings.sleepWindowEnd).atZone(settings.zone).toInstant()
        return from to to
    }

    fun midnight(day: LocalDate, zone: ZoneId): Instant = day.atStartOfDay(zone).toInstant()
}
