package com.oblonian.stracker.data

import com.oblonian.stracker.data.db.SignalDao
import com.oblonian.stracker.domain.MotionTransition
import com.oblonian.stracker.domain.Signals
import java.time.Instant

/** Append-only store of what the phone collected. */
class SignalRepository(private val dao: SignalDao) {

    suspend fun append(signals: Signals) {
        if (signals.screen.isNotEmpty()) dao.insertScreen(signals.screen.map { it.toEntity() })
        if (signals.apps.isNotEmpty()) dao.insertApps(signals.apps.map { it.toEntity() })
        if (signals.motion.isNotEmpty()) dao.insertMotion(signals.motion.map { it.toEntity() })
        if (signals.steps.isNotEmpty()) dao.insertSteps(signals.steps.map { it.toEntity() })
        if (signals.locations.isNotEmpty()) dao.insertLocations(signals.locations.map { it.toEntity() })
    }

    /** Everything in [from, to), each list sorted by time. */
    suspend fun signalsBetween(from: Instant, to: Instant): Signals {
        val f = from.toEpochMilli()
        val t = to.toEpochMilli()
        return Signals(
            screen = dao.screenBetween(f, t).map { it.toDomain() },
            apps = dao.appsBetween(f, t).map { it.toDomain() },
            motion = dao.motionBetween(f, t).map { it.toDomain() },
            steps = dao.stepsBetween(f, t).map { it.toDomain() },
            locations = dao.locationsBetween(f, t).map { it.toDomain() },
        )
    }

    /** The motion state the owner was in as [time] began, or null when nothing was recorded yet. */
    suspend fun motionBefore(time: Instant): MotionTransition? = dao.motionBefore(time.toEpochMilli())?.toDomain()

    suspend fun lastMotion(): MotionTransition? = dao.lastMotion()?.toDomain()

    suspend fun lastAppEventTime(): Instant? = dao.lastAppEventTime()?.let(Instant::ofEpochMilli)

    suspend fun lastScreenEventTime(): Instant? = dao.lastScreenEventTime()?.let(Instant::ofEpochMilli)
}
