package com.oblonian.stracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oblonian.stracker.data.DayRepository
import com.oblonian.stracker.data.DaySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repository: DayRepository, private val clock: Clock) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now(clock))
    val month: StateFlow<YearMonth> = _month

    val days: StateFlow<Map<LocalDate, DaySummary>> = _month
        .flatMapLatest { repository.calendar(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setMonth(month: YearMonth) {
        _month.value = month
    }

    fun today(): LocalDate = LocalDate.now(clock)
}
