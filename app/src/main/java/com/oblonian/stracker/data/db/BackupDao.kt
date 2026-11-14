package com.oblonian.stracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Whole-table operations used only by backup and restore. */
@Dao
interface BackupDao {

    @Query("DELETE FROM screen_events") suspend fun clearScreen()
    @Query("DELETE FROM app_usage_events") suspend fun clearApps()
    @Query("DELETE FROM motion_transitions") suspend fun clearMotion()
    @Query("DELETE FROM step_samples") suspend fun clearSteps()
    @Query("DELETE FROM locations") suspend fun clearLocations()
    @Query("DELETE FROM episodes") suspend fun clearEpisodes()
    @Query("DELETE FROM events") suspend fun clearEvents()
    @Query("DELETE FROM places") suspend fun clearPlaces()
    @Query("DELETE FROM app_daily_usage") suspend fun clearAppUsage()
    @Query("DELETE FROM overrides") suspend fun clearOverrides()
    @Query("DELETE FROM quick_log_buttons") suspend fun clearButtons()

    suspend fun clearAll() {
        clearScreen(); clearApps(); clearMotion(); clearSteps(); clearLocations()
        clearEpisodes(); clearEvents(); clearPlaces(); clearAppUsage(); clearOverrides(); clearButtons()
    }

    @Insert suspend fun insertPlaces(rows: List<PlaceEntity>)
    @Insert suspend fun insertEvents(rows: List<EventEntity>)
    @Insert suspend fun insertOverrides(rows: List<OverrideEntity>)
}
