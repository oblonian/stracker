package com.oblonian.stracker

import android.app.Application
import com.oblonian.stracker.di.AppContainer
import com.oblonian.stracker.notify.Notifications
import org.osmdroid.config.Configuration
import java.io.File

class StrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        Notifications.createChannels(this)
    }
}
