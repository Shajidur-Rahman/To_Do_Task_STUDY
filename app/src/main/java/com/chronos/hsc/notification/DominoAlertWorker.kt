package com.chronos.hsc.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chronos.hsc.ChronosApplication
import java.time.LocalDate

class DominoAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ChronosApplication ?: return Result.failure()
        val repo = app.container.studyRepository

        val todayEpochDay = LocalDate.now().toEpochDay()
        val overdueSlices = repo.getOverdueSlicesSnapshot(todayEpochDay)

        if (overdueSlices.isNotEmpty()) {
            val preview = overdueSlices.take(3).joinToString(", ") { it.slice.shortBadge }
            ChronosNotificationHelper.sendDominoAlertNotification(
                context = applicationContext,
                overdueCount = overdueSlices.size,
                previewBadges = preview
            )
        }

        return Result.success()
    }
}
