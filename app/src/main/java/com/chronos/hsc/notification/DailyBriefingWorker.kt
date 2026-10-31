package com.chronos.hsc.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chronos.hsc.ChronosApplication
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ChronosApplication ?: return Result.failure()
        val repo = app.container.studyRepository

        val todayEpochDay = LocalDate.now().toEpochDay()
        val todaySlices = repo.getTodaySlicesSnapshot(todayEpochDay)

        if (todaySlices.isNotEmpty()) {
            val pendingSlices = todaySlices.filter { !it.slice.isCompleted }
            if (pendingSlices.isNotEmpty()) {
                val preview = pendingSlices.take(3).joinToString(", ") { it.slice.shortBadge }
                val userStats = repo.observeUserStats().firstOrNull()
                val pinnedExam = repo.getPinnedExamSnapshot()

                val examSummary = pinnedExam?.let { exam ->
                    val daysLeft = ChronoUnit.DAYS.between(
                        LocalDate.ofEpochDay(todayEpochDay),
                        LocalDate.ofEpochDay(exam.targetDateEpochDay)
                    )
                    when {
                        daysLeft > 0L -> "${exam.examTitle}: ${daysLeft}d left"
                        daysLeft == 0L -> "${exam.examTitle}: TODAY!"
                        else -> null
                    }
                }

                ChronosNotificationHelper.sendMorningBriefingNotification(
                    context = applicationContext,
                    slicesCount = pendingSlices.size,
                    previewBadges = preview,
                    streakDays = userStats?.streakDays ?: 1,
                    examSummary = examSummary
                )
            }
        }

        return Result.success()
    }
}
