package com.oblonian.stracker.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oblonian.stracker.data.ButtonRepository
import com.oblonian.stracker.data.PlaceRepository
import com.oblonian.stracker.data.QuickLogButton
import com.oblonian.stracker.data.SettingsRepository
import com.oblonian.stracker.data.UserSettings
import com.oblonian.stracker.domain.Place
import com.oblonian.stracker.domain.WorkingHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsViewModel(
    private val app: Application,
    private val settingsRepository: SettingsRepository,
    private val placeRepository: PlaceRepository,
    private val buttonRepository: ButtonRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val places: StateFlow<List<Place>> = placeRepository.places
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val buttons: StateFlow<List<QuickLogButton>> = buttonRepository.buttons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _permissions = MutableStateFlow(Permissions.current(app))
    val permissions: StateFlow<List<PermissionStatus>> = _permissions

    /** Called when the screen comes back to the front: the owner may have changed something in system settings. */
    fun refreshPermissions() {
        _permissions.value = Permissions.current(app)
    }

    fun setWorkingHours(day: DayOfWeek, hours: WorkingHours?) = viewModelScope.launch { settingsRepository.setWorkingHours(day, hours) }

    fun copyMondayToWeekdays() = viewModelScope.launch {
        val monday = settings.value?.workingHours?.get(DayOfWeek.MONDAY) ?: return@launch
        listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY).forEach { settingsRepository.setWorkingHours(it, monday) }
    }

    fun setSleepWindow(start: LocalTime, end: LocalTime) = viewModelScope.launch { settingsRepository.setSleepWindow(start, end) }

    fun savePlace(place: Place) = viewModelScope.launch { if (place.id == 0L) placeRepository.add(place) else placeRepository.update(place) }

    fun deletePlace(id: Long) = viewModelScope.launch { placeRepository.delete(id) }

    fun saveButton(button: QuickLogButton) = viewModelScope.launch { buttonRepository.save(button) }

    fun setButtonHidden(id: Long, hidden: Boolean) = viewModelScope.launch { buttonRepository.setHidden(id, hidden) }

    fun moveButton(id: Long, up: Boolean) = viewModelScope.launch {
        val ids = buttons.value.map { it.id }.toMutableList()
        val i = ids.indexOf(id)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in ids.indices) return@launch
        ids[i] = ids[j].also { ids[j] = ids[i] }
        buttonRepository.reorder(ids)
    }

    fun deleteButton(id: Long) = viewModelScope.launch { buttonRepository.delete(id) }
}
