package com.chronos.hsc.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.UserStats
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object BitmapShareHelper {

    fun generateCyberpunkCardBitmap(
        userStats: UserStats,
        pinnedExam: ExamCountdownEntity?,
        todayEpochDay: Long,
        completedSlicesCount: Int,
        totalProblemsToday: Int,
        activeBossesCount: Int
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Pitch-Black Obsidian Background
        val bgPaint = Paint().apply { color = Color.parseColor("#080B12") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle Cyberpunk Radial/Linear glow at top
        val glowPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), 500f,
                Color.parseColor("#182A4A"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 500f, glowPaint)

        // Outer Neon Cyan Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(RectF(24f, 24f, width - 24f, height - 24f), 36f, 36f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        // Header Title
        textPaint.color = Color.parseColor("#00E5FF")
        textPaint.textSize = 54f
        canvas.drawText("CHRONOS HSC", 60f, 120f, textPaint)

        textPaint.color = Color.parseColor("#8E9EB8")
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("SMART STUDY ENGINE • DAILY PROGRESS PROTOCOL", 60f, 165f, textPaint)

        // Date Stamp
        val todayStr = LocalDate.ofEpochDay(todayEpochDay).format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        textPaint.color = Color.parseColor("#00E676")
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("📅 $todayStr", 60f, 220f, textPaint)

        // Card 1: Student Level, XP & Streak Hero Box
        val boxPaint = Paint().apply {
            color = Color.parseColor("#121929")
            style = Paint.Style.FILL
        }
        val boxBorder = Paint().apply {
            color = Color.parseColor("#20304D")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val heroRect = RectF(60f, 260f, width - 60f, 480f)
        canvas.drawRoundRect(heroRect, 24f, 24f, boxPaint)
        canvas.drawRoundRect(heroRect, 24f, 24f, boxBorder)

        textPaint.color = Color.parseColor("#D500F9")
        textPaint.textSize = 36f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("OPERATIVE RANK", 90f, 320f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 72f
        canvas.drawText("LEVEL ${userStats.currentLevel}", 90f, 400f, textPaint)

        textPaint.color = Color.parseColor("#00E5FF")
        textPaint.textSize = 34f
        canvas.drawText("${userStats.totalXp} Total XP", 90f, 450f, textPaint)

        // Streak Box on Right side of Hero
        textPaint.color = Color.parseColor("#FF6D00")
        textPaint.textSize = 48f
        canvas.drawText("🔥 ${userStats.streakDays}d Streak", width - 420f, 400f, textPaint)

        // Card 2: Pinned Exam Countdown Banner
        val examRect = RectF(60f, 510f, width - 60f, 690f)
        val examBoxPaint = Paint().apply {
            color = Color.parseColor("#141D30")
        }
        val examBorderPaint = Paint().apply {
            color = Color.parseColor("#FFAB00")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(examRect, 24f, 24f, examBoxPaint)
        canvas.drawRoundRect(examRect, 24f, 24f, examBorderPaint)

        textPaint.color = Color.parseColor("#FFAB00")
        textPaint.textSize = 30f
        canvas.drawText("⏳ TARGET EXAM COUNTDOWN", 90f, 565f, textPaint)

        if (pinnedExam != null) {
            val daysLeft = ChronoUnit.DAYS.between(
                LocalDate.ofEpochDay(todayEpochDay),
                LocalDate.ofEpochDay(pinnedExam.targetDateEpochDay)
            )
            textPaint.color = Color.WHITE
            textPaint.textSize = 58f
            canvas.drawText(pinnedExam.examTitle, 90f, 635f, textPaint)

            textPaint.color = Color.parseColor("#00E5FF")
            textPaint.textSize = 40f
            val countdownStr = when {
                daysLeft > 0L -> "$daysLeft Days Left"
                daysLeft == 0L -> "🎯 EXAM TODAY!"
                else -> "Completed"
            }
            canvas.drawText(countdownStr, width - 360f, 635f, textPaint)
        } else {
            textPaint.color = Color.parseColor("#8E9EB8")
            textPaint.textSize = 36f
            canvas.drawText("No target exam pinned yet", 90f, 635f, textPaint)
        }

        // Card 3: Today's Battleground Metrics Grid (3 columns)
        val metricsY = 720f
        val colW = (width - 120f - 40f) / 3f

        // Metric 1: Slices Cleared
        drawMetricBox(
            canvas = canvas,
            rect = RectF(60f, metricsY, 60f + colW, metricsY + 220f),
            label = "SLICES DONE",
            value = completedSlicesCount.toString(),
            colorHex = "#00E676"
        )

        // Metric 2: Problems Solved
        drawMetricBox(
            canvas = canvas,
            rect = RectF(60f + colW + 20f, metricsY, 60f + colW * 2 + 20f, metricsY + 220f),
            label = "PROBLEMS",
            value = totalProblemsToday.toString(),
            colorHex = "#00E5FF"
        )

        // Metric 3: Active Bosses
        drawMetricBox(
            canvas = canvas,
            rect = RectF(60f + colW * 2 + 40f, metricsY, width - 60f, metricsY + 220f),
            label = "BOSS CHAPTERS",
            value = activeBossesCount.toString(),
            colorHex = "#D500F9"
        )

        // Card 4: Motivational Creed & Seal
        val creedRect = RectF(60f, 970f, width - 60f, 1160f)
        val creedPaint = Paint().apply { color = Color.parseColor("#0E1626") }
        canvas.drawRoundRect(creedRect, 24f, 24f, creedPaint)

        textPaint.color = Color.parseColor("#00E5FF")
        textPaint.textSize = 34f
        canvas.drawText("🛡️ HSC SCIENCE CADET DISCIPLINE", 90f, 1030f, textPaint)

        textPaint.color = Color.parseColor("#E0E6F0")
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("\"Consistency outworks talent. Slicing chapters daily beats", 90f, 1080f, textPaint)
        canvas.drawText("the exam night panic.\" — Chronos Study Engine", 90f, 1120f, textPaint)

        // Footer Branding
        textPaint.color = Color.parseColor("#5A6E8C")
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("OFFLINE-FIRST JETPACK COMPOSE STUDY ENGINE • GITHUB: To_Do_Task_STUDY", 60f, 1260f, textPaint)

        return bitmap
    }

    private fun drawMetricBox(canvas: Canvas, rect: RectF, label: String, value: String, colorHex: String) {
        val paint = Paint().apply { color = Color.parseColor("#121929") }
        val border = Paint().apply {
            color = Color.parseColor(colorHex)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(rect, 20f, 20f, paint)
        canvas.drawRoundRect(rect, 20f, 20f, border)

        val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        tPaint.color = Color.parseColor(colorHex)
        tPaint.textSize = 24f
        canvas.drawText(label, rect.left + 24f, rect.top + 50f, tPaint)

        tPaint.color = Color.WHITE
        tPaint.textSize = 68f
        canvas.drawText(value, rect.left + 24f, rect.top + 140f, tPaint)
    }

    fun shareCardBitmap(context: Context, bitmap: Bitmap) {
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val imageFile = File(shareDir, "chronos_progress_card.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Chronos HSC Study Progress")
            putExtra(Intent.EXTRA_TEXT, "Conquering the HSC Syllabus with Chronos HSC: Smart Study Engine! ⚔️🔥")
            clipData = ClipData.newRawUri("Chronos Progress Card", contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Chronos Progress Card")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
