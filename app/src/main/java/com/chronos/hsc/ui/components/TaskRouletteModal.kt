package com.chronos.hsc.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSyllabusCatalog
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.service.GeminiRouletteService
import com.chronos.hsc.data.store.AiRouletteRecommendation
import com.chronos.hsc.data.store.BatchExamStore
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.WorkloadBucket
import com.chronos.hsc.ui.theme.AppThemeMode
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.LocalAppThemeMode
import com.chronos.hsc.ui.theme.NatureAutumnOrange
import com.chronos.hsc.ui.theme.NatureFireflyGold
import com.chronos.hsc.ui.theme.NatureLagRust
import com.chronos.hsc.ui.theme.NatureLeafGreen
import com.chronos.hsc.ui.theme.NatureSproutLime
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TerrariumBackground
import com.chronos.hsc.ui.theme.TerrariumBorder
import com.chronos.hsc.ui.theme.TerrariumSurface
import com.chronos.hsc.ui.theme.TerrariumSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import com.chronos.hsc.ui.theme.brandColor
import com.chronos.hsc.ui.theme.harmonizeForBlackUi
import com.chronos.hsc.ui.viewmodel.SprintTimerState
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TaskRouletteModal(
    incompleteSlices: List<SliceWithParentChain>,
    sprintTimerState: SprintTimerState?,
    onDismiss: () -> Unit,
    onLockedSlice: (SliceWithParentChain) -> Unit,
    onAcceptAndStartSprint: () -> Unit,
    onTogglePauseSprint: () -> Unit = {},
    onCompleteSprintSlice: (String) -> Unit,
    onStopSprint: () -> Unit,
    allTodayCompletedSlices: List<SliceWithParentChain> = emptyList(),
    batteryMode: EnergyLevel = EnergyLevel.HIGH_FOCUS,
    pinnedExam: ExamCountdownEntity? = null,
    userStreakDays: Int = 0,
    userLevel: Int = 1
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val isNature = LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER

    val windowBg = if (isNature) TerrariumBackground else ObsidianBackground
    val cardSurface = if (isNature) TerrariumSurface else ObsidianSurface
    val elevatedSurface = if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated
    val borderCol = if (isNature) TerrariumBorder else ObsidianBorder
    val primaryAccent = if (isNature) NatureLeafGreen else Color(0xFFFAFAFA)

    var isSpinning by remember { mutableStateOf(false) }
    var isAiAnalyzing by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var aiRecommendation by remember { mutableStateOf<AiRouletteRecommendation?>(CustomSubjectAndAiStore.getLastRecommendation()) }
    var highlightedIndex by remember { mutableIntStateOf(0) }
    var lockedItem by remember(sprintTimerState?.targetSliceId, incompleteSlices) {
        mutableStateOf(
            incompleteSlices.firstOrNull { it.slice.sliceId == sprintTimerState?.targetSliceId }
                ?: incompleteSlices.firstOrNull { it.slice.sliceId == aiRecommendation?.selectedSliceId }
        )
    }

    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    val activeExams = remember { BatchExamStore.getActiveUpcomingExams() }
    val comebackCount = remember(incompleteSlices, todayEpoch) {
        incompleteSlices.count { it.slice.scheduledDateEpochDay < todayEpoch || it.slice.effectiveLagDays > 0 }
    }

    val spinRoulette: () -> Unit = {
        if (!isSpinning && incompleteSlices.isNotEmpty()) {
            isSpinning = true
            isAiAnalyzing = true
            lockedItem = null
            scope.launch {
                val batteryPct = when (batteryMode) {
                    EnergyLevel.HIGH_FOCUS -> 100
                    EnergyLevel.MEDIUM -> 50
                    EnergyLevel.ZOMBIE_MODE -> 15
                }
                val aiDeferred = async(Dispatchers.IO) {
                    GeminiRouletteService.analyzeAndSpinLiveRoulette(
                        candidateSlices = incompleteSlices,
                        allTodayCompletedSlices = allTodayCompletedSlices,
                        batteryPercent = batteryPct,
                        batteryModeLabel = batteryMode.name,
                        pinnedExam = pinnedExam,
                        upcomingBatchExams = BatchExamStore.getActiveUpcomingExams(),
                        userStreakDays = userStreakDays,
                        userLevel = userLevel
                    )
                }

                val poolSize = incompleteSlices.size.coerceAtLeast(1)
                var delayMs = 45L
                var step = 0
                while (step < 16 || !aiDeferred.isCompleted) {
                    highlightedIndex = step % poolSize
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    delay(delayMs)
                    step++
                    if (step > 12) {
                        delayMs = (delayMs * 1.05f).toLong().coerceAtMost(160L)
                    }
                }

                val rec = try {
                    aiDeferred.await()
                } catch (_: Exception) {
                    null
                }

                aiRecommendation = rec
                isAiAnalyzing = false

                val winner = incompleteSlices.find { it.slice.sliceId == rec?.selectedSliceId }
                    ?: incompleteSlices.random()
                val targetWinnerIdx = incompleteSlices.indexOf(winner).coerceAtLeast(0)

                val currentMod = step % poolSize
                val stepsNeeded = ((targetWinnerIdx - currentMod + poolSize) % poolSize) + poolSize
                for (extra in 1..stepsNeeded) {
                    highlightedIndex = (step + extra) % poolSize
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    delay(delayMs)
                    delayMs = (delayMs * 1.15f).toLong().coerceAtMost(320L)
                }

                lockedItem = winner
                isSpinning = false
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onLockedSlice(winner)
            }
        }
    }

    val onTriggerSpin: () -> Unit = {
        if (!CustomSubjectAndAiStore.isRouletteUnlocked()) {
            showPasscodeDialog = true
        } else {
            spinRoulette()
        }
    }

    // Full-Screen Dedicated AI Subject & Task Roulette Window
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = windowBg,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Window Header Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardSurface,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = elevatedSurface,
                                border = BorderStroke(1.dp, borderCol),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        tint = primaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI SUBJECT & TASK STRATEGIST",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Live AI Roulette • Pace Reality Check • 2x XP Sprint",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "AI Settings",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Window",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Live Study Pace & Backlog Diagnostic Strip (Always Visible)
                val isBehindPace = comebackCount > 0 || (allTodayCompletedSlices.isEmpty() && incompleteSlices.size >= 4)
                val statusStripBorder = when {
                    isBehindPace -> if (isNature) NatureLagRust else CyberCrimson
                    allTodayCompletedSlices.size >= 2 -> if (isNature) NatureLeafGreen else CyberEmerald
                    else -> borderCol
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardSurface,
                    border = BorderStroke(1.dp, statusStripBorder.copy(alpha = 0.75f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PaceMetricChip(
                                label = "DONE TODAY",
                                value = "${allTodayCompletedSlices.size}",
                                valueColor = if (allTodayCompletedSlices.isNotEmpty()) CyberEmerald else TextSecondary,
                                bgColor = elevatedSurface
                            )
                            PaceMetricChip(
                                label = "WAITING",
                                value = "${incompleteSlices.size}",
                                valueColor = TextPrimary,
                                bgColor = elevatedSurface
                            )
                            PaceMetricChip(
                                label = "OVERDUE / LAG",
                                value = "$comebackCount",
                                valueColor = if (comebackCount > 0) CyberCrimson else TextSecondary,
                                bgColor = elevatedSurface
                            )
                            PaceMetricChip(
                                label = "STREAK",
                                value = "${userStreakDays}d",
                                valueColor = CyberAmber,
                                bgColor = elevatedSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val livePaceSummary = when {
                            comebackCount > 0 -> "⚠️ Caution: You have $comebackCount lagging task(s) from earlier days. Spin and finish a task now so you don't fall further behind!"
                            incompleteSlices.isEmpty() -> "🏆 Amazing work! You have completed every scheduled task in your queue today."
                            allTodayCompletedSlices.isEmpty() -> "⚡ Pace Alert: 0 tasks completed so far today with ${incompleteSlices.size} waiting. Start your first 25m sprint now to prevent tonight's backlog!"
                            else -> "🔥 Good momentum: ${allTodayCompletedSlices.size} done today! Keep going to clear the remaining ${incompleteSlices.size} task(s) on time."
                        }
                        Text(
                            text = livePaceSummary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isBehindPace) CyberCrimson else TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (incompleteSlices.isEmpty() && sprintTimerState == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardSurface)
                            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No incomplete slices on this date! Add a study topic chain first or switch dates to spin the AI Task Roulette.",
                            textAlign = TextAlign.Center,
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // 3. Spin Button (Prominent Action Trigger)
                    if (sprintTimerState?.isSprintRunning != true) {
                        Button(
                            onClick = { onTriggerSpin() },
                            enabled = !isSpinning && incompleteSlices.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpinning) CyberAmber else primaryAccent,
                                contentColor = if (isNature) Color(0xFF00210E) else Color(0xFF09090B)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpinning) Icons.Default.AutoAwesome else Icons.Default.Casino,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) {
                                    "🧠 AI ANALYZING PACE, LAG & SUBJECTS..."
                                } else {
                                    "🎲 SPIN AI SUBJECT & TASK ROULETTE"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 4. 60-Second Acceptance Window OR Active 25-Minute Micro-Sprint Timer
                    if (sprintTimerState != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardSurface)
                                .border(
                                    1.5.dp,
                                    if (sprintTimerState.isSprintRunning) CyberEmerald else CyberAmber,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!sprintTimerState.isSprintRunning) {
                                    Text(
                                        text = "⚡ 60s ACCEPTANCE WINDOW FOR 2X XP MULTIPLIER",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = CyberAmber
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sprintTimerState.targetSliceTitle,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${sprintTimerState.acceptanceSecondsRemaining}s remaining to claim 2x XP!",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (sprintTimerState.acceptanceSecondsRemaining > 10) TextPrimary else CyberCrimson
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            onAcceptAndStartSprint()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberEmerald,
                                            contentColor = Color(0xFF052E16)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Accept & Start 25m Sprint (2x XP)",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                } else {
                                    val mins = sprintTimerState.sprintSecondsRemaining / 60
                                    val secs = sprintTimerState.sprintSecondsRemaining % 60
                                    val timeFormatted = String.format("%02d:%02d", mins, secs)
                                    val sprintProgress by animateFloatAsState(
                                        targetValue = 1f - (sprintTimerState.sprintSecondsRemaining.toFloat() / (25f * 60f)),
                                        animationSpec = tween(300, easing = LinearEasing),
                                        label = "sprintProgress"
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = CyberAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (sprintTimerState.isSprintPaused) {
                                                "MICRO-SPRINT PAUSED • 2X XP"
                                            } else {
                                                "MICRO-SPRINT ACTIVE • 2X XP ATTACHED"
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = if (sprintTimerState.isSprintPaused) CyberAmber else CyberEmerald
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = timeFormatted,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 36.sp,
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = sprintTimerState.targetSliceTitle,
                                        fontSize = 12.5.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { sprintProgress },
                                        color = if (sprintTimerState.isSprintPaused) CyberAmber else CyberEmerald,
                                        trackColor = windowBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onStopSprint,
                                            border = BorderStroke(1.dp, borderCol),
                                            modifier = Modifier.weight(0.9f)
                                        ) {
                                            Text("Abort", color = TextMuted, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = onTogglePauseSprint,
                                            border = BorderStroke(1.dp, TextSecondary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (sprintTimerState.isSprintPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = "Pause/Resume",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (sprintTimerState.isSprintPaused) "Resume" else "Pause",
                                                color = TextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                onCompleteSprintSlice(sprintTimerState.targetSliceId)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberEmerald,
                                                contentColor = Color(0xFF052E16)
                                            ),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Done", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 5. Locked Winner Chapter Showcase & AI Coach Verdict ("HOW YOU ARE GOING" Caution / Encouragement)
                    if (!isSpinning && lockedItem != null && aiRecommendation != null) {
                        val winner = lockedItem!!
                        val rec = aiRecommendation!!
                        val winnerCustom = CustomSubjectAndAiStore.getCustomSubjectForChain(winner.chain.chainId)
                        val winnerSubName = winnerCustom?.customSubjectName ?: winner.chain.subject.fullName
                        val winnerPaper = if (winnerCustom != null) {
                            if (winnerCustom.hasSecondPaper && winner.chain.paper != HscPaper.SINGLE) "$winnerSubName • ${winner.chain.paper.compactTitleLabel}" else winnerSubName
                        } else if (winner.chain.subject.isSinglePaperOnly || winner.chain.paper == HscPaper.SINGLE) {
                            winnerSubName
                        } else {
                            "$winnerSubName • ${winner.chain.paper.compactTitleLabel}"
                        }
                        val winnerBanglaChapter = winner.chain.chapterName.trim().ifEmpty {
                            HscSyllabusCatalog.getChapterNameBangla(
                                winner.chain.subject,
                                winner.chain.paper,
                                winner.chain.chapterNumber
                            )
                        }
                        val winnerBnCh = HscSyllabusCatalog.toBanglaDigits(winner.chain.chapterNumber)

                        // A. Selected Subject & Chapter Hero Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardSurface,
                            border = BorderStroke(1.5.dp, primaryAccent.copy(alpha = 0.85f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎯 AI SELECTED SUBJECT & CHAPTER",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.5.sp,
                                        color = TextSecondary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = elevatedSurface,
                                        border = BorderStroke(1.dp, borderCol)
                                    ) {
                                        Text(
                                            text = "Day ${winner.slice.sliceNumber}/${winner.slice.totalSlicesInChain} • ${winner.slice.phaseTag}",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = primaryAccent
                                    ) {
                                        Text(
                                            text = "CH ${winner.chain.chapterNumber} • অধ্যায় $winnerBnCh",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = if (isNature) Color(0xFF00210E) else Color(0xFF09090B),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = winnerPaper.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = winnerBanglaChapter,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // B. AI COACH REALITY CHECK CARD ("HOW YOU ARE GOING" — Caution / Encouraging Feedback)
                        val isCriticalCaution = rec.coachStatusLevel.equals("CRITICAL_CAUTION", ignoreCase = true)
                        val isHighMomentum = rec.coachStatusLevel.equals("HIGH_MOMENTUM", ignoreCase = true)
                        val coachAccent = when {
                            isCriticalCaution -> CyberCrimson
                            isHighMomentum -> CyberEmerald
                            else -> CyberAmber
                        }
                        val coachBadgeLabel = when {
                            isCriticalCaution -> "⚠️ CAUTION • LAG & BACKLOG WARNING"
                            isHighMomentum -> "🔥 ENCOURAGEMENT • STRONG PACE"
                            else -> "⚡ PACE CHECK • STAY ON SCHEDULE"
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardSurface,
                            border = BorderStroke(1.5.dp, coachAccent.copy(alpha = 0.85f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCriticalCaution) Icons.Default.WarningAmber else Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = coachAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AI COACH: HOW YOU ARE GOING",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = coachAccent
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = coachAccent.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, coachAccent.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = coachBadgeLabel,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp,
                                            color = coachAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (rec.coachHeadline.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = rec.coachHeadline,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }

                                if (rec.coachHonestComment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rec.coachHonestComment,
                                        fontSize = 12.5.sp,
                                        color = TextPrimary.copy(alpha = 0.92f),
                                        lineHeight = 17.sp
                                    )
                                }

                                if (rec.lagConsequenceWarning.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(coachAccent.copy(alpha = 0.10f))
                                            .border(1.dp, coachAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "⏳ IF YOU DO NOT FINISH THIS TASK TODAY:",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                color = coachAccent
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = rec.lagConsequenceWarning,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // C. Deep AI Cognitive Strategy & 25-Minute Tactical Plan Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardSurface,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (rec.isOfflineFallback) "SMART COGNITIVE STRATEGIST" else "LIVE GEMINI AI • TELEMETRY",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.5.sp,
                                        color = TextSecondary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = elevatedSurface,
                                        border = BorderStroke(1.dp, borderCol)
                                    ) {
                                        Text(
                                            text = rec.cognitiveTransitionTag,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "🎯 Why this chapter now: ${rec.whyThisTaskNow}",
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )

                                if (rec.stepByStepSprintPlan.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(elevatedSurface)
                                            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "⏱️ 25-MINUTE MICRO-SPRINT GAMEPLAN",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = rec.stepByStepSprintPlan,
                                                fontSize = 11.5.sp,
                                                color = TextPrimary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "🧠 Circadian Note: ${rec.behaviorAnalysisNote}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "⏭️ Next after this: ${rec.nextRecommendedAfterThis}",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 15.sp
                                )

                                if (sprintTimerState?.isSprintRunning != true) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val currentWinner = lockedItem
                                            if (currentWinner != null) {
                                                CustomSubjectAndAiStore.addRejectedSlice(currentWinner.slice.sliceId)
                                            }
                                            onTriggerSpin()
                                        },
                                        border = BorderStroke(1.dp, borderCol),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "🎲 Re-Spin Another Subject (Skip & Rebalance)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 6. Candidate Pool List (With Clear Subject + Chapter Number + Chapter Name)
                    if (incompleteSlices.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardSurface,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CANDIDATE POOL (${incompleteSlices.size} SLICES)",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "3-Bucket Balanced Pool",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 260.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    incompleteSlices.forEachIndexed { idx, candidate ->
                                        val isHighlighted = (isSpinning && highlightedIndex % incompleteSlices.size == idx) ||
                                            (!isSpinning && lockedItem?.slice?.sliceId == candidate.slice.sliceId)
                                        val customLink = CustomSubjectAndAiStore.getCustomSubjectForChain(candidate.chain.chainId)
                                        val sColor = if (customLink != null) {
                                            Color(customLink.colorHex).harmonizeForBlackUi()
                                        } else {
                                            candidate.chain.subject.brandColor()
                                        }

                                        val bucket = when {
                                            candidate.slice.scheduledDateEpochDay < todayEpoch || candidate.slice.effectiveLagDays > 0 -> WorkloadBucket.COMEBACK_LEFTOVER
                                            activeExams.any { it.matchesSlice(candidate) } -> WorkloadBucket.BATCH_EXAM
                                            else -> WorkloadBucket.TODAY_NORMAL
                                        }
                                        val (badgeText, badgeBg, badgeTextColor) = when (bucket) {
                                            WorkloadBucket.COMEBACK_LEFTOVER -> Triple("🔥 LAG", CyberCrimson.copy(alpha = 0.16f), CyberCrimson)
                                            WorkloadBucket.BATCH_EXAM -> Triple("📝 EXAM", CyberAmber.copy(alpha = 0.16f), CyberAmber)
                                            WorkloadBucket.TODAY_NORMAL -> Triple("📅 TODAY", elevatedSurface, TextSecondary)
                                        }

                                        val chBangla = candidate.chain.chapterName.trim().ifEmpty {
                                            HscSyllabusCatalog.getChapterNameBangla(
                                                candidate.chain.subject,
                                                candidate.chain.paper,
                                                candidate.chain.chapterNumber
                                            )
                                        }
                                        val subShort = customLink?.customShortCode ?: candidate.chain.subject.shortCode

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isHighlighted) primaryAccent.copy(alpha = 0.14f) else elevatedSurface,
                                            border = BorderStroke(
                                                width = if (isHighlighted) 1.6.dp else 1.dp,
                                                color = if (isHighlighted) primaryAccent else borderCol
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = badgeBg,
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = badgeText,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeTextColor,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "$subShort • Ch ${candidate.chain.chapterNumber}",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = sColor,
                                                        fontSize = 11.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = chBangla,
                                                        fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        color = if (isHighlighted) TextPrimary else TextSecondary,
                                                        fontSize = 12.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (isHighlighted && !isSpinning && lockedItem != null) {
                                                    Text(
                                                        text = "🎯 LOCKED",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 10.sp,
                                                        color = primaryAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        AiRouletteSettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPasscodeDialog) {
        RouletteSecurityPasscodeDialog(
            onDismiss = { showPasscodeDialog = false },
            onUnlocked = {
                showPasscodeDialog = false
                spinRoulette()
            }
        )
    }
}

@Composable
private fun PaceMetricChip(
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = valueColor
            )
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.5.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun RouletteSecurityPasscodeDialog(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    var passcodeInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val view = LocalView.current

    val submitPasscode: () -> Unit = {
        if (CustomSubjectAndAiStore.verifyPasscode(passcodeInput)) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onUnlocked()
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            isError = true
            errorMessage = "Incorrect passcode. Please try again."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY ACCESS CODE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "API Key Spam Protection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Please enter the 4-digit security code to unlock the AI Task Roulette Engine. You only need to verify this once.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passcodeInput,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            passcodeInput = newValue
                            isError = false
                            if (newValue.length == 4) {
                                if (CustomSubjectAndAiStore.verifyPasscode(newValue)) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    onUnlocked()
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    isError = true
                                    errorMessage = "Incorrect passcode (4 digits). Try again."
                                }
                            }
                        }
                    },
                    label = { Text("4-Digit Passcode") },
                    placeholder = { Text("••••") },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitPasscode() }
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 6.sp,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        errorBorderColor = CyberCrimson,
                        focusedLabelColor = TextPrimary,
                        unfocusedLabelColor = TextSecondary,
                        errorLabelColor = CyberCrimson
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = CyberCrimson,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = submitPasscode,
                        enabled = passcodeInput.length == 4,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextPrimary,
                            contentColor = ObsidianBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
