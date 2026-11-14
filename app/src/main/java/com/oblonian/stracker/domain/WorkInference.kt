package com.oblonian.stracker.domain

import java.time.LocalDate

/**
 * Work = inside the owner's working hours, phone idle, not moving — and, when a place is tagged
 * WORK, while at such a place. Without a work place the blocks are still produced but marked low
 * confidence (spec §6.4).
 */
object WorkInference {

    fun work(
        window: DayWindow,
        settings: InferenceSettings,
        phone: List<Episode>,
        intervals: List<MotionSpan>,
        visits: List<Visit>,
        places: List<Place>,
    ): List<Episode> {
        val range = Span(window.start, window.end)
        val hours = workingHourSpans(range, settings)
        if (hours.isEmpty()) return emptyList()

        val still = intervals.filter { it.motion == Motion.STILL || it.motion == Motion.UNKNOWN }.map { it.span }
        val phoneSpans = phone.mapNotNull { e -> e.end?.let { Span(e.start, it) } }
        val candidate = hours
            .flatMap { h -> still.mapNotNull { it.clip(h) } }
            .flatMap { Intervals.subtract(it, phoneSpans) }

        val workPlaces = places.filter { it.tag == PlaceTag.WORK }
        if (workPlaces.isEmpty()) {
            return Intervals.mergeGaps(candidate, Thresholds.WORK_GAP_MAX).map {
                Episode(type = EpisodeType.WORK, start = it.start, end = it.end, day = window.day, source = Source.AUTO, confidence = Confidence.LOW)
            }
        }
        return visits
            .filter { v -> workPlaces.any { it.id == v.placeId } }
            .flatMap { v ->
                val place = workPlaces.first { it.id == v.placeId }
                val inside = candidate.mapNotNull { it.clip(Span(v.start, v.end)) }
                Intervals.mergeGaps(inside, Thresholds.WORK_GAP_MAX).map {
                    Episode(
                        type = EpisodeType.WORK, start = it.start, end = it.end, day = window.day, source = Source.AUTO,
                        confidence = Confidence.HIGH, placeId = place.id, label = "work (${place.name})",
                    )
                }
            }
            .sortedBy { it.start }
    }

    /** The working-hour spans of every local date the range touches, clipped to the range. */
    fun workingHourSpans(range: Span, settings: InferenceSettings): List<Span> {
        val out = ArrayList<Span>()
        var date: LocalDate = range.start.atZone(settings.zone).toLocalDate()
        val lastDate = range.end.atZone(settings.zone).toLocalDate()
        while (!date.isAfter(lastDate)) {
            settings.workingHours[date.dayOfWeek]?.let { h ->
                val s = Span(date.atTime(h.start).atZone(settings.zone).toInstant(), date.atTime(h.end).atZone(settings.zone).toInstant())
                s.clip(range)?.let(out::add)
            }
            date = date.plusDays(1)
        }
        return out
    }
}
