package com.oblonian.stracker.collect

import com.oblonian.stracker.domain.Motion

/** What the location collector should do after a motion change. */
enum class GateAction(val intervalMillis: Long, val minDisplacementM: Float) {
    /** Walking or running: a fix every 10 s or 10 m. */
    START_WALK(10_000L, 10f),
    /** Vehicle or bicycle: a fix every 5 s. */
    START_VEHICLE(5_000L, 0f),
    /** Came to rest: take one last fix, then stop asking for GPS. */
    FINAL_FIX_THEN_STOP(0L, 0f),
    NONE(0L, 0f),
}

/**
 * GPS is only requested while the owner is moving (spec §6.2). This pure state machine decides
 * when to start, re-tune and stop location updates from motion transitions alone.
 */
class LocationGate {

    private enum class Mode { REST, WALK, VEHICLE }

    private var mode = Mode.REST

    fun onMotion(motion: Motion): GateAction {
        val next = when (motion) {
            Motion.STILL -> Mode.REST
            Motion.WALKING, Motion.RUNNING -> Mode.WALK
            Motion.IN_VEHICLE, Motion.ON_BICYCLE -> Mode.VEHICLE
            Motion.UNKNOWN -> return GateAction.NONE
        }
        if (next == mode) return GateAction.NONE
        val previous = mode
        mode = next
        return when (next) {
            Mode.WALK -> GateAction.START_WALK
            Mode.VEHICLE -> GateAction.START_VEHICLE
            Mode.REST -> if (previous == Mode.REST) GateAction.NONE else GateAction.FINAL_FIX_THEN_STOP
        }
    }
}
