package com.oblonian.stracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScreenEventEntity::class,
        AppUsageEventEntity::class,
        MotionTransitionEntity::class,
        StepSampleEntity::class,
        LocationEntity::class,
        EpisodeEntity::class,
        EventEntity::class,
        PlaceEntity::class,
        AppDailyUsageEntity::class,
        OverrideEntity::class,
        QuickLogButtonEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun signalDao(): SignalDao

    abstract fun dayDao(): DayDao

    abstract fun placeDao(): PlaceDao

    abstract fun buttonDao(): ButtonDao

    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        const val DATABASE_NAME = "stracker.db"

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                    .build()
                    .also { instance = it }
            }
    }
}
