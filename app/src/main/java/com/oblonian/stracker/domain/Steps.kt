package com.oblonian.stracker.domain

/** Daily steps from the phone's cumulative step counter, which resets on reboot. */
object Steps {

    fun forWindow(samples: List<StepSample>, window: DayWindow): Int {
        val inside = samples.filter { it.time >= window.start && it.time < window.end }.sortedBy { it.time }
        var total = 0L
        for (i in 1 until inside.size) {
            val delta = inside[i].cumulative - inside[i - 1].cumulative
            // A drop means the counter reset (reboot); the steps between the two samples are unknown, so count none.
            if (delta > 0) total += delta
        }
        return total.toInt()
    }
}
