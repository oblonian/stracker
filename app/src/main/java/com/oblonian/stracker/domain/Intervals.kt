package com.oblonian.stracker.domain

import java.time.Duration
import java.time.Instant

/** A half-open span [start, end). */
data class Span(val start: Instant, val end: Instant) {
    val millis: Long get() = end.toEpochMilli() - start.toEpochMilli()
    fun overlaps(o: Span) = start < o.end && o.start < end
    fun clip(to: Span): Span? {
        val a = maxOf(start, to.start)
        val b = minOf(end, to.end)
        return if (b > a) Span(a, b) else null
    }
}

/** The motion the owner was in over a span. */
data class MotionSpan(val motion: Motion, val span: Span)

/** Small interval algebra shared by the inference rules. Inputs are sorted by time. */
object Intervals {

    /**
     * Screen-on spans inside [range]. An OFF with no earlier ON means the screen was on from the
     * range start; an ON with no later OFF runs to the range end.
     */
    fun screenOn(screen: List<ScreenEvent>, range: Span): List<Span> {
        val out = ArrayList<Span>()
        var onSince: Instant? = null
        var first = true
        for (e in screen.filter { it.time >= range.start && it.time < range.end }.sortedBy { it.time }) {
            if (e.on) {
                if (onSince == null) onSince = e.time
            } else {
                val start = onSince ?: if (first) range.start else null
                if (start != null) Span(start, e.time).clip(range)?.let(out::add)
                onSince = null
            }
            first = false
        }
        onSince?.let { Span(it, range.end).clip(range)?.let(out::add) }
        return out
    }

    /**
     * What the owner was doing across [range], carrying the state from the last transition before
     * the range start; UNKNOWN until the first transition when there is none.
     */
    fun motionStates(motion: List<MotionTransition>, range: Span): List<MotionSpan> {
        val sorted = motion.sortedBy { it.time }
        var state = sorted.lastOrNull { it.time <= range.start }?.motion ?: Motion.UNKNOWN
        var cursor = range.start
        val out = ArrayList<MotionSpan>()
        for (t in sorted.filter { it.time > range.start && it.time < range.end }) {
            if (t.time > cursor) out += MotionSpan(state, Span(cursor, t.time))
            state = t.motion
            cursor = t.time
        }
        if (range.end > cursor) out += MotionSpan(state, Span(cursor, range.end))
        return out
    }

    /** [range] minus every span in [cuts]. */
    fun subtract(range: Span, cuts: List<Span>): List<Span> {
        var remaining = listOf(range)
        for (cut in cuts.sortedBy { it.start }) {
            remaining = remaining.flatMap { r ->
                if (!r.overlaps(cut)) listOf(r)
                else listOfNotNull(
                    if (cut.start > r.start) Span(r.start, cut.start) else null,
                    if (cut.end < r.end) Span(cut.end, r.end) else null,
                )
            }
        }
        return remaining
    }

    /** Joins spans whose gap is at most [maxGap]. */
    fun mergeGaps(spans: List<Span>, maxGap: Duration): List<Span> {
        val out = ArrayList<Span>()
        for (s in spans.sortedBy { it.start }) {
            val last = out.lastOrNull()
            if (last != null && Duration.between(last.end, s.start) <= maxGap) {
                out[out.size - 1] = Span(last.start, maxOf(last.end, s.end))
            } else {
                out += s
            }
        }
        return out
    }

    /** How much of [span] is covered by [others]. */
    fun coveredMillis(span: Span, others: List<Span>): Long =
        others.sumOf { it.clip(span)?.millis ?: 0L }
}
