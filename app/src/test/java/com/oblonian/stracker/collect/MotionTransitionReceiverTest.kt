package com.oblonian.stracker.collect

import android.app.Application
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.DetectedActivity
import com.oblonian.stracker.domain.Motion
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MotionTransitionReceiverTest {

    @Test
    fun `detected activities map to motions`() {
        assertEquals(Motion.STILL, MotionTransitionReceiver.toMotion(DetectedActivity.STILL))
        assertEquals(Motion.WALKING, MotionTransitionReceiver.toMotion(DetectedActivity.WALKING))
        assertEquals(Motion.RUNNING, MotionTransitionReceiver.toMotion(DetectedActivity.RUNNING))
        assertEquals(Motion.ON_BICYCLE, MotionTransitionReceiver.toMotion(DetectedActivity.ON_BICYCLE))
        assertEquals(Motion.IN_VEHICLE, MotionTransitionReceiver.toMotion(DetectedActivity.IN_VEHICLE))
        assertEquals(Motion.UNKNOWN, MotionTransitionReceiver.toMotion(DetectedActivity.TILTING))
    }

    @Test
    fun `only enter transitions become motion rows and exits are dropped`() {
        val bootMillis = 1_000_000L
        val enter = ActivityTransitionEvent(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER, 5_000_000_000L)
        val exit = ActivityTransitionEvent(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT, 5_000_000_000L)
        val rows = MotionTransitionReceiver.toTransitions(listOf(exit, enter), bootEpochMillis = bootMillis)
        assertEquals(1, rows.size)
        assertEquals(Motion.WALKING, rows[0].motion)
        // elapsed real-time nanos since boot → epoch millis
        assertEquals(bootMillis + 5_000L, rows[0].time.toEpochMilli())
    }
}
