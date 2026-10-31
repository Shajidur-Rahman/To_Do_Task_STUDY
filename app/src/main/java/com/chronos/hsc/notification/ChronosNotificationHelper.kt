package com.chronos.hsc.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chronos.hsc.MainActivity
import com.chronos.hsc.R

object ChronosNotificationHelper {

    const val CHANNEL_MORNING_BRIEFING = "chronos_morning_briefing"
    const val CHANNEL_DOMINO_ALERTS = "chronos_domino_alerts"

    private const val NOTIF_ID_MORNING = 1001
    private const val NOTIF_ID_DOMINO = 1002
    private const val NOTIF_ID_TEST = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val morningChannel = NotificationChannel(
                CHANNEL_MORNING_BRIEFING,
                "Morning Battleground Briefing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily 08:00 AM summary of scheduled study slices, streak, and target exam countdown"
                enableVibration(true)
            }

            val dominoChannel = NotificationChannel(
                CHANNEL_DOMINO_ALERTS,
                "Domino Shift & Overdue Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nightly 09:00 PM alert when incomplete slices need a 1-tap Domino Shift"
                enableVibration(true)
            }

            manager.createNotificationChannel(morningChannel)
            manager.createNotificationChannel(dominoChannel)
        }
    }

    private fun getLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun hasNotificationPermission(context: Context): Boolean = canSendNotifications(context)

    fun canSendNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendMorningBriefingNotification(
        context: Context,
        slicesCount: Int,
        previewBadges: String,
        streakDays: Int,
        examSummary: String?
    ) {
        if (!canSendNotifications(context)) return

        val title = "⚔️ Today's Battleground: $slicesCount Slice${if (slicesCount > 1) "s" else ""} Scheduled"
        val examPart = if (examSummary != null) " • $examSummary" else ""
        val content = "[$previewBadges] • 🔥 ${streakDays}d Streak$examPart. Attack your syllabus!"

        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING_BRIEFING)
            .setSmallIcon(R.drawable.ic_chronos_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_MORNING, notification)
        } catch (_: SecurityException) {}
    }

    fun sendDominoAlertNotification(
        context: Context,
        overdueCount: Int,
        previewBadges: String
    ) {
        if (!canSendNotifications(context)) return

        val title = "⚡ Domino Alert: $overdueCount Missed Slice${if (overdueCount > 1) "s" else ""}"
        val content = "Missed: [$previewBadges]. Don't let tasks stack on tomorrow! Execute 1-Tap Domino Shift now."

        val notification = NotificationCompat.Builder(context, CHANNEL_DOMINO_ALERTS)
            .setSmallIcon(R.drawable.ic_chronos_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_DOMINO, notification)
        } catch (_: SecurityException) {}
    }

    fun sendImmediateTestNotification(context: Context) {
        if (!canSendNotifications(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING_BRIEFING)
            .setSmallIcon(R.drawable.ic_chronos_logo)
            .setContentTitle("🛡️ Chronos HSC: Notifications Active!")
            .setContentText("Morning Briefings (8:00 AM) and Evening Domino Alerts (9:00 PM) are armed and ready.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_TEST, notification)
        } catch (_: SecurityException) {}
    }
}
