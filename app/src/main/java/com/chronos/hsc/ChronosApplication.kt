package com.chronos.hsc

import android.app.Application
import com.chronos.hsc.di.AppContainer

class ChronosApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.chronos.hsc.notification.ChronosNotificationHelper.createNotificationChannels(this)
    }
}
