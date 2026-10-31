package com.chronos.hsc.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.UserStats
import com.chronos.hsc.notification.ChronosNotificationHelper
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.CyberOrange
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountabilityCardModal(
    todayEpochDay: Long,
    userStats: UserStats,
    pinnedExam: ExamCountdownEntity?,
    completedSlicesCount: Int,
    totalProblemsToday: Int,
    activeBossesCount: Int,
    onDismiss: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDominoAlerts: (Boolean) -> Unit,
    morningBriefingEnabled: Boolean,
    dominoAlertsEnabled: Boolean
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackup(uri)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onRestoreBackup(uri)
        }
    }

    var morningEnabled by remember { mutableStateOf(morningBriefingEnabled) }
    var dominoEnabled by remember { mutableStateOf(dominoAlertsEnabled) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛡️ ACCOUNTABILITY, BACKUP & NOTIFICATIONS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = CyberCyan
                    )
                    Text(
                        text = "Share daily proof of work, safely back up data, and manage study alerts",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // 1. Cyberpunk Accountability Card Preview
            CyberpunkCardPreview(
                userStats = userStats,
                pinnedExam = pinnedExam,
                todayEpochDay = todayEpochDay,
                completedSlicesCount = completedSlicesCount,
                totalProblemsToday = totalProblemsToday,
                activeBossesCount = activeBossesCount
            )

            // Share Card Button
            Button(
                onClick = {
                    val bitmap = BitmapShareHelper.generateCyberpunkCardBitmap(
                        userStats = userStats,
                        pinnedExam = pinnedExam,
                        todayEpochDay = todayEpochDay,
                        completedSlicesCount = completedSlicesCount,
                        totalProblemsToday = totalProblemsToday,
                        activeBossesCount = activeBossesCount
                    )
                    BitmapShareHelper.shareCardBitmap(context, bitmap)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color(0xFF001F24)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Cyberpunk Progress Card (PNG)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            HorizontalDivider(color = ObsidianBorder)

            // 2. Offline JSON Backup & Restore Engine
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "💾 OFFLINE JSON DATABASE BACKUP & RESTORE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = CyberAmber
                )
                Text(
                    text = "Safely export your study chapters, task slices, exam countdowns, and neuro sessions into a portable JSON backup file.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultName = "Chronos_HSC_Backup_${LocalDate.now()}.json"
                            exportLauncher.launch(defaultName)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianBackground,
                            contentColor = CyberCyan
                        ),
                        border = BorderStroke(1.dp, CyberCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export (.json)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianBackground,
                            contentColor = CyberMagenta
                        ),
                        border = BorderStroke(1.dp, CyberMagenta),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Restore (.json)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3. Smart Scheduled Notifications Center
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔔 SMART STUDY ALERTS & BRIEFINGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = CyberEmerald
                    )
                    Button(
                        onClick = {
                            ChronosNotificationHelper.sendImmediateTestNotification(context)
                            Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberEmerald.copy(alpha = 0.16f),
                            contentColor = CyberEmerald
                        ),
                        border = BorderStroke(1.dp, CyberEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Test Alert 🔔", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Toggle 1: Morning Briefing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "08:00 AM Morning Battleground Briefing",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Overview of today's scheduled slices, streak, and days left to exam.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = morningEnabled,
                        onCheckedChange = {
                            morningEnabled = it
                            onToggleMorningBriefing(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberEmerald,
                            checkedTrackColor = CyberEmerald.copy(alpha = 0.25f)
                        )
                    )
                }

                // Toggle 2: Domino Overdue Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "09:00 PM Domino Shift Alert",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Alerts you if any incomplete slice is pending so you can 1-tap shift before bed.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = dominoEnabled,
                        onCheckedChange = {
                            dominoEnabled = it
                            onToggleDominoAlerts(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.25f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CyberpunkCardPreview(
    userStats: UserStats,
    pinnedExam: ExamCountdownEntity?,
    todayEpochDay: Long,
    completedSlicesCount: Int,
    totalProblemsToday: Int,
    activeBossesCount: Int
) {
    val todayDateStr = remember(todayEpochDay) {
        LocalDate.ofEpochDay(todayEpochDay).format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1726), Color(0xFF070B12))
                )
            )
            .border(2.dp, CyberCyan, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHRONOS HSC",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = CyberCyan
                )
                Text(
                    text = "DAILY PROOF OF WORK",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberMagenta.copy(alpha = 0.22f))
                    .border(1.dp, CyberMagenta, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LVL ${userStats.currentLevel}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = Color(0xFFF3C6FF)
                )
            }
        }

        Text(
            text = "📅 $todayDateStr",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = CyberEmerald
        )

        // Streak & Total XP Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianSurfaceElevated)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${userStats.totalXp} Total XP",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = "🔥 ${userStats.streakDays}d Streak",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = CyberOrange
            )
        }

        // Pinned Exam Countdown Badge
        if (pinnedExam != null) {
            val daysLeft = ChronoUnit.DAYS.between(
                LocalDate.ofEpochDay(todayEpochDay),
                LocalDate.ofEpochDay(pinnedExam.targetDateEpochDay)
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyberAmber.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, CyberAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ ${pinnedExam.examTitle}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (daysLeft > 0L) "$daysLeft Days Left" else "EXAM TODAY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = CyberAmber
                    )
                }
            }
        }

        // Today's Battleground Scoreboard (3 Pills)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScoreboardMiniBox(
                label = "SLICES",
                value = completedSlicesCount.toString(),
                color = CyberEmerald,
                modifier = Modifier.weight(1f)
            )
            ScoreboardMiniBox(
                label = "PROBLEMS",
                value = totalProblemsToday.toString(),
                color = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            ScoreboardMiniBox(
                label = "BOSSES",
                value = activeBossesCount.toString(),
                color = CyberMagenta,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "🛡️ \"Consistency outworks talent. Slicing chapters beats exam panic.\"",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScoreboardMiniBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = color
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = TextPrimary
        )
    }
}
