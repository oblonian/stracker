package com.oblonian.stracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {

    // ---- episodes

    @Query("SELECT * FROM episodes WHERE day = :day ORDER BY start")
    fun episodesForDay(day: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE day = :day ORDER BY start")
    suspend fun episodesForDayOnce(day: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE day >= :from AND day <= :to ORDER BY start")
    fun episodesBetweenDays(from: String, to: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun episode(id: Long): EpisodeEntity?

    /** Episodes touching [from, to); an ongoing one (end NULL) counts as touching. */
    @Query("SELECT * FROM episodes WHERE start < :to AND (`end` IS NULL OR `end` > :from) ORDER BY start")
    fun episodesOverlapping(from: Long, to: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE `end` IS NULL AND source = 'MANUAL' ORDER BY start")
    fun ongoingManualEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE `end` IS NULL AND source = 'MANUAL' ORDER BY start")
    suspend fun ongoingManualEpisodesOnce(): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE source = 'MANUAL' AND start < :to AND (`end` IS NULL OR `end` > :from) ORDER BY start")
    suspend fun manualEpisodesOverlapping(from: Long, to: Long): List<EpisodeEntity>

    @Insert
    suspend fun insertEpisode(row: EpisodeEntity): Long

    @Insert
    suspend fun insertEpisodes(rows: List<EpisodeEntity>)

    @Update
    suspend fun updateEpisode(row: EpisodeEntity)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteEpisode(id: Long)

    @Query("DELETE FROM episodes WHERE day = :day AND source != 'MANUAL'")
    suspend fun deleteAutoEpisodes(day: String)

    /** Swaps the automatic episodes of [day] for [rows]; the owner's manual episodes stay. */
    @Transaction
    suspend fun replaceAutoEpisodes(day: String, rows: List<EpisodeEntity>) {
        deleteAutoEpisodes(day)
        insertEpisodes(rows)
    }

    // ---- events

    @Query("SELECT * FROM events WHERE day = :day ORDER BY time")
    fun eventsForDay(day: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE day >= :from AND day <= :to ORDER BY time")
    fun eventsBetweenDays(from: String, to: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun event(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE time >= :from AND time < :to ORDER BY time")
    fun eventsBetween(from: Long, to: Long): Flow<List<EventEntity>>

    @Insert
    suspend fun insertEvent(row: EventEntity): Long

    @Update
    suspend fun updateEvent(row: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    // ---- overrides

    @Query("SELECT * FROM overrides WHERE day = :day")
    suspend fun overridesForDay(day: String): List<OverrideEntity>

    @Query("DELETE FROM overrides WHERE day = :day AND episodeKey = :key")
    suspend fun deleteOverride(day: String, key: String)

    @Insert
    suspend fun insertOverride(row: OverrideEntity)

    /** One override per automatic episode: the newest replaces any older one for the same key. */
    @Transaction
    suspend fun upsertOverride(row: OverrideEntity) {
        deleteOverride(row.day, row.episodeKey)
        insertOverride(row)
    }

    // ---- per-app usage cache

    @Query("SELECT * FROM app_daily_usage WHERE day = :day ORDER BY foregroundMillis DESC")
    fun appUsageForDay(day: String): Flow<List<AppDailyUsageEntity>>

    @Query("DELETE FROM app_daily_usage WHERE day = :day")
    suspend fun deleteAppUsage(day: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(rows: List<AppDailyUsageEntity>)

    @Transaction
    suspend fun replaceAppUsage(day: String, rows: List<AppDailyUsageEntity>) {
        deleteAppUsage(day)
        insertAppUsage(rows)
    }

    // ---- everything, for backup

    @Query("SELECT * FROM episodes ORDER BY start")
    suspend fun allEpisodes(): List<EpisodeEntity>

    @Query("SELECT * FROM events ORDER BY time")
    suspend fun allEvents(): List<EventEntity>

    @Query("SELECT * FROM overrides")
    suspend fun allOverrides(): List<OverrideEntity>

    @Query("SELECT * FROM app_daily_usage")
    suspend fun allAppUsage(): List<AppDailyUsageEntity>
}
