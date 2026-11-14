package com.oblonian.stracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SignalDao {

    @Insert
    suspend fun insertScreen(rows: List<ScreenEventEntity>)

    @Insert
    suspend fun insertApps(rows: List<AppUsageEventEntity>)

    @Insert
    suspend fun insertMotion(rows: List<MotionTransitionEntity>)

    @Insert
    suspend fun insertSteps(rows: List<StepSampleEntity>)

    @Insert
    suspend fun insertLocations(rows: List<LocationEntity>)

    @Query("SELECT * FROM screen_events WHERE time >= :from AND time < :to ORDER BY time")
    suspend fun screenBetween(from: Long, to: Long): List<ScreenEventEntity>

    @Query("SELECT * FROM app_usage_events WHERE time >= :from AND time < :to ORDER BY time")
    suspend fun appsBetween(from: Long, to: Long): List<AppUsageEventEntity>

    @Query("SELECT * FROM motion_transitions WHERE time >= :from AND time < :to ORDER BY time")
    suspend fun motionBetween(from: Long, to: Long): List<MotionTransitionEntity>

    @Query("SELECT * FROM step_samples WHERE time >= :from AND time < :to ORDER BY time")
    suspend fun stepsBetween(from: Long, to: Long): List<StepSampleEntity>

    @Query("SELECT * FROM locations WHERE time >= :from AND time < :to ORDER BY time")
    suspend fun locationsBetween(from: Long, to: Long): List<LocationEntity>

    /** The last motion transition strictly before [time]; the state the owner was in as [time] began. */
    @Query("SELECT * FROM motion_transitions WHERE time < :time ORDER BY time DESC LIMIT 1")
    suspend fun motionBefore(time: Long): MotionTransitionEntity?

    @Query("SELECT * FROM motion_transitions ORDER BY time DESC LIMIT 1")
    suspend fun lastMotion(): MotionTransitionEntity?

    @Query("SELECT MAX(time) FROM app_usage_events")
    suspend fun lastAppEventTime(): Long?

    @Query("SELECT MAX(time) FROM screen_events")
    suspend fun lastScreenEventTime(): Long?

    @Query("SELECT * FROM screen_events ORDER BY time")
    suspend fun allScreen(): List<ScreenEventEntity>

    @Query("SELECT * FROM app_usage_events ORDER BY time")
    suspend fun allApps(): List<AppUsageEventEntity>

    @Query("SELECT * FROM motion_transitions ORDER BY time")
    suspend fun allMotion(): List<MotionTransitionEntity>

    @Query("SELECT * FROM step_samples ORDER BY time")
    suspend fun allSteps(): List<StepSampleEntity>

    @Query("SELECT * FROM locations ORDER BY time")
    suspend fun allLocations(): List<LocationEntity>
}
