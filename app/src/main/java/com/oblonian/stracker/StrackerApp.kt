package com.oblonian.stracker

import android.app.Application
import com.oblonian.stracker.collect.CollectorService
import com.oblonian.stracker.di.AppContainer
import com.oblonian.stracker.infer.InferenceScheduler
import com.oblonian.stracker.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File
import java.time.ZonedDateTime

class StrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        Notifications.createChannels(this)
        InferenceScheduler.install(this)

        appScope.launch {
            container.buttonRepository.seedDefaultsIfEmpty()
            if (container.settingsRepository.settings.first().onboardingDone) {
                CollectorService.start(this@StrackerApp)
                InferenceScheduler.scheduleEndHour(this@StrackerApp, container.settingsRepository, ZonedDateTime.now())
            }
        }
    }
}
