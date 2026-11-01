package com.chronos.hsc.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ChronosWorkScheduler {

    private const val WORK_MORNING_BRIEFING = "chronos_work_morning_briefing"
    private const val WORK_DOMINO_ALERT = "chronos_work_domino_alert"

    fun scheduleDailyBriefing(context: Context, enabled: Boolean, forceUpdate: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_MORNING_BRIEFING)
            return
        }

        val initialDelayMinutes = computeDelayUntilTargetTime(8, 0)
        val request = PeriodicWorkRequestBuilder<DailyBriefingWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_MORNING_BRIEFING,
            if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleDominoAlerts(context: Context, enabled: Boolean, forceUpdate: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_DOMINO_ALERT)
            return
        }

        val initialDelayMinutes = computeDelayUntilTargetTime(21, 0)
        val request = PeriodicWorkRequestBuilder<DominoAlertWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_DOMINO_ALERT,
            if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun computeDelayUntilTargetTime(targetHour: Int, targetMinute: Int): Long {
        val now = LocalDateTime.now()
        val targetToday = now.with(LocalTime.of(targetHour, targetMinute, 0))
        val targetTime = if (now.isAfter(targetToday)) {
            targetToday.plusDays(1)
        } else {
            targetToday
        }
        return Duration.between(now, targetTime).toMinutes().coerceAtLeast(1L)
    }
}
