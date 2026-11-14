package com.oblonian.stracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oblonian.stracker.StrackerApp

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            MainViewModel(settingsRepository = app().container.settingsRepository)
        }
    }
}

internal fun CreationExtras.app(): StrackerApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StrackerApp
