package com.chronos.hsc

import android.app.Application
import com.chronos.hsc.di.AppContainer

class ChronosApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.chronos.hsc.data.store.CustomSubjectAndAiStore.init(this)
        com.chronos.hsc.data.store.BatchExamStore.init(this)
        container.firebaseSyncManager.initializeOnAppStart()
        com.chronos.hsc.notification.ChronosNotificationHelper.createNotificationChannels(this)
        com.chronos.hsc.notification.ChronosWorkScheduler.scheduleDailyBriefing(
            this,
            enabled = com.chronos.hsc.notification.ChronosWorkScheduler.isMorningBriefingEnabled(this),
            forceUpdate = false
        )
        com.chronos.hsc.notification.ChronosWorkScheduler.scheduleDominoAlerts(
            this,
            enabled = com.chronos.hsc.notification.ChronosWorkScheduler.isDominoAlertsEnabled(this),
            forceUpdate = false
        )
    }
}
