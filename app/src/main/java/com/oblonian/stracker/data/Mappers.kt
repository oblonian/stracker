package com.oblonian.stracker.data

import com.oblonian.stracker.data.db.AppDailyUsageEntity
import com.oblonian.stracker.data.db.AppUsageEventEntity
import com.oblonian.stracker.data.db.EpisodeEntity
import com.oblonian.stracker.data.db.EventEntity
import com.oblonian.stracker.data.db.LocationEntity
import com.oblonian.stracker.data.db.MotionTransitionEntity
import com.oblonian.stracker.data.db.OverrideEntity
import com.oblonian.stracker.data.db.PlaceEntity
import com.oblonian.stracker.data.db.QuickLogButtonEntity
import com.oblonian.stracker.data.db.ScreenEventEntity
import com.oblonian.stracker.data.db.StepSampleEntity
import com.oblonian.stracker.domain.AppUsage
import com.oblonian.stracker.domain.AppUsageEvent
import com.oblonian.stracker.domain.Confidence
import com.oblonian.stracker.domain.Detail
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Event
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.domain.LocationSample
import com.oblonian.stracker.domain.Motion
import com.oblonian.stracker.domain.MotionTransition
import com.oblonian.stracker.domain.Override
import com.oblonian.stracker.domain.Place
import com.oblonian.stracker.domain.PlaceTag
import com.oblonian.stracker.domain.ScreenEvent
import com.oblonian.stracker.domain.Source
import com.oblonian.stracker.domain.StepSample
import java.time.Instant
import java.time.LocalDate

// Raw signals

fun ScreenEvent.toEntity() = ScreenEventEntity(time = time.toEpochMilli(), on = on)
fun ScreenEventEntity.toDomain() = ScreenEvent(Instant.ofEpochMilli(time), on)

fun AppUsageEvent.toEntity() = AppUsageEventEntity(time = time.toEpochMilli(), packageName = packageName, foreground = foreground)
fun AppUsageEventEntity.toDomain() = AppUsageEvent(Instant.ofEpochMilli(time), packageName, foreground)

fun MotionTransition.toEntity() = MotionTransitionEntity(time = time.toEpochMilli(), motion = motion.name)
fun MotionTransitionEntity.toDomain() = MotionTransition(Instant.ofEpochMilli(time), Motion.valueOf(motion))

fun StepSample.toEntity() = StepSampleEntity(time = time.toEpochMilli(), cumulative = cumulative)
fun StepSampleEntity.toDomain() = StepSample(Instant.ofEpochMilli(time), cumulative)

fun LocationSample.toEntity() = LocationEntity(time = time.toEpochMilli(), lat = lat, lon = lon, accuracyM = accuracyM, speedMps = speedMps)
fun LocationEntity.toDomain() = LocationSample(Instant.ofEpochMilli(time), lat, lon, accuracyM, speedMps)

// Episodes and events

fun Episode.toEntity() = EpisodeEntity(
    id = id, type = type.name, customName = customName, start = start.toEpochMilli(), end = end?.toEpochMilli(),
    day = day.toString(), source = source.name, confidence = confidence.name, placeId = placeId, label = label,
    severity = detail?.severity, note = detail?.note, autoKey = autoKey,
)

fun EpisodeEntity.toDomain() = Episode(
    id = id, type = EpisodeType.valueOf(type), customName = customName, start = Instant.ofEpochMilli(start),
    end = end?.let(Instant::ofEpochMilli), day = LocalDate.parse(day), source = Source.valueOf(source),
    confidence = Confidence.valueOf(confidence), placeId = placeId, label = label,
    detail = detail(severity, note), autoKey = autoKey,
)

fun Event.toEntity() = EventEntity(
    id = id, type = type.name, customName = customName, time = time.toEpochMilli(), day = day.toString(),
    severity = detail?.severity, note = detail?.note,
)

fun EventEntity.toDomain() = Event(
    id = id, type = EventType.valueOf(type), customName = customName, time = Instant.ofEpochMilli(time),
    day = LocalDate.parse(day), detail = detail(severity, note),
)

private fun detail(severity: Int?, note: String?): Detail? =
    if (severity == null && note == null) null else Detail(severity, note)

// Places, overrides, usage, buttons

fun Place.toEntity() = PlaceEntity(id = id, name = name, lat = lat, lon = lon, radiusM = radiusM, tag = tag.name)
fun PlaceEntity.toDomain() = Place(id, name, lat, lon, radiusM, PlaceTag.valueOf(tag))

fun Override.toEntity() = OverrideEntity(
    id = id, day = day.toString(), episodeKey = episodeKey, newType = newType?.name,
    newStart = newStart?.toEpochMilli(), newEnd = newEnd?.toEpochMilli(), deleted = deleted,
)

fun OverrideEntity.toDomain() = Override(
    id = id, day = LocalDate.parse(day), episodeKey = episodeKey, newType = newType?.let(EpisodeType::valueOf),
    newStart = newStart?.let(Instant::ofEpochMilli), newEnd = newEnd?.let(Instant::ofEpochMilli), deleted = deleted,
)

fun AppUsage.toEntity(day: LocalDate) = AppDailyUsageEntity(day.toString(), packageName, foregroundMillis)
fun AppDailyUsageEntity.toDomain() = AppUsage(packageName, foregroundMillis)

enum class ButtonKind { EVENT, EPISODE }

data class QuickLogButton(
    val id: Long = 0,
    val label: String,
    val emoji: String,
    val kind: ButtonKind,
    val episodeType: EpisodeType? = null,
    val eventType: EventType? = null,
    val customName: String? = null,
    val sortOrder: Int,
    val hidden: Boolean = false,
)

fun QuickLogButton.toEntity() = QuickLogButtonEntity(
    id = id, label = label, emoji = emoji, kind = kind.name,
    typeName = when (kind) { ButtonKind.EVENT -> eventType!!.name; ButtonKind.EPISODE -> episodeType!!.name },
    customName = customName, sortOrder = sortOrder, hidden = hidden,
)

fun QuickLogButtonEntity.toDomain(): QuickLogButton {
    val k = ButtonKind.valueOf(kind)
    return QuickLogButton(
        id = id, label = label, emoji = emoji, kind = k,
        episodeType = if (k == ButtonKind.EPISODE) EpisodeType.valueOf(typeName) else null,
        eventType = if (k == ButtonKind.EVENT) EventType.valueOf(typeName) else null,
        customName = customName, sortOrder = sortOrder, hidden = hidden,
    )
}
