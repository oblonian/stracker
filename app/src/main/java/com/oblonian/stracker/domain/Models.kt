package com.oblonian.stracker.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A stretch of time with a type. Automatic ones are recomputed; manual ones are the owner's. */
enum class EpisodeType { SLEEP, NAP, PHONE, WORK, TRAVEL, EXERCISE, EATING, SICK, PERIOD, CUSTOM }

/** A single moment the owner logged. */
enum class EventType { HEADACHE, FEVER, MEAL, COFFEE, WASHROOM, WATER, CUSTOM }

enum class Source { AUTO, AUTO_EDITED, MANUAL }

enum class Confidence { HIGH, LOW }

/** What the phone's activity recognition says the owner is doing. */
enum class Motion { STILL, WALKING, RUNNING, ON_BICYCLE, IN_VEHICLE, UNKNOWN }

data class Detail(val severity: Int? = null, val note: String? = null)

data class Episode(
    val id: Long = 0,
    val type: EpisodeType,
    val customName: String? = null,
    val start: Instant,
    /** Null while a manual episode is still going on. */
    val end: Instant?,
    /** The calendar day this belongs to (a night's sleep belongs to the wake-up day). */
    val day: LocalDate,
    val source: Source,
    val confidence: Confidence = Confidence.HIGH,
    val placeId: Long? = null,
    val label: String? = null,
    val detail: Detail? = null,
    /** The key inference gave an automatic episode before overrides were applied. */
    val autoKey: String? = null,
)

/** Stable identity of an automatic episode, so a correction survives re-running inference. */
fun Episode.key(): String = "$type:${start.toEpochMilli()}"

val Episode.durationMillis: Long
    get() = end?.let { it.toEpochMilli() - start.toEpochMilli() } ?: 0L

data class Event(
    val id: Long = 0,
    val type: EventType,
    val customName: String? = null,
    val time: Instant,
    val day: LocalDate,
    val detail: Detail? = null,
)

enum class PlaceTag { NONE, HOME, WORK }

data class Place(
    val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusM: Double,
    val tag: PlaceTag = PlaceTag.NONE,
)

// Raw signals, exactly as collected. Never rewritten.

data class ScreenEvent(val time: Instant, val on: Boolean)

data class AppUsageEvent(val time: Instant, val packageName: String, val foreground: Boolean)

/** The owner started doing [motion] at [time]. */
data class MotionTransition(val time: Instant, val motion: Motion)

data class StepSample(val time: Instant, val cumulative: Long)

data class LocationSample(
    val time: Instant,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val speedMps: Float,
)

/** Everything collected in a time range; every list is sorted by time. */
data class Signals(
    val screen: List<ScreenEvent> = emptyList(),
    val apps: List<AppUsageEvent> = emptyList(),
    val motion: List<MotionTransition> = emptyList(),
    val steps: List<StepSample> = emptyList(),
    val locations: List<LocationSample> = emptyList(),
)

data class WorkingHours(val start: LocalTime, val end: LocalTime)

data class InferenceSettings(
    /** A missing or null entry means a day off. */
    val workingHours: Map<DayOfWeek, WorkingHours?>,
    val sleepWindowStart: LocalTime = Thresholds.SLEEP_WINDOW_START,
    val sleepWindowEnd: LocalTime = Thresholds.SLEEP_WINDOW_END,
    val zone: ZoneId,
)

/**
 * The span of one calendar day as stracker sees it: from the start of the night's main sleep
 * to the start of the next one. Falls back to local midnight bounds when no sleep was found.
 */
data class DayWindow(val day: LocalDate, val start: Instant, val end: Instant, val wake: Instant?)

/** The owner's correction to an automatic episode, matched by [episodeKey]. */
data class Override(
    val id: Long = 0,
    val day: LocalDate,
    val episodeKey: String,
    val newType: EpisodeType? = null,
    val newStart: Instant? = null,
    val newEnd: Instant? = null,
    val deleted: Boolean = false,
)

data class AppUsage(val packageName: String, val foregroundMillis: Long)

/** Everything the day view shows. */
data class DayData(
    val window: DayWindow,
    val episodes: List<Episode>,
    val events: List<Event>,
    val appUsage: List<AppUsage>,
    val steps: Int,
    val path: List<LocationSample>,
)
