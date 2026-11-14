package com.oblonian.stracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ---- Raw signals: appended as collected, never rewritten (spec §6.3). Times are epoch millis.

@Entity(tableName = "screen_events", indices = [Index("time")])
data class ScreenEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val on: Boolean,
)

@Entity(tableName = "app_usage_events", indices = [Index("time")])
data class AppUsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val packageName: String,
    val foreground: Boolean,
)

@Entity(tableName = "motion_transitions", indices = [Index("time")])
data class MotionTransitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    /** [com.oblonian.stracker.domain.Motion] name. */
    val motion: String,
)

@Entity(tableName = "step_samples", indices = [Index("time")])
data class StepSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val cumulative: Long,
)

@Entity(tableName = "locations", indices = [Index("time")])
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val speedMps: Float,
)

// ---- Derived and user-facing.

/** `day` is an ISO local date, e.g. 2026-11-12. */
@Entity(tableName = "episodes", indices = [Index("day"), Index("start")])
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val customName: String? = null,
    val start: Long,
    val end: Long?,
    val day: String,
    val source: String,
    val confidence: String,
    val placeId: Long? = null,
    val label: String? = null,
    val severity: Int? = null,
    val note: String? = null,
    /** For automatic episodes: the key inference gave it before any correction, so overrides match. */
    val autoKey: String? = null,
)

@Entity(tableName = "events", indices = [Index("day"), Index("time")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val customName: String? = null,
    val time: Long,
    val day: String,
    val severity: Int? = null,
    val note: String? = null,
)

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusM: Double,
    val tag: String,
)

@Entity(tableName = "app_daily_usage", primaryKeys = ["day", "packageName"])
data class AppDailyUsageEntity(
    val day: String,
    val packageName: String,
    val foregroundMillis: Long,
)

/** The owner's correction to an automatic episode, matched by key so re-running inference keeps it. */
@Entity(tableName = "overrides", indices = [Index("day")])
data class OverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    val episodeKey: String,
    val newType: String? = null,
    val newStart: Long? = null,
    val newEnd: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "quick_log_buttons")
data class QuickLogButtonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val emoji: String,
    /** "EVENT" or "EPISODE". */
    val kind: String,
    /** EventType or EpisodeType name, matching [kind]. */
    val typeName: String,
    val customName: String? = null,
    val sortOrder: Int,
    val hidden: Boolean = false,
)
