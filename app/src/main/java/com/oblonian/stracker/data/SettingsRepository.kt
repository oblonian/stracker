package com.oblonian.stracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oblonian.stracker.domain.InferenceSettings
import com.oblonian.stracker.domain.Thresholds
import com.oblonian.stracker.domain.WorkingHours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val onboardingDone: Boolean = false,
    val batteryStepDone: Boolean = false,
    /** A null value is a day off. */
    val workingHours: Map<DayOfWeek, WorkingHours?> = DEFAULT_WORKING_HOURS,
    val sleepWindowStart: LocalTime = Thresholds.SLEEP_WINDOW_START,
    val sleepWindowEnd: LocalTime = Thresholds.SLEEP_WINDOW_END,
    val lastInferenceDay: LocalDate? = null,
) {
    fun toInference(zone: ZoneId) = InferenceSettings(workingHours, sleepWindowStart, sleepWindowEnd, zone)

    companion object {
        val DEFAULT_WORKING_HOURS: Map<DayOfWeek, WorkingHours?> = DayOfWeek.entries.associateWith { d ->
            if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) null else WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0))
        }
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val BATTERY_STEP_DONE = booleanPreferencesKey("battery_step_done")
        val SLEEP_WINDOW_START = stringPreferencesKey("sleep_window_start")
        val SLEEP_WINDOW_END = stringPreferencesKey("sleep_window_end")
        val LAST_INFERENCE_DAY = stringPreferencesKey("last_inference_day")
        fun hours(day: DayOfWeek) = stringPreferencesKey("hours_${day.name.lowercase()}")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            batteryStepDone = prefs[Keys.BATTERY_STEP_DONE] ?: false,
            workingHours = DayOfWeek.entries.associateWith { d ->
                val raw = prefs[Keys.hours(d)]
                if (raw == null) UserSettings.DEFAULT_WORKING_HOURS[d] else parseHours(raw)
            },
            sleepWindowStart = prefs[Keys.SLEEP_WINDOW_START]?.let(LocalTime::parse) ?: Thresholds.SLEEP_WINDOW_START,
            sleepWindowEnd = prefs[Keys.SLEEP_WINDOW_END]?.let(LocalTime::parse) ?: Thresholds.SLEEP_WINDOW_END,
            lastInferenceDay = prefs[Keys.LAST_INFERENCE_DAY]?.let(LocalDate::parse),
        )
    }

    suspend fun setOnboardingDone(value: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = value }

    suspend fun setBatteryStepDone(value: Boolean) = context.dataStore.edit { it[Keys.BATTERY_STEP_DONE] = value }

    /** Stored as "HH:mm-HH:mm", or "off" for a day with no working hours. */
    suspend fun setWorkingHours(day: DayOfWeek, hours: WorkingHours?) = context.dataStore.edit {
        it[Keys.hours(day)] = hours?.let { h -> "${h.start}-${h.end}" } ?: OFF
    }

    suspend fun setSleepWindow(start: LocalTime, end: LocalTime) = context.dataStore.edit {
        it[Keys.SLEEP_WINDOW_START] = start.toString()
        it[Keys.SLEEP_WINDOW_END] = end.toString()
    }

    suspend fun setLastInferenceDay(day: LocalDate) = context.dataStore.edit { it[Keys.LAST_INFERENCE_DAY] = day.toString() }

    private fun parseHours(raw: String): WorkingHours? {
        if (raw == OFF) return null
        val (a, b) = raw.split("-")
        return WorkingHours(LocalTime.parse(a), LocalTime.parse(b))
    }

    private companion object {
        const val OFF = "off"
    }
}
