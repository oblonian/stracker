package com.oblonian.stracker.collect

import com.oblonian.stracker.domain.Motion
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationGateTest {

    @Test
    fun `still to walking starts walk interval`() {
        val gate = LocationGate()
        assertEquals(GateAction.NONE, gate.onMotion(Motion.STILL))
        assertEquals(GateAction.START_WALK, gate.onMotion(Motion.WALKING))
        assertEquals(10_000L, GateAction.START_WALK.intervalMillis)
        assertEquals(10f, GateAction.START_WALK.minDisplacementM)
    }

    @Test
    fun `walking to still requests a final fix then stops`() {
        val gate = LocationGate()
        gate.onMotion(Motion.WALKING)
        assertEquals(GateAction.FINAL_FIX_THEN_STOP, gate.onMotion(Motion.STILL))
        assertEquals(GateAction.NONE, gate.onMotion(Motion.STILL))
    }

    @Test
    fun `vehicle uses the five second interval and switching between moving states re-requests`() {
        val gate = LocationGate()
        assertEquals(GateAction.START_VEHICLE, gate.onMotion(Motion.IN_VEHICLE))
        assertEquals(5_000L, GateAction.START_VEHICLE.intervalMillis)
        assertEquals(GateAction.START_WALK, gate.onMotion(Motion.WALKING))
        assertEquals(GateAction.NONE, gate.onMotion(Motion.RUNNING))
        assertEquals(GateAction.START_VEHICLE, gate.onMotion(Motion.ON_BICYCLE))
    }

    @Test
    fun `unknown does not change state`() {
        val gate = LocationGate()
        gate.onMotion(Motion.WALKING)
        assertEquals(GateAction.NONE, gate.onMotion(Motion.UNKNOWN))
        assertEquals(GateAction.FINAL_FIX_THEN_STOP, gate.onMotion(Motion.STILL))
    }
}
