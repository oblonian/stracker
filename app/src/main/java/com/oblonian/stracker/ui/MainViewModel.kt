package com.oblonian.stracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oblonian.stracker.StrackerApp
import com.oblonian.stracker.collect.CollectorService
import com.oblonian.stracker.data.UserSettings
import com.oblonian.stracker.infer.InferenceScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class MainViewModel(private val app: StrackerApp) : ViewModel() {

    private val settingsRepository get() = app.container.settingsRepository

    /** Null until the settings have loaded, so the UI can avoid flashing onboarding. */
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The end of onboarding: remember it, start collecting, schedule the daily inference check. */
    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingDone(true)
            settingsRepository.setBatteryStepDone(true)
            CollectorService.start(app)
            InferenceScheduler.scheduleEndHour(app, settingsRepository, ZonedDateTime.now())
        }
    }
}
