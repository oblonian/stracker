package com.oblonian.stracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oblonian.stracker.StrackerApp
import com.oblonian.stracker.ui.calendar.CalendarViewModel
import com.oblonian.stracker.ui.day.DayViewModel
import com.oblonian.stracker.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            MainViewModel(app())
        }
        initializer {
            CalendarViewModel(app().container.dayRepository, app().container.clock)
        }
        initializer {
            DayViewModel(createSavedStateHandle(), app().container.dayRepository, app().container.clock, app().container.buttonRepository)
        }
        initializer {
            SettingsViewModel(app(), app().container.settingsRepository, app().container.placeRepository, app().container.buttonRepository)
        }
    }
}

internal fun CreationExtras.app(): StrackerApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StrackerApp
