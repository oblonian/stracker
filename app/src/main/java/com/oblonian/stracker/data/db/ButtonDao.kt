package com.oblonian.stracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ButtonDao {

    @Query("SELECT * FROM quick_log_buttons ORDER BY sortOrder")
    fun observeAll(): Flow<List<QuickLogButtonEntity>>

    @Query("SELECT * FROM quick_log_buttons ORDER BY sortOrder")
    suspend fun all(): List<QuickLogButtonEntity>

    @Query("SELECT COUNT(*) FROM quick_log_buttons")
    suspend fun count(): Int

    @Insert
    suspend fun insert(row: QuickLogButtonEntity): Long

    @Insert
    suspend fun insertAll(rows: List<QuickLogButtonEntity>)

    @Update
    suspend fun update(row: QuickLogButtonEntity)

    @Query("DELETE FROM quick_log_buttons WHERE id = :id")
    suspend fun delete(id: Long)
}
