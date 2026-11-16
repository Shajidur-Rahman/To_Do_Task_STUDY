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

    private const val PREFS_NAME = "chronos_notification_prefs"
    private const val KEY_MORNING_ENABLED = "key_morning_briefing_enabled"
    private const val KEY_DOMINO_ENABLED = "key_domino_alerts_enabled"

    fun isMorningBriefingEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MORNING_ENABLED, true)

    fun isDominoAlertsEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DOMINO_ENABLED, true)

    fun scheduleDailyBriefing(context: Context, enabled: Boolean, forceUpdate: Boolean = false) {
        if (forceUpdate) {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_MORNING_ENABLED, enabled).apply()
        }
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_MORNING_BRIEFING)
            return
        }

        val initialDelayMillis = computeDelayMillisUntilTargetTime(8, 0)
        val request = PeriodicWorkRequestBuilder<DailyBriefingWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_MORNING_BRIEFING,
            if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleDominoAlerts(context: Context, enabled: Boolean, forceUpdate: Boolean = false) {
        if (forceUpdate) {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DOMINO_ENABLED, enabled).apply()
        }
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_DOMINO_ALERT)
            return
        }

        val initialDelayMillis = computeDelayMillisUntilTargetTime(21, 0)
        val request = PeriodicWorkRequestBuilder<DominoAlertWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_DOMINO_ALERT,
            if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun computeDelayMillisUntilTargetTime(targetHour: Int, targetMinute: Int): Long {
        val now = LocalDateTime.now()
        val targetToday = now.with(LocalTime.of(targetHour, targetMinute, 0))
        val targetTime = if (!now.isBefore(targetToday)) {
            targetToday.plusDays(1)
        } else {
            targetToday
        }
        return Duration.between(now, targetTime).toMillis().coerceAtLeast(60_000L)
    }
}
