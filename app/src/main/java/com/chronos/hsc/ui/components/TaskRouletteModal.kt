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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.TextStyle
import com.chronos.hsc.data.service.GeminiRouletteService
import com.chronos.hsc.data.store.AiRouletteRecommendation
import com.chronos.hsc.data.store.BatchExamStore
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.WorkloadBucket
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import com.chronos.hsc.ui.theme.brandColor
import com.chronos.hsc.ui.viewmodel.SprintTimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun TaskRouletteModal(
    incompleteSlices: List<SliceWithParentChain>,
    sprintTimerState: SprintTimerState?,
    onDismiss: () -> Unit,
    onLockedSlice: (SliceWithParentChain) -> Unit,
    onAcceptAndStartSprint: () -> Unit,
    onTogglePauseSprint: () -> Unit = {},
    onCompleteSprintSlice: (String) -> Unit,
    onStopSprint: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var isSpinning by remember { mutableStateOf(false) }
    var isAiAnalyzing by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var aiRecommendation by remember { mutableStateOf<AiRouletteRecommendation?>(CustomSubjectAndAiStore.getLastRecommendation()) }
    var highlightedIndex by remember { mutableIntStateOf(0) }
    var lockedItem by remember(sprintTimerState?.targetSliceId, incompleteSlices) {
        mutableStateOf(
            incompleteSlices.firstOrNull { it.slice.sliceId == sprintTimerState?.targetSliceId }
        )
    }

    val spinRoulette: () -> Unit = {
        if (!isSpinning && incompleteSlices.isNotEmpty()) {
            isSpinning = true
            isAiAnalyzing = true
            lockedItem = null
            scope.launch {
                val aiDeferred = async(Dispatchers.IO) {
                    GeminiRouletteService.analyzeAndSpinLiveRoulette(
                        candidateSlices = incompleteSlices,
                        upcomingBatchExams = BatchExamStore.getActiveUpcomingExams()
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, CyberMagenta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = CyberMagenta
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TASK ROULETTE ENGINE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = CyberMagenta
                            )
                            Text(
                                text = "Beat decision paralysis • 2x XP Micro-Sprint",
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
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (incompleteSlices.isEmpty() && sprintTimerState == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianBackground)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No incomplete slices on this date yet! Add a study topic chain first to spin the Task Roulette.",
                            textAlign = TextAlign.Center,
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    if (incompleteSlices.isNotEmpty()) {
                        // Slot Wheel List of Candidate Slices
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianBackground)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            incompleteSlices.forEachIndexed { idx, candidate ->
                                val isHighlighted = (isSpinning && highlightedIndex % incompleteSlices.size == idx) ||
                                    (!isSpinning && lockedItem?.slice?.sliceId == candidate.slice.sliceId)
                                val customLink = CustomSubjectAndAiStore.getCustomSubjectForChain(candidate.chain.chainId)
                                val sColor = if (customLink != null) {
                                    Color(customLink.colorHex)
                                } else {
                                    candidate.chain.subject.brandColor()
                                }

                                val todayEpoch = remember { LocalDate.now().toEpochDay() }
                                val activeExams = remember { BatchExamStore.getActiveUpcomingExams() }
                                val bucket = when {
                                    candidate.slice.scheduledDateEpochDay < todayEpoch || candidate.slice.effectiveLagDays > 0 -> WorkloadBucket.COMEBACK_LEFTOVER
                                    activeExams.any { it.matchesSlice(candidate) } -> WorkloadBucket.BATCH_EXAM
                                    else -> WorkloadBucket.TODAY_NORMAL
                                }
                                val (badgeText, badgeBg, badgeTextColor) = when (bucket) {
                                    WorkloadBucket.COMEBACK_LEFTOVER -> Triple("🔥 COMEBACK", Color(0x33FF5252), Color(0xFFFF5252))
                                    WorkloadBucket.BATCH_EXAM -> Triple("📝 EXAM", Color(0x33E040FB), Color(0xFFE040FB))
                                    WorkloadBucket.TODAY_NORMAL -> Triple("📅 TODAY", Color(0x3300E5FF), Color(0xFF00E5FF))
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isHighlighted) sColor.copy(alpha = 0.24f) else ObsidianSurfaceElevated,
                                    border = BorderStroke(
                                        width = if (isHighlighted) 1.8.dp else 1.dp,
                                        color = if (isHighlighted) CyberAmber else ObsidianBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = candidate.slice.shortBadge,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = sColor,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = candidate.slice.displayTitle,
                                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isHighlighted) TextPrimary else TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                        if (isHighlighted && !isSpinning && lockedItem != null) {
                                            Text(
                                                text = "🎯 LOCKED",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                color = CyberAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Spin Button (when not currently running a 25-min sprint)
                    if (sprintTimerState?.isSprintRunning != true) {
                        Button(
                            onClick = { onTriggerSpin() },
                            enabled = !isSpinning && incompleteSlices.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpinning) CyberAmber else CyberMagenta,
                                contentColor = if (isSpinning) ObsidianBackground else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpinning) Icons.Default.AutoAwesome else Icons.Default.Casino,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "🧠 AI SYNAPSES FIRING... LIVE TELEMETRY" else "🎲 SPIN TASK ROULETTE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    // Rich AI Behavioral & Cognitive Insight Card
                    if (!isSpinning && lockedItem != null && aiRecommendation != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurfaceElevated,
                            border = BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (aiRecommendation?.isOfflineFallback == true) "COGNITIVE HEURISTIC" else "GEMINI 3.8 FLASH • LIVE TELEMETRY",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = CyberCyan
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CyberCyan.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = aiRecommendation?.cognitiveTransitionTag ?: "COGNITIVE REBOUND",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = CyberCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = aiRecommendation?.behaviorAnalysisNote ?: "",
                                    fontSize = 11.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "🎯 Why now: ${aiRecommendation?.whyThisTaskNow ?: ""}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "⏭️ Next recommended: ${aiRecommendation?.nextRecommendedAfterThis ?: ""}",
                                    fontSize = 10.5.sp,
                                    color = TextMuted,
                                    lineHeight = 14.sp
                                )

                                if (sprintTimerState?.isSprintRunning != true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val currentWinner = lockedItem
                                            if (currentWinner != null) {
                                                CustomSubjectAndAiStore.addRejectedSlice(currentWinner.slice.sliceId)
                                            }
                                            onTriggerSpin()
                                        },
                                        border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.7f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "🎲 Re-Spin (Skip & Rebalance)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 60-Second Acceptance Window OR Active 25-Minute Micro-Sprint Timer
                    if (sprintTimerState != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianSurfaceElevated)
                                .border(
                                    1.5.dp,
                                    if (sprintTimerState.isSprintRunning) CyberEmerald else CyberAmber,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!sprintTimerState.isSprintRunning) {
                                    // 60-Second Acceptance Window
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${sprintTimerState.acceptanceSecondsRemaining}s remaining to claim 2x XP!",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (sprintTimerState.acceptanceSecondsRemaining > 10) CyberCyan else CyberCrimson
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            onAcceptAndStartSprint()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberEmerald,
                                            contentColor = Color(0xFF00210E)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Accept & Start 25m Sprint (2x XP)",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                } else {
                                    // Built-in 25-Minute Micro-Sprint Countdown Timer
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
                                        fontSize = 34.sp,
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = sprintTimerState.targetSliceTitle,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { sprintProgress },
                                        color = if (sprintTimerState.isSprintPaused) CyberAmber else CyberEmerald,
                                        trackColor = ObsidianBackground,
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
                                            border = BorderStroke(1.dp, ObsidianBorder),
                                            modifier = Modifier.weight(0.9f)
                                        ) {
                                            Text("Abort", color = TextMuted, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = onTogglePauseSprint,
                                            border = BorderStroke(1.dp, CyberCyan),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (sprintTimerState.isSprintPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = "Pause/Resume",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (sprintTimerState.isSprintPaused) "Resume" else "Pause",
                                                color = CyberCyan,
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
                                                contentColor = Color(0xFF00210E)
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
            border = BorderStroke(1.5.dp, CyberMagenta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY ACCESS CODE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = CyberMagenta
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyberMagenta.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.35f)),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyberMagenta,
                            modifier = Modifier.size(30.dp)
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
                        focusedBorderColor = CyberMagenta,
                        unfocusedBorderColor = ObsidianBorder,
                        errorBorderColor = CyberCrimson,
                        focusedLabelColor = CyberMagenta,
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
                            containerColor = CyberMagenta,
                            contentColor = Color.White
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
