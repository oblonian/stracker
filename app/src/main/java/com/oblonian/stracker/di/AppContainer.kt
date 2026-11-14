package com.oblonian.stracker.di

import android.content.Context
import com.oblonian.stracker.data.ButtonRepository
import com.oblonian.stracker.data.PlaceRepository
import com.oblonian.stracker.data.SettingsRepository
import com.oblonian.stracker.data.SignalRepository
import com.oblonian.stracker.data.db.AppDatabase

/** Hand-rolled dependency container; everything is created lazily on first use. */
class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
    val signalRepository: SignalRepository by lazy { SignalRepository(database.signalDao()) }
    val placeRepository: PlaceRepository by lazy { PlaceRepository(database.placeDao()) }
    val buttonRepository: ButtonRepository by lazy { ButtonRepository(database.buttonDao()) }
}
