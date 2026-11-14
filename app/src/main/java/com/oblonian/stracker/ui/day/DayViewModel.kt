package com.oblonian.stracker.ui.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oblonian.stracker.data.ButtonKind
import com.oblonian.stracker.data.ButtonRepository
import com.oblonian.stracker.data.DayRepository
import com.oblonian.stracker.data.QuickLogButton
import com.oblonian.stracker.domain.DayData
import com.oblonian.stracker.domain.Detail
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Event
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.domain.formatClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The last thing the owner logged, so a snackbar can offer "add details" and "undo". */
sealed class LastAction(val message: String) {
    class LoggedEvent(val id: Long, message: String) : LastAction(message)
    class StartedEpisode(val id: Long, message: String) : LastAction(message)
    class EndedEpisode(val id: Long, message: String) : LastAction(message)
}

class DayViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: DayRepository,
    private val clock: Clock,
    private val buttonRepository: ButtonRepository,
) : ViewModel() {

    val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle.get<String>("date")))
    val zone: ZoneId = clock.zone

    val data: StateFlow<DayData?> = repository.day(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val buttons: StateFlow<List<QuickLogButton>> = buttonRepository.buttons
        .map { list -> list.filter { !it.hidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ongoing: StateFlow<List<Episode>> = repository.ongoing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastAction = MutableStateFlow<LastAction?>(null)
    val lastAction: StateFlow<LastAction?> = _lastAction

    init {
        // Opening a day is one of the moments inference runs (spec §6.4).
        viewModelScope.launch { runCatching { repository.recompute(date) } }
    }

    fun now(): Instant = clock.instant()

    /** One tap: an event is logged; an episode starts, or ends if one of its kind is running. */
    fun tap(button: QuickLogButton, at: Instant?) {
        val t = at ?: now()
        viewModelScope.launch {
            val clockText = formatClock(t, zone)
            when (button.kind) {
                ButtonKind.EVENT -> {
                    val id = repository.logEvent(button.eventType!!, button.customName, t)
                    _lastAction.value = LastAction.LoggedEvent(id, "${button.label.replaceFirstChar { it.uppercase() }} logged at $clockText")
                }
                ButtonKind.EPISODE -> {
                    val running = repository.ongoingOnce()
                        .firstOrNull { it.type == button.episodeType && it.customName == button.customName }
                    if (running != null) {
                        // Set before ending: endEpisode re-runs inference, and undo must not wait on that.
                        _lastAction.value = LastAction.EndedEpisode(running.id, "${button.label.replaceFirstChar { it.uppercase() }} ended at $clockText")
                        repository.endEpisode(running.id, t)
                    } else {
                        val id = repository.startEpisode(button.episodeType!!, button.customName, t)
                        _lastAction.value = LastAction.StartedEpisode(id, "${button.label.replaceFirstChar { it.uppercase() }} started at $clockText")
                    }
                }
            }
        }
    }

    fun undoLast() {
        val action = _lastAction.value ?: return
        _lastAction.value = null
        viewModelScope.launch {
            when (action) {
                is LastAction.LoggedEvent -> repository.deleteEvent(action.id)
                is LastAction.StartedEpisode -> repository.deleteEpisode(action.id)
                is LastAction.EndedEpisode -> repository.reopenEpisode(action.id)
            }
        }
    }

    fun clearLastAction() {
        _lastAction.value = null
    }

    /** What the last action created, for "add details". */
    suspend fun lastItem(): DetailItem? = when (val a = _lastAction.value) {
        is LastAction.LoggedEvent -> repository.event(a.id)?.let { DetailItem.Ev(it) }
        is LastAction.StartedEpisode -> repository.episode(a.id)?.let { DetailItem.Ep(it) }
        is LastAction.EndedEpisode -> repository.episode(a.id)?.let { DetailItem.Ep(it) }
        null -> null
    }

    fun save(item: DetailItem, start: Instant, end: Instant?, detail: Detail?, customName: String?) {
        viewModelScope.launch {
            when (item) {
                is DetailItem.Ep -> {
                    val e = item.episode
                    val changedSpan = start != e.start || end != e.end || customName != e.customName
                    if (changedSpan) repository.updateEpisode(e.copy(start = start, end = end, customName = customName, detail = detail))
                    repository.setDetail(e.copy(start = start, end = end, customName = customName), detail)
                }
                is DetailItem.Ev -> repository.updateEvent(item.event.copy(time = start, detail = detail, customName = customName))
            }
        }
    }

    fun retype(item: DetailItem, type: Enum<*>) {
        viewModelScope.launch {
            when (item) {
                is DetailItem.Ep -> repository.retype(item.episode, type as EpisodeType, item.episode.customName.takeIf { type == EpisodeType.CUSTOM })
                is DetailItem.Ev -> repository.updateEvent(item.event.copy(type = type as EventType))
            }
        }
    }

    fun confirm(item: DetailItem) {
        if (item is DetailItem.Ep) viewModelScope.launch { repository.updateEpisode(item.episode) }
    }

    fun delete(item: DetailItem) {
        viewModelScope.launch {
            when (item) {
                is DetailItem.Ep -> repository.deleteEpisode(item.episode.id)
                is DetailItem.Ev -> repository.deleteEvent(item.event.id)
            }
        }
    }

    fun saveButton(button: QuickLogButton) {
        viewModelScope.launch { buttonRepository.save(button) }
    }

    fun hideButton(id: Long) {
        viewModelScope.launch { buttonRepository.setHidden(id, true) }
    }
}
