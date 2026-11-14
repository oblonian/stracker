package com.oblonian.stracker.di

import android.content.Context
import com.oblonian.stracker.data.SettingsRepository

/** Hand-rolled dependency container; everything is created lazily on first use. */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
}
