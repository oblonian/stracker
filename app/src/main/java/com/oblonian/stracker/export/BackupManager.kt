package com.oblonian.stracker.export

import androidx.room.withTransaction
import com.oblonian.stracker.data.SettingsRepository
import com.oblonian.stracker.data.db.AppDailyUsageEntity
import com.oblonian.stracker.data.db.AppDatabase
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
import com.oblonian.stracker.domain.WorkingHours
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/**
 * Everything on the phone as one JSON file: raw signals, episodes, events, places, overrides,
 * per-app usage, buttons and settings. Import replaces all of it (spec §6.7).
 */
object BackupManager {

    const val VERSION = 1

    suspend fun export(db: AppDatabase, settings: SettingsRepository, out: OutputStream) {
        val s = settings.settings.first()
        val root = JSONObject()
            .put("version", VERSION)
            .put("exportedAt", Instant.now().toEpochMilli())
            .put("settings", JSONObject().apply {
                put("sleepWindowStart", s.sleepWindowStart.toString())
                put("sleepWindowEnd", s.sleepWindowEnd.toString())
                put("workingHours", JSONObject().apply {
                    s.workingHours.forEach { (d, h) -> put(d.name, h?.let { "${it.start}-${it.end}" } ?: "off") }
                })
            })
            .put("screen", arr(db.signalDao().allScreen()) { r -> obj("time" to r.time, "on" to r.on) })
            .put("apps", arr(db.signalDao().allApps()) { r -> obj("time" to r.time, "pkg" to r.packageName, "fg" to r.foreground) })
            .put("motion", arr(db.signalDao().allMotion()) { r -> obj("time" to r.time, "motion" to r.motion) })
            .put("steps", arr(db.signalDao().allSteps()) { r -> obj("time" to r.time, "cumulative" to r.cumulative) })
            .put("locations", arr(db.signalDao().allLocations()) { r -> obj("time" to r.time, "lat" to r.lat, "lon" to r.lon, "acc" to r.accuracyM, "speed" to r.speedMps) })
            .put("episodes", arr(db.dayDao().allEpisodes()) { r ->
                obj("id" to r.id, "type" to r.type, "customName" to r.customName, "start" to r.start, "end" to r.end, "day" to r.day,
                    "source" to r.source, "confidence" to r.confidence, "placeId" to r.placeId, "label" to r.label,
                    "severity" to r.severity, "note" to r.note, "autoKey" to r.autoKey)
            })
            .put("events", arr(db.dayDao().allEvents()) { r ->
                obj("id" to r.id, "type" to r.type, "customName" to r.customName, "time" to r.time, "day" to r.day, "severity" to r.severity, "note" to r.note)
            })
            .put("places", arr(db.placeDao().all()) { r -> obj("id" to r.id, "name" to r.name, "lat" to r.lat, "lon" to r.lon, "radiusM" to r.radiusM, "tag" to r.tag) })
            .put("overrides", arr(db.dayDao().allOverrides()) { r ->
                obj("day" to r.day, "key" to r.episodeKey, "newType" to r.newType, "newStart" to r.newStart, "newEnd" to r.newEnd, "deleted" to r.deleted)
            })
            .put("appUsage", arr(db.dayDao().allAppUsage()) { r -> obj("day" to r.day, "pkg" to r.packageName, "millis" to r.foregroundMillis) })
            .put("buttons", arr(db.buttonDao().all()) { r ->
                obj("id" to r.id, "label" to r.label, "emoji" to r.emoji, "kind" to r.kind, "typeName" to r.typeName,
                    "customName" to r.customName, "sortOrder" to r.sortOrder, "hidden" to r.hidden)
            })
        out.bufferedWriter().use { it.write(root.toString()) }
        settings.setLastBackupAt(Instant.now())
    }

