package com.oblonian.stracker.ui.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oblonian.stracker.data.DayRepository
import com.oblonian.stracker.domain.DayData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: DayRepository,
    private val clock: Clock,
) : ViewModel() {

    val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle.get<String>("date")))
    val zone: ZoneId = clock.zone

    val data: StateFlow<DayData?> = repository.day(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Opening a day is one of the moments inference runs (spec §6.4).
        viewModelScope.launch { runCatching { repository.recompute(date) } }
    }

    fun now(): Instant = clock.instant()
}
