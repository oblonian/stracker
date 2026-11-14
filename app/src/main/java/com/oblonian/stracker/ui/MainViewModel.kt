package com.oblonian.stracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oblonian.stracker.data.SettingsRepository
import com.oblonian.stracker.data.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    /** Null until the settings have loaded, so the UI can avoid flashing onboarding. */
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingDone(true) }
    }
}