    /** Replaces every table and the settings with the file's contents. Throws on an unknown version. */
    suspend fun import(db: AppDatabase, settings: SettingsRepository, input: InputStream) {
        val root = JSONObject(input.bufferedReader().use { it.readText() })
        require(root.optInt("version", -1) == VERSION) { "Unsupported backup version ${root.opt("version")}" }

        db.withTransaction {
            db.backupDao().clearAll()
            db.signalDao().insertScreen(list(root, "screen") { ScreenEventEntity(time = it.getLong("time"), on = it.getBoolean("on")) })
            db.signalDao().insertApps(list(root, "apps") { AppUsageEventEntity(time = it.getLong("time"), packageName = it.getString("pkg"), foreground = it.getBoolean("fg")) })
            db.signalDao().insertMotion(list(root, "motion") { MotionTransitionEntity(time = it.getLong("time"), motion = it.getString("motion")) })
            db.signalDao().insertSteps(list(root, "steps") { StepSampleEntity(time = it.getLong("time"), cumulative = it.getLong("cumulative")) })
            db.signalDao().insertLocations(list(root, "locations") {
                LocationEntity(time = it.getLong("time"), lat = it.getDouble("lat"), lon = it.getDouble("lon"), accuracyM = it.getDouble("acc").toFloat(), speedMps = it.getDouble("speed").toFloat())
            })
            db.dayDao().insertEpisodes(list(root, "episodes") {
                EpisodeEntity(
                    id = it.getLong("id"), type = it.getString("type"), customName = it.optStringOrNull("customName"),
                    start = it.getLong("start"), end = it.optLongOrNull("end"), day = it.getString("day"),
                    source = it.getString("source"), confidence = it.getString("confidence"), placeId = it.optLongOrNull("placeId"),
                    label = it.optStringOrNull("label"), severity = it.optIntOrNull("severity"), note = it.optStringOrNull("note"),
                    autoKey = it.optStringOrNull("autoKey"),
                )
            })
            db.backupDao().insertEvents(list(root, "events") {
                EventEntity(id = it.getLong("id"), type = it.getString("type"), customName = it.optStringOrNull("customName"), time = it.getLong("time"),
                    day = it.getString("day"), severity = it.optIntOrNull("severity"), note = it.optStringOrNull("note"))
            })
            db.backupDao().insertPlaces(list(root, "places") {
                PlaceEntity(id = it.getLong("id"), name = it.getString("name"), lat = it.getDouble("lat"), lon = it.getDouble("lon"), radiusM = it.getDouble("radiusM"), tag = it.getString("tag"))
            })
            db.backupDao().insertOverrides(list(root, "overrides") {
                OverrideEntity(day = it.getString("day"), episodeKey = it.getString("key"), newType = it.optStringOrNull("newType"),
                    newStart = it.optLongOrNull("newStart"), newEnd = it.optLongOrNull("newEnd"), deleted = it.getBoolean("deleted"))
            })
            db.dayDao().insertAppUsage(list(root, "appUsage") { AppDailyUsageEntity(it.getString("day"), it.getString("pkg"), it.getLong("millis")) })
            db.buttonDao().insertAll(list(root, "buttons") {
                QuickLogButtonEntity(id = it.getLong("id"), label = it.getString("label"), emoji = it.getString("emoji"), kind = it.getString("kind"),
                    typeName = it.getString("typeName"), customName = it.optStringOrNull("customName"), sortOrder = it.getInt("sortOrder"), hidden = it.getBoolean("hidden"))
            })
        }

        root.optJSONObject("settings")?.let { s ->
            settings.setSleepWindow(LocalTime.parse(s.getString("sleepWindowStart")), LocalTime.parse(s.getString("sleepWindowEnd")))
            val hours = s.getJSONObject("workingHours")
            DayOfWeek.entries.forEach { d ->
                val raw = hours.optString(d.name, "off")
                settings.setWorkingHours(d, if (raw == "off") null else raw.split("-").let { (a, b) -> WorkingHours(LocalTime.parse(a), LocalTime.parse(b)) })
            }
        }
    }

    // ---- JSON helpers

    private fun <T> arr(rows: List<T>, map: (T) -> JSONObject) = JSONArray().also { a -> rows.forEach { a.put(map(it)) } }

    private fun obj(vararg pairs: Pair<String, Any?>) = JSONObject().also { o -> pairs.forEach { (k, v) -> o.put(k, v ?: JSONObject.NULL) } }

    private fun <T> list(root: JSONObject, key: String, map: (JSONObject) -> T): List<T> {
        val a = root.optJSONArray(key) ?: return emptyList()
        return (0 until a.length()).map { map(a.getJSONObject(it)) }
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
}
