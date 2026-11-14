package com.oblonian.stracker.domain

import java.time.Instant

/** A stay inside a named place. */
data class Visit(val placeId: Long, val start: Instant, val end: Instant)

/** Travel, exercise and place visits from motion transitions and the GPS path. */
object MovementInference {

    fun motionIntervals(motion: List<MotionTransition>, window: DayWindow): List<MotionSpan> =
        Intervals.motionStates(motion, Span(window.start, window.end))

    /** Runs of samples inside a place's radius lasting at least [Thresholds.PLACE_VISIT_MIN]. */
    fun placeVisits(locations: List<LocationSample>, places: List<Place>, window: DayWindow): List<Visit> {
        if (places.isEmpty()) return emptyList()
        val visits = ArrayList<Visit>()
        var current: Place? = null
        var start: Instant? = null
        var last: Instant? = null
        fun close() {
            val p = current; val s = start; val e = last
            if (p != null && s != null && e != null && e.toEpochMilli() - s.toEpochMilli() >= Thresholds.PLACE_VISIT_MIN.toMillis()) {
                visits += Visit(p.id, s, e)
            }
            current = null; start = null; last = null
        }
        for (l in locations.filter { it.time >= window.start && it.time < window.end }.sortedBy { it.time }) {
            val here = places.filter { haversineMeters(l.lat, l.lon, it.lat, it.lon) <= it.radiusM }
                .minByOrNull { haversineMeters(l.lat, l.lon, it.lat, it.lon) }
            if (here?.id != current?.id) {
                close()
                if (here != null) { current = here; start = l.time }
            }
            if (here != null) last = l.time
        }
        close()
        return visits
    }

    /**
     * Vehicle and bicycle spans are travel. Walking is travel only when it moved the owner more than
     * [Thresholds.TRAVEL_WALK_MIN_METERS] in a straight line. Labelled "to <place>" when it ends in a visit.
     */
    fun travel(
        intervals: List<MotionSpan>,
        locations: List<LocationSample>,
        visits: List<Visit>,
        window: DayWindow,
        places: List<Place> = emptyList(),
    ): List<Episode> = intervals.mapNotNull { ms ->
        val isTravel = when (ms.motion) {
            Motion.IN_VEHICLE, Motion.ON_BICYCLE -> true
            Motion.WALKING -> displacement(locations, ms.span) > Thresholds.TRAVEL_WALK_MIN_METERS
            else -> false
        }
        if (!isTravel) return@mapNotNull null
        val arrival = visits.firstOrNull { ms.span.end >= it.start && ms.span.end <= it.end }
        val label = arrival?.let { v -> places.firstOrNull { it.id == v.placeId }?.let { "to ${it.name}" } }
        Episode(type = EpisodeType.TRAVEL, start = ms.span.start, end = ms.span.end, day = window.day, source = Source.AUTO, label = label, placeId = arrival?.placeId)
    }

    /** Running of any length, or walking of at least [Thresholds.WALK_EXERCISE_MIN]. */
    fun exercise(intervals: List<MotionSpan>, window: DayWindow): List<Episode> = intervals.mapNotNull { ms ->
        val label = when {
            ms.motion == Motion.RUNNING -> "run"
            ms.motion == Motion.WALKING && ms.span.millis >= Thresholds.WALK_EXERCISE_MIN.toMillis() -> "walk"
            else -> null
        } ?: return@mapNotNull null
        Episode(type = EpisodeType.EXERCISE, start = ms.span.start, end = ms.span.end, day = window.day, source = Source.AUTO, label = label)
    }

    private fun displacement(locations: List<LocationSample>, span: Span): Double {
        val inside = locations.filter { it.time >= span.start && it.time <= span.end }
        if (inside.size < 2) return 0.0
        val a = inside.first(); val b = inside.last()
        return haversineMeters(a.lat, a.lon, b.lat, b.lon)
    }
}
