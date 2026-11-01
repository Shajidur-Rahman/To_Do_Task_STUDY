package com.chronos.hsc.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.data.local.CognitiveProblemCategory
import com.chronos.hsc.data.local.NeuroDrillType
import com.chronos.hsc.domain.NeuroTelemetrySummary
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun NeuroGymScreen(
    telemetry: NeuroTelemetrySummary,
    onDrillCompleted: (
        drillType: NeuroDrillType,
        scoreMetric: Double,
        durationSeconds: Int,
        completionMessage: String
    ) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(CognitiveProblemCategory.LOST_FOCUS)
    }
    var activeDrill by remember { mutableStateOf<NeuroDrillType?>(null) }

    AnimatedContent(
        targetState = activeDrill,
        label = "neuroGymDrillTransition"
    ) { currentDrill ->
        if (currentDrill == null) {
            NeuroGymHubContent(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                telemetry = telemetry,
                onLaunchDrill = { activeDrill = it }
            )
        } else {
            when (currentDrill) {
                NeuroDrillType.FOVEAL_LOCK -> FovealSingularityLockDrill(
                    onBack = { activeDrill = null },
                    onComplete = { durationSec ->
                        onDrillCompleted(
                            NeuroDrillType.FOVEAL_LOCK,
                            durationSec.toDouble(),
                            durationSec,
                            "Foveal Singularity Lock (${durationSec}s) Complete!"
                        )
                    }
                )

                NeuroDrillType.SCHULTE_GRID -> SchulteGridSpeedTrainerDrill(
                    personalBestSeconds = telemetry.fastestSchulteSeconds,
                    onBack = { activeDrill = null },
                    onComplete = { totalSeconds ->
                        onDrillCompleted(
                            NeuroDrillType.SCHULTE_GRID,
                            totalSeconds,
                            totalSeconds.roundToInt().coerceAtLeast(1),
                            "5x5 Schulte Grid Cleared in ${String.format("%.2f", totalSeconds)}s!"
                        )
                    }
                )

                NeuroDrillType.QUANT_REFLEX -> QuantReflexVelocityBlitzDrill(
                    personalBestQpm = telemetry.highestQuantQpm.toDouble(),
                    onBack = { activeDrill = null },
                    onComplete = { qpmScore ->
                        onDrillCompleted(
                            NeuroDrillType.QUANT_REFLEX,
                            qpmScore,
                            60,
                            "Quant-Reflex Blitz Complete: ${qpmScore.roundToInt()} QPM!"
                        )
                    }
                )

                NeuroDrillType.SPATIAL_N_BACK -> SpatialNBackMatrixDrill(
                    onBack = { activeDrill = null },
                    onComplete = { accuracyPercent, nLevel ->
                        onDrillCompleted(
                            NeuroDrillType.SPATIAL_N_BACK,
                            accuracyPercent,
                            55,
                            "Spatial ${nLevel}-Back Complete: ${accuracyPercent.roundToInt()}% Accuracy!"
                        )
                    }
                )

                NeuroDrillType.CHIMP_MEMORY -> ChimpFlashMemoryVaultDrill(
                    personalBestLevel = telemetry.maxChimpMemorySpan,
                    onBack = { activeDrill = null },
                    onComplete = { maxLevelReached, elapsedSec ->
                        onDrillCompleted(
                            NeuroDrillType.CHIMP_MEMORY,
                            maxLevelReached.toDouble(),
                            elapsedSec,
                            "Chimp Flash Memory Vault: Reached Level $maxLevelReached!"
                        )
                    }
                )

                NeuroDrillType.OCULOMOTOR_SIGH -> OculomotorFigure8PursuitDrill(
                    onBack = { activeDrill = null },
                    onComplete = {
                        onDrillCompleted(
                            NeuroDrillType.OCULOMOTOR_SIGH,
                            100.0,
                            60,
                            "60s Oculomotor Figure-8 & Physiological Sigh Reset Complete!"
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun NeuroGymHubContent(
    selectedCategory: CognitiveProblemCategory,
    onSelectCategory: (CognitiveProblemCategory) -> Unit,
    telemetry: NeuroTelemetrySummary,
    onLaunchDrill: (NeuroDrillType) -> Unit
) {
    val prescribedSet = selectedCategory.recommendedDrills.toSet()
    val orderedDrills = remember(selectedCategory) {
        val prescribed = NeuroDrillType.entries.filter { it in prescribedSet }
        val others = NeuroDrillType.entries.filter { it !in prescribedSet }
        prescribed + others
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ObsidianSurfaceElevated, ObsidianSurface)
                        )
                    )
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🧠 NEURO-GYM & COGNITIVE FOCUS LAB",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Evidence-based oculomotor, foveal lock, and working-memory protocols (+25 XP per completed session).",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberMagenta.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CyberMagenta)
                    ) {
                        Text(
                            text = "${telemetry.totalSessionsCompleted} Runs",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color(0xFFF3C6FF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 1. Symptom-Driven Cognitive Diagnostic Selector
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "WHAT COGNITIVE PROBLEM ARE YOU FACING RIGHT NOW?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = CyberAmber
                )

                CognitiveProblemCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            CyberCyan.copy(alpha = 0.16f)
                        } else {
                            ObsidianBackground
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) CyberCyan else ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCategory(category) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${category.badgeEmoji} ${category.symptomTitle}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) CyberCyan else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = category.prescriptionNote,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberCyan.copy(alpha = 0.24f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        color = CyberCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Interactive Drill Cards List (Prescribed first)
        item {
            Text(
                text = "INTERACTIVE COGNITIVE PROTOCOLS (6 DRILLS)",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = CyberCyan
            )
        }

        items(
            count = orderedDrills.size,
            key = { index -> orderedDrills[index].name }
        ) { index ->
            val drill = orderedDrills[index]
            val isPrescribed = drill in prescribedSet
            NeuroDrillLaunchCard(
                drill = drill,
                isPrescribed = isPrescribed,
                telemetry = telemetry,
                onLaunch = { onLaunchDrill(drill) }
            )
        }

        // 3. Brain Muscle Telemetry Card (Bottom of NeuroGymScreen)
        item {
            BrainMuscleTelemetryCard(telemetry = telemetry)
        }
    }
}

@Composable
private fun NeuroDrillLaunchCard(
    drill: NeuroDrillType,
    isPrescribed: Boolean,
    telemetry: NeuroTelemetrySummary,
    onLaunch: () -> Unit
) {
    val accentColor = if (isPrescribed) CyberCyan else CyberMagenta
    val drillEmoji = when (drill) {
        NeuroDrillType.FOVEAL_LOCK -> "🎯"
        NeuroDrillType.SCHULTE_GRID -> "👁️"
        NeuroDrillType.QUANT_REFLEX -> "⚡"
        NeuroDrillType.SPATIAL_N_BACK -> "💪"
        NeuroDrillType.CHIMP_MEMORY -> "🦍"
        NeuroDrillType.OCULOMOTOR_SIGH -> "🌊"
    }
    val personalMetricText = when (drill) {
        NeuroDrillType.FOVEAL_LOCK ->
            "Total Locked: ${telemetry.totalFocusLockMinutes} min (${telemetry.totalFocusLockSeconds}s)"
        NeuroDrillType.SCHULTE_GRID ->
            telemetry.fastestSchulteSeconds?.let { "Best Time: ${String.format("%.2f", it)}s" }
                ?: "Best Time: Not set yet"
        NeuroDrillType.QUANT_REFLEX ->
            if (telemetry.highestQuantQpm > 0) {
                "Peak Velocity: ${telemetry.highestQuantQpm} QPM"
            } else {
                "Peak Velocity: Not set yet"
            }
        NeuroDrillType.SPATIAL_N_BACK ->
            if (telemetry.highestNBackAccuracyPercent > 0) {
                "Best Accuracy: ${telemetry.highestNBackAccuracyPercent}%"
            } else {
                "Best Accuracy: Not set yet"
            }
        NeuroDrillType.CHIMP_MEMORY ->
            if (telemetry.maxChimpMemorySpan > 0) {
                "Max Vault Span: Level ${telemetry.maxChimpMemorySpan}"
            } else {
                "Max Vault Span: Level 5 Baseline"
            }
        NeuroDrillType.OCULOMOTOR_SIGH ->
            "60s Lemniscate + Double-Inhale Sigh"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPrescribed) ObsidianSurfaceElevated else ObsidianSurface,
        border = BorderStroke(
            width = if (isPrescribed) 1.5.dp else 1.dp,
            color = if (isPrescribed) CyberCyan else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunch() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = drillEmoji,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        if (isPrescribed) {
                            Text(
                                text = "⭐ PRESCRIBED FOR YOUR SYMPTOM",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                color = CyberEmerald
                            )
                        }
                        Text(
                            text = drill.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${drill.tag} • ${drill.shortTitle}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = accentColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberEmerald.copy(alpha = 0.18f))
                        .border(1.dp, CyberEmerald, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+25 XP",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = CyberEmerald
                    )
                }
            }

            Text(
                text = drill.mechanismSummary,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = personalMetricText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberAmber
                )

                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPrescribed) CyberCyan else ObsidianBackground,
                        contentColor = if (isPrescribed) Color(0xFF001F24) else CyberCyan
                    ),
                    border = if (!isPrescribed) BorderStroke(1.dp, CyberCyan) else null,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Launch Drill",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BrainMuscleTelemetryCard(
    telemetry: NeuroTelemetrySummary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF141C2E), ObsidianSurface)
                )
            )
            .border(1.5.dp, CyberMagenta.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📊 BRAIN MUSCLE TELEMETRY VAULT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color(0xFFF3C6FF)
                )
                Text(
                    text = "Persistent cognitive metrics recorded across your Neuro-Gym drills",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "${telemetry.totalSessionsCompleted} Sessions",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = CyberCyan
            )
        }

        // 1. Focus Stability Score
        TelemetryRowItem(
            title = "🎯 Focus Stability Score",
            subtitle = "Total duration locked in Foveal Singularity Lock",
            valueText = "${telemetry.totalFocusLockMinutes}m (${telemetry.totalFocusLockSeconds}s)",
            accentColor = CyberCyan
        )

        // 2. Peak Brain Speed
        val schulteText = telemetry.fastestSchulteSeconds?.let {
            val rank = when {
                it < 20.0 -> "Grandmaster"
                it <= 30.0 -> "High Velocity"
                else -> "Warmup"
            }
            "${String.format("%.2f", it)}s ($rank)"
        } ?: "--"
        val qpmText = if (telemetry.highestQuantQpm > 0) {
            "${telemetry.highestQuantQpm} QPM"
        } else {
            "--"
        }

        TelemetryRowItem(
            title = "⚡ Peak Brain Speed",
            subtitle = "Fastest 5x5 Schulte Grid & Peak Quant-Reflex QPM",
            valueText = "$schulteText • $qpmText",
            accentColor = CyberAmber
        )

        // 3. Working Memory Capacity
        val nBackText = if (telemetry.highestNBackAccuracyPercent > 0) {
            "${telemetry.highestNBackAccuracyPercent}% N-Back"
        } else {
            "-- N-Back"
        }
        val chimpText = if (telemetry.maxChimpMemorySpan > 0) {
            "Lvl ${telemetry.maxChimpMemorySpan} Chimp"
        } else {
            "-- Chimp"
        }

        TelemetryRowItem(
            title = "💪 Working Memory Capacity",
            subtitle = "Highest Spatial N-Back accuracy & Max Chimp Flash span",
            valueText = "$nBackText • $chimpText",
            accentColor = CyberEmerald
        )
    }
}

@Composable
private fun TelemetryRowItem(
    title: String,
    subtitle: String,
    valueText: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBackground)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = valueText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = accentColor
        )
    }
}

// ============================================================================================
// DRILL 1: FOVEAL SINGULARITY LOCK (Instant Focus Restoration / "Look at One Point")
// ============================================================================================
@Composable
private fun FovealSingularityLockDrill(
    onBack: () -> Unit,
    onComplete: (durationSeconds: Int) -> Unit
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    DisposableEffect(view) {
        val originalKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = originalKeepScreenOn
        }
    }

    val durationOptions = remember { listOf(60, 90, 120, 180) }
    var selectedDurationSec by remember { mutableIntStateOf(60) }
    var remainingMillis by remember { mutableLongStateOf(60_000L) }
    var isThumbHolding by remember { mutableStateOf(false) }
    var didBreakGazeWarning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // Peripheral Impulse-Control Spark state
    var sparkOffsetNormalized by remember { mutableStateOf<Offset?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "fovealRings")
    val ringProgress by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringContract"
    )

    // Timer loop that ONLY advances while thumb is physically holding the anchor
    LaunchedEffect(isThumbHolding, isCompleted, selectedDurationSec) {
        if (!isThumbHolding || isCompleted) return@LaunchedEffect
        var lastTickSecond = (remainingMillis / 1000L).toInt()
        var nextSparkCountdownMs = Random.nextLong(6_000L, 10_000L)

        while (isThumbHolding && remainingMillis > 0L) {
            delay(100L)
            remainingMillis = (remainingMillis - 100L).coerceAtLeast(0L)

            // Peripheral distraction spark every 6-10s for 900ms
            nextSparkCountdownMs -= 100L
            if (nextSparkCountdownMs <= 0L) {
                val angle = Random.nextDouble(0.0, 2 * PI)
                val radius = Random.nextDouble(0.72, 0.92).toFloat()
                sparkOffsetNormalized = Offset(
                    x = (0.5f + radius * 0.45f * cos(angle).toFloat()).coerceIn(0.06f, 0.94f),
                    y = (0.5f + radius * 0.45f * sin(angle).toFloat()).coerceIn(0.08f, 0.88f)
                )
            }
            if (nextSparkCountdownMs <= -900L) {
                sparkOffsetNormalized = null
                nextSparkCountdownMs = Random.nextLong(6_000L, 10_000L)
            }

            val currentSec = (remainingMillis / 1000L).toInt()
            val elapsedSec = selectedDurationSec - currentSec
            if (currentSec != lastTickSecond && elapsedSec > 0 && elapsedSec % 15 == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            lastTickSecond = currentSec

            if (remainingMillis <= 0L && !isCompleted) {
                isCompleted = true
                sparkOffsetNormalized = null
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete(selectedDurationSec)
                break
            }
        }
    }

    val elapsedFraction = remember(remainingMillis, selectedDurationSec) {
        1f - (remainingMillis.toFloat() / (selectedDurationSec * 1000f)).coerceIn(0f, 1f)
    }

    // Shift aura from Cyan (#00E5FF) -> Pure White-Violet as neural lock deepens
    val auraColor = remember(elapsedFraction) {
        Color(
            red = (0f + 0.86f * elapsedFraction).coerceIn(0f, 1f),
            green = (0.898f - 0.15f * elapsedFraction).coerceIn(0f, 1f),
            blue = 1.0f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar + Duration Chips
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "🎯 FOVEAL SINGULARITY LOCK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "Keep eyes locked on the center dot. Do NOT look at peripheral sparks.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                val secsLeft = (remainingMillis / 1000L).toInt()
                Text(
                    text = String.format("%02d:%02d", secsLeft / 60, secsLeft % 60),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = if (isThumbHolding) auraColor else CyberAmber
                )
            }

            // Duration Selector Chips (60s, 90s, 120s, 180s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationOptions.forEach { sec ->
                    val isSelected = selectedDurationSec == sec
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else ObsidianSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyberCyan else ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isThumbHolding) {
                                selectedDurationSec = sec
                                remainingMillis = sec * 1000L
                                didBreakGazeWarning = false
                                isCompleted = false
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec}s",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (isSelected) CyberCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            if (didBreakGazeWarning && !isThumbHolding && !isCompleted) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CyberAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CyberAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Gaze Broken! Re-anchor your thumb below to resume focus lock.",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CyberAmber,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Center Singularity Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension * 0.34f

                // Outer subtle aura glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = if (isThumbHolding) 0.24f else 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * 0.7f
                    ),
                    radius = maxRadius * 0.7f,
                    center = center
                )

                // 2 ultra-thin (1.dp) concentric harmonic rings contracting toward the center dot
                val ring1Phase = ringProgress
                val ring2Phase = (ringProgress + 0.5f) % 1.0f

                drawCircle(
                    color = auraColor.copy(alpha = (1f - ring1Phase) * 0.65f),
                    radius = (maxRadius * ring1Phase).coerceAtLeast(3.dp.toPx()),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = auraColor.copy(alpha = (1f - ring2Phase) * 0.65f),
                    radius = (maxRadius * ring2Phase).coerceAtLeast(3.dp.toPx()),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Center Singularity Point: Razor-sharp 5.dp glowing cyan/white-violet dot
                drawCircle(
                    color = auraColor,
                    radius = 5.dp.toPx(),
                    center = center
                )

                // Peripheral Impulse-Control Spark (alpha = 0.25f)
                sparkOffsetNormalized?.let { norm ->
                    val sparkCenter = Offset(
                        x = norm.x * size.width,
                        y = norm.y * size.height
                    )
                    drawCircle(
                        color = CyberCrimson.copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = sparkCenter
                    )
                    drawCircle(
                        color = CyberMagenta.copy(alpha = 0.25f),
                        radius = 5.dp.toPx(),
                        center = sparkCenter
                    )
                }
            }
        }

        // Bottom Tactile Thumb-Hold Anchor (Anti-Phone-Check Mechanism)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCompleted) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CyberEmerald.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, CyberEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅ NEURAL SINGULARITY LOCK COMPLETE (+25 XP)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = CyberEmerald
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    remainingMillis = selectedDurationSec * 1000L
                                    isCompleted = false
                                    didBreakGazeWarning = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = Color(0xFF001F24)
                                )
                            ) {
                                Text("Run Again", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ObsidianSurfaceElevated,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Text("Back to Neuro-Gym", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isThumbHolding) {
                                auraColor.copy(alpha = 0.20f)
                            } else {
                                ObsidianSurfaceElevated
                            }
                        )
                        .border(
                            width = if (isThumbHolding) 2.dp else 1.5.dp,
                            color = if (isThumbHolding) auraColor else CyberCyan.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .pointerInput(isCompleted) {
                            if (isCompleted) return@pointerInput
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (anyPressed && !isThumbHolding) {
                                        isThumbHolding = true
                                        didBreakGazeWarning = false
                                    } else if (!anyPressed && isThumbHolding) {
                                        isThumbHolding = false
                                        if (remainingMillis > 0L) {
                                            didBreakGazeWarning = true
                                        }
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Thumb Hold Anchor",
                            tint = if (isThumbHolding) auraColor else CyberCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isThumbHolding) {
                                    "⚡ BIOMETRIC FOCUS LOCK ACTIVE — KEEP EYES ON CENTER DOT"
                                } else {
                                    "HOLD THUMB HERE & LOCK EYES ON THE CENTER DOT"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (isThumbHolding) auraColor else TextPrimary
                            )
                            Text(
                                text = "Releasing your thumb pauses the countdown immediately",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// DRILL 2: 5x5 SCHULTE GRID SPEED TRAINER (Peripheral Vision & Reading Speed)
// ============================================================================================
@Composable
private fun SchulteGridSpeedTrainerDrill(
    personalBestSeconds: Double?,
    onBack: () -> Unit,
    onComplete: (totalTimeSeconds: Double) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var gridNumbers by remember { mutableStateOf((1..25).shuffled()) }
    var targetNumber by remember { mutableIntStateOf(1) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var penaltyMillis by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }
    var hardModeShuffleEveryTap by remember { mutableStateOf(false) }

    var flashedGreenCellNumber by remember { mutableIntStateOf(-1) }
    var flashedRedCellNumber by remember { mutableIntStateOf(-1) }

    LaunchedEffect(isRunning, isCompleted) {
        if (!isRunning || isCompleted) return@LaunchedEffect
        val startNs = System.nanoTime()
        val baseElapsed = elapsedMillis
        while (isRunning && !isCompleted) {
            delay(30L)
            val deltaMs = (System.nanoTime() - startNs) / 1_000_000L
            elapsedMillis = baseElapsed + deltaMs
        }
    }

    LaunchedEffect(flashedGreenCellNumber) {
        if (flashedGreenCellNumber != -1) {
            delay(150L)
            flashedGreenCellNumber = -1
        }
    }

    LaunchedEffect(flashedRedCellNumber) {
        if (flashedRedCellNumber != -1) {
            delay(250L)
            flashedRedCellNumber = -1
        }
    }

    val totalSeconds = (elapsedMillis + penaltyMillis) / 1000.0
    val velocityPerTarget = totalSeconds / 25.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header & HUD
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "👁️ 5x5 SCHULTE GRID TRAINER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "Keep eyes on the center red dot (•) and tap 1 -> 25 in ascending order.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        gridNumbers = (1..25).shuffled()
                        targetNumber = 1
                        elapsedMillis = 0L
                        penaltyMillis = 0L
                        isCompleted = false
                        isRunning = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Grid",
                        tint = CyberCyan
                    )
                }
            }

            // HUD Bar: Find Next [ X ] + Live Stopwatch + Hard Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "FIND NEXT:",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.22f))
                            .border(1.5.dp, CyberCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "DONE" else targetNumber.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = CyberCyan
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%05.2fs", totalSeconds),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = if (penaltyMillis > 0L) CyberAmber else CyberEmerald
                    )
                    if (penaltyMillis > 0L) {
                        Text(
                            text = "+${penaltyMillis / 1000}s penalty",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberCrimson
                        )
                    }
                }
            }

            // Hard Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianSurface)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 Hard Mode: Shuffle grid on every tap!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hardModeShuffleEveryTap) CyberAmber else TextSecondary
                )
                Switch(
                    checked = hardModeShuffleEveryTap,
                    onCheckedChange = { hardModeShuffleEveryTap = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberAmber,
                        checkedTrackColor = CyberAmber.copy(alpha = 0.25f)
                    )
                )
            }
        }

        // 5x5 Matrix (All 25 numbers remain visible at all times)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0 until 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 5) {
                            val index = row * 5 + col
                            val number = gridNumbers[index]
                            val isCenterFixationCell = (row == 2 && col == 2)
                            val isGreenFlash = (flashedGreenCellNumber == number)
                            val isRedFlash = (flashedRedCellNumber == number)

                            val bgColor = when {
                                isGreenFlash -> CyberEmerald.copy(alpha = 0.45f)
                                isRedFlash -> CyberCrimson.copy(alpha = 0.45f)
                                isCenterFixationCell -> Color(0xFF1A2234)
                                else -> ObsidianSurfaceElevated
                            }
                            val borderColor = when {
                                isGreenFlash -> CyberEmerald
                                isRedFlash -> CyberCrimson
                                isCenterFixationCell -> CyberCrimson.copy(alpha = 0.7f)
                                else -> ObsidianBorder
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgColor)
                                    .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isCompleted) {
                                        if (number == targetNumber) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            flashedGreenCellNumber = number
                                            if (targetNumber == 25) {
                                                isCompleted = true
                                                isRunning = false
                                                onComplete(totalSeconds)
                                            } else {
                                                targetNumber++
                                                if (hardModeShuffleEveryTap) {
                                                    gridNumbers = (1..25).shuffled()
                                                }
                                            }
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            flashedRedCellNumber = number
                                            penaltyMillis += 1_000L
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = number.toString(),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = TextPrimary
                                    )
                                    if (isCenterFixationCell) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(CyberCrimson)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Footer Metrics & Rank Comparison
        if (isCompleted) {
            val rankLabel = when {
                totalSeconds < 20.0 -> "🏆 GRANDMASTER (<20s)"
                totalSeconds <= 30.0 -> "⚡ HIGH VELOCITY (20–30s)"
                else -> "🔋 WARMUP (>30s)"
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CyberEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.5.dp, CyberEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$rankLabel • ${String.format("%.2f", velocityPerTarget)}s / target",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = CyberEmerald
                    )
                    personalBestSeconds?.let { best ->
                        Text(
                            text = "All-Time Personal Best: ${String.format("%.2f", best)}s",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                gridNumbers = (1..25).shuffled()
                                targetNumber = 1
                                elapsedMillis = 0L
                                penaltyMillis = 0L
                                isCompleted = false
                                isRunning = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001F24)
                            )
                        ) {
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceElevated,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text("Back to Neuro-Gym", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Anti-Cheat Active: All 25 numbers stay visible so visual search difficulty never drops.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================================
// DRILL 3: QUANT-REFLEX VELOCITY BLITZ (Mental Math & Prefrontal Ignition)
// ============================================================================================
private data class QuantQuestion(
    val tier: Int,
    val tierLabel: String,
    val promptText: String,
    val options: List<String>,
    val correctIndex: Int
)

private fun generateQuantQuestion(streak: Int): QuantQuestion {
    val tier = when {
        streak < 4 -> 1
        streak < 8 -> 2
        else -> Random.nextInt(1, 4)
    }

    return when (tier) {
        1 -> {
            // Tier 1 (Warmup): Rapid 2-digit arithmetic & multiplication
            val opType = Random.nextInt(3)
            val (prompt, answer) = when (opType) {
                0 -> {
                    val a = Random.nextInt(24, 89)
                    val b = Random.nextInt(15, 76)
                    "$a + $b" to (a + b)
                }
                1 -> {
                    val a = Random.nextInt(65, 160)
                    val b = Random.nextInt(18, 59)
                    "$a − $b" to (a - b)
                }
                else -> {
                    val a = Random.nextInt(12, 24)
                    val b = Random.nextInt(4, 12)
                    "$a × $b" to (a * b)
                }
            }
            val distractors = mutableSetOf<Int>()
            val offsets = listOf(-10, -2, -1, 1, 2, 10, -4, 4)
            for (offset in offsets.shuffled()) {
                val candidate = answer + offset
                if (candidate > 0 && candidate != answer) {
                    distractors.add(candidate)
                }
                if (distractors.size == 3) break
            }
            val allChoices = (distractors.toList() + answer).shuffled()
            QuantQuestion(
                tier = 1,
                tierLabel = "TIER 1 • ARITHMETIC IGNITION",
                promptText = "$prompt = ?",
                options = allChoices.map { it.toString() },
                correctIndex = allChoices.indexOf(answer)
            )
        }

        2 -> {
            // Tier 2 (Velocity Inequality): Two expressions side-by-side
            val a1 = Random.nextInt(11, 19)
            val b1 = Random.nextInt(5, 9)
            val leftVal = a1 * b1

            val delta = listOf(-6, -3, 0, 0, 3, 6).random()
            val rightVal = leftVal + delta
            val rAdd = Random.nextInt(12, 35)
            val rBase = rightVal + rAdd

            val options = listOf("[ LEFT > ]", "[ EQUAL = ]", "[ < RIGHT ]")
            val correctIdx = when {
                leftVal > rightVal -> 0
                leftVal == rightVal -> 1
                else -> 2
            }
            QuantQuestion(
                tier = 2,
                tierLabel = "TIER 2 • VELOCITY INEQUALITY",
                promptText = "($a1 × $b1)   vs   ($rBase − $rAdd)",
                options = options,
                correctIndex = correctIdx
            )
        }

        else -> {
            // Tier 3 (HSC Science Reflex): Powers, squares, roots, percentages
            val bank = listOf(
                "13²" to 169,
                "14²" to 196,
                "15²" to 225,
                "16²" to 256,
                "17²" to 289,
                "18²" to 324,
                "19²" to 361,
                "21²" to 441,
                "√196" to 14,
                "√225" to 15,
                "√256" to 16,
                "√289" to 17,
                "√324" to 18,
                "2⁷" to 128,
                "2⁸" to 256,
                "2⁹" to 512,
                "3⁵" to 243,
                "15% of 240" to 36,
                "25% of 360" to 90,
                "20% of 450" to 90
            )
            val (prompt, answer) = bank.random()
            val distractors = mutableSetOf<Int>()
            val smartOffsets = listOf(-10, 10, -2, 2, -4, 4, -12, 12)
            for (off in smartOffsets.shuffled()) {
                val cand = answer + off
                if (cand > 0 && cand != answer) {
                    distractors.add(cand)
                }
                if (distractors.size == 3) break
            }
            val allChoices = (distractors.toList() + answer).shuffled()
            QuantQuestion(
                tier = 3,
                tierLabel = "TIER 3 • HSC SCIENCE REFLEX",
                promptText = "$prompt = ?",
                options = allChoices.map { it.toString() },
                correctIndex = allChoices.indexOf(answer)
            )
        }
    }
}

@Composable
private fun QuantReflexVelocityBlitzDrill(
    personalBestQpm: Double,
    onBack: () -> Unit,
    onComplete: (qpmScore: Double) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var secondsRemaining by remember { mutableIntStateOf(60) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalAttempted by remember { mutableIntStateOf(0) }
    var currentStreak by remember { mutableIntStateOf(0) }
    var totalScorePoints by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }

    var currentQuestion by remember { mutableStateOf(generateQuantQuestion(0)) }

    val comboMultiplier = when {
        currentStreak >= 9 -> 8
        currentStreak >= 6 -> 4
        currentStreak >= 3 -> 2
        else -> 1
    }

    val elapsedSeconds = (60 - secondsRemaining).coerceAtLeast(1)
    val liveQpm = (correctCount.toDouble() / elapsedSeconds.toDouble()) * 60.0

    LaunchedEffect(isCompleted) {
        if (isCompleted) return@LaunchedEffect
        while (secondsRemaining > 0) {
            delay(1_000L)
            secondsRemaining--
            if (secondsRemaining <= 0) {
                isCompleted = true
                onComplete(correctCount.toDouble())
                break
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header + Speedometer HUD
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "⚡ QUANT-REFLEX VELOCITY BLITZ",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "60-second prefrontal math sprint with auto-scaling tiers",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "${secondsRemaining}s",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = if (secondsRemaining <= 10) CyberCrimson else CyberCyan
                )
            }

            // Combo Multiplier & QPM Speedometer Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, CyberAmber),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "COMBO MULTIPLIER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "x$comboMultiplier (${currentStreak}🔥)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = CyberAmber
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, CyberEmerald),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VELOCITY (QPM)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${liveQpm.roundToInt()} QPM",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = CyberEmerald
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, CyberMagenta),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SCORE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = totalScorePoints.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFFF3C6FF)
                        )
                    }
                }
            }
        }

        // Question Display Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ObsidianSurfaceElevated, ObsidianSurface)
                    )
                )
                .border(1.5.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentQuestion.tierLabel,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = CyberCyan
                    )
                }

                Text(
                    text = currentQuestion.promptText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Solved: $correctCount / $totalAttempted",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Answer Pads or Completion Summary
        if (isCompleted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CyberEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.5.dp, CyberEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚡ BLITZ COMPLETE: $correctCount QPM • $totalScorePoints PTS (+25 XP)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = CyberEmerald
                    )
                    if (personalBestQpm > 0.0) {
                        Text(
                            text = "All-Time Peak Velocity: ${personalBestQpm.roundToInt()} QPM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                secondsRemaining = 60
                                correctCount = 0
                                totalAttempted = 0
                                currentStreak = 0
                                totalScorePoints = 0
                                isCompleted = false
                                currentQuestion = generateQuantQuestion(0)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001F24)
                            )
                        ) {
                            Text("Blitz Again", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceElevated,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text("Back to Neuro-Gym", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val options = currentQuestion.options
                if (options.size == 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        options.forEachIndexed { idx, label ->
                            QuantOptionButton(
                                label = label,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    totalAttempted++
                                    if (idx == currentQuestion.correctIndex) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        correctCount++
                                        currentStreak++
                                        totalScorePoints += (10 * comboMultiplier)
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentStreak = 0
                                    }
                                    currentQuestion = generateQuantQuestion(currentStreak)
                                }
                            )
                        }
                    }
                } else {
                    for (row in 0 until 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (col in 0 until 2) {
                                val idx = row * 2 + col
                                if (idx < options.size) {
                                    QuantOptionButton(
                                        label = options[idx],
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            totalAttempted++
                                            if (idx == currentQuestion.correctIndex) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                correctCount++
                                                currentStreak++
                                                totalScorePoints += (10 * comboMultiplier)
                                            } else {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                currentStreak = 0
                                            }
                                            currentQuestion = generateQuantQuestion(currentStreak)
                                        }
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

@Composable
private fun QuantOptionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.65f)),
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = TextPrimary
            )
        }
    }
}

// ============================================================================================
// DRILL 4: SPATIAL N-BACK MATRIX (Working Memory & "Brain Muscle" Hypertrophy)
// ============================================================================================
private fun generateWeightedNBackSequence(totalSteps: Int, nBack: Int): List<Int> {
    val seq = MutableList(totalSteps) { Random.nextInt(0, 9) }
    for (i in nBack until totalSteps) {
        if (Random.nextFloat() < 0.35f) {
            seq[i] = seq[i - nBack]
        } else {
            var candidate = Random.nextInt(0, 9)
            while (candidate == seq[i - nBack]) {
                candidate = Random.nextInt(0, 9)
            }
            seq[i] = candidate
        }
    }
    return seq
}

@Composable
private fun SpatialNBackMatrixDrill(
    onBack: () -> Unit,
    onComplete: (accuracyPercent: Double, nLevel: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val totalSteps = 25

    var nLevel by remember { mutableIntStateOf(2) }
    var sequence by remember { mutableStateOf(generateWeightedNBackSequence(totalSteps, 2)) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isCellLit by remember { mutableStateOf(true) }
    var hasRespondedThisStep by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }

    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var falseAlarms by remember { mutableIntStateOf(0) }
    var correctPasses by remember { mutableIntStateOf(0) }
    var sessionToken by remember { mutableIntStateOf(0) }

    // 2,200ms per step, 900ms active cyan flash
    LaunchedEffect(isRunning, isCompleted, sessionToken) {
        if (!isRunning || isCompleted) return@LaunchedEffect
        while (currentStepIndex < totalSteps && !isCompleted) {
            hasRespondedThisStep = false
            isCellLit = true
            delay(900L)
            isCellLit = false
            delay(1_300L)

            // Evaluate unresponded step if >= nLevel
            if (currentStepIndex >= nLevel && !hasRespondedThisStep) {
                val isActualMatch = sequence[currentStepIndex] == sequence[currentStepIndex - nLevel]
                if (isActualMatch) {
                    misses++
                } else {
                    correctPasses++
                }
            }

            if (currentStepIndex + 1 >= totalSteps) {
                isCompleted = true
                isRunning = false
                val totalScorable = (hits + misses + falseAlarms + correctPasses).coerceAtLeast(1)
                val accuracy = ((hits + correctPasses).toDouble() / totalScorable.toDouble()) * 100.0
                onComplete(accuracy, nLevel)
                break
            } else {
                currentStepIndex++
            }
        }
    }

    val totalScorableSoFar = (hits + misses + falseAlarms + correctPasses).coerceAtLeast(1)
    val liveAccuracy = ((hits + correctPasses).toDouble() / totalScorableSoFar.toDouble()) * 100.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header + N-Level Selector
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "💪 SPATIAL N-BACK MATRIX",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "Does the flashing cell match the exact position from $nLevel steps ago?",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "Step ${currentStepIndex + 1}/$totalSteps",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = CyberCyan
                )
            }

            // Level Selector: 1-Back, 2-Back, 3-Back
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    1 to "1-Back (Warmup)",
                    2 to "2-Back (Standard HSC)",
                    3 to "3-Back (Overclock)"
                ).forEach { (level, label) ->
                    val isSelected = nLevel == level
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else ObsidianSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyberCyan else ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                nLevel = level
                                sequence = generateWeightedNBackSequence(totalSteps, level)
                                currentStepIndex = 0
                                hits = 0
                                misses = 0
                                falseAlarms = 0
                                correctPasses = 0
                                isCompleted = false
                                isRunning = true
                                sessionToken++
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isSelected) CyberCyan else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Real-time Score Tracker: Hits, Misses, False Alarms, Accuracy %
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NBackMetricPill("HITS", hits.toString(), CyberEmerald)
                NBackMetricPill("MISSES", misses.toString(), CyberAmber)
                NBackMetricPill("FALSE ALARMS", falseAlarms.toString(), CyberCrimson)
                NBackMetricPill("ACCURACY", "${liveAccuracy.roundToInt()}%", CyberCyan)
            }
        }

        // 3x3 Neon Cyber-Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val activeCell = sequence.getOrElse(currentStepIndex) { 0 }
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .aspectRatio(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 3) {
                            val cellIndex = row * 3 + col
                            val isFlashing = isCellLit && (cellIndex == activeCell) && !isCompleted

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isFlashing) CyberCyan else ObsidianSurfaceElevated
                                    )
                                    .border(
                                        width = if (isFlashing) 2.5.dp else 1.5.dp,
                                        color = if (isFlashing) Color.White else ObsidianBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls: [ ⚡ MATCH (N-Steps Ago) ] and [ ⏭️ NO MATCH / PASS ]
        if (isCompleted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CyberEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.5.dp, CyberEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✅ ${nLevel}-BACK COMPLETE • ${liveAccuracy.roundToInt()}% ACCURACY (+25 XP)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = CyberEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                sequence = generateWeightedNBackSequence(totalSteps, nLevel)
                                currentStepIndex = 0
                                hits = 0
                                misses = 0
                                falseAlarms = 0
                                correctPasses = 0
                                isCompleted = false
                                isRunning = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001F24)
                            )
                        ) {
                            Text("Run Again", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceElevated,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text("Back to Neuro-Gym", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val canJudge = currentStepIndex >= nLevel && !hasRespondedThisStep
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (!canJudge) return@Button
                        hasRespondedThisStep = true
                        val isActualMatch =
                            sequence[currentStepIndex] == sequence[currentStepIndex - nLevel]
                        if (isActualMatch) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            hits++
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            falseAlarms++
                        }
                    },
                    enabled = canJudge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Color(0xFF001F24)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                ) {
                    Text(
                        text = "⚡ MATCH (${nLevel}-Ago)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        if (!canJudge) return@Button
                        hasRespondedThisStep = true
                        val isActualMatch =
                            sequence[currentStepIndex] == sequence[currentStepIndex - nLevel]
                        if (!isActualMatch) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            correctPasses++
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            misses++
                        }
                    },
                    enabled = canJudge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                ) {
                    Text(
                        text = "⏭️ NO MATCH / PASS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NBackMetricPill(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = color
        )
    }
}

// ============================================================================================
// DRILL 5: CHIMP FLASH MEMORY VAULT (Instantaneous Photographic Working Memory)
// ============================================================================================
private fun generateChimpBoard(levelCount: Int): Map<Int, Int> {
    val chosenCells = (0 until 25).shuffled().take(levelCount)
    val map = mutableMapOf<Int, Int>()
    chosenCells.forEachIndexed { idx, cellIndex ->
        map[cellIndex] = idx + 1
    }
    return map
}

@Composable
private fun ChimpFlashMemoryVaultDrill(
    personalBestLevel: Int,
    onBack: () -> Unit,
    onComplete: (maxLevelReached: Int, elapsedSeconds: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var currentLevel by remember { mutableIntStateOf(5) }
    var peakLevelReached by remember { mutableIntStateOf(0) }
    var livesRemaining by remember { mutableIntStateOf(3) }
    var cellToNumberMap by remember { mutableStateOf(generateChimpBoard(5)) }
    var nextExpectedNumber by remember { mutableIntStateOf(1) }
    var isMaskedPhase by remember { mutableStateOf(false) }
    var clearedCells by remember { mutableStateOf(setOf<Int>()) }
    var isGameOver by remember { mutableStateOf(false) }
    val startTimestampMs = remember { System.currentTimeMillis() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header + Status Bar
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "🦍 CHIMP FLASH MEMORY VAULT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "Study 1..$currentLevel. The instant you tap 1, tiles 2..$currentLevel mask!",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "❤️".repeat(livesRemaining.coerceAtLeast(0)),
                    fontSize = 16.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEVEL $currentLevel ($currentLevel Tiles)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = CyberCyan
                )
                Text(
                    text = if (!isMaskedPhase) {
                        "PHASE 1: ENCODE & TAP [ 1 ]"
                    } else {
                        "PHASE 2: RECALL [ $nextExpectedNumber ]"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (!isMaskedPhase) CyberEmerald else CyberAmber
                )
            }
        }

        // 5x5 Chimp Flash Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0 until 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 5) {
                            val cellIdx = row * 5 + col
                            val assignedNumber = cellToNumberMap[cellIdx]
                            val isCleared = cellIdx in clearedCells
                            val hasActiveTile = assignedNumber != null && !isCleared

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            !hasActiveTile -> ObsidianSurface
                                            isMaskedPhase -> CyberCyan
                                            else -> ObsidianSurfaceElevated
                                        }
                                    )
                                    .border(
                                        width = if (hasActiveTile) 2.dp else 1.dp,
                                        color = if (hasActiveTile) CyberCyan else ObsidianBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = hasActiveTile && !isGameOver) {
                                        val tappedNum = assignedNumber ?: return@clickable
                                        if (tappedNum == nextExpectedNumber) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            clearedCells = clearedCells + cellIdx
                                            if (tappedNum == 1 && currentLevel > 1) {
                                                // Exact millisecond user taps 1, numbers 2..N mask!
                                                isMaskedPhase = true
                                            }
                                            if (tappedNum == currentLevel) {
                                                // Level Cleared!
                                                peakLevelReached = maxOf(peakLevelReached, currentLevel)
                                                if (currentLevel >= 12) {
                                                    isGameOver = true
                                                    val elapsedSec =
                                                        ((System.currentTimeMillis() - startTimestampMs) / 1000L)
                                                            .toInt()
                                                            .coerceAtLeast(1)
                                                    onComplete(peakLevelReached, elapsedSec)
                                                } else {
                                                    val nextLvl = currentLevel + 1
                                                    currentLevel = nextLvl
                                                    cellToNumberMap = generateChimpBoard(nextLvl)
                                                    clearedCells = emptySet()
                                                    nextExpectedNumber = 1
                                                    isMaskedPhase = false
                                                }
                                            } else {
                                                nextExpectedNumber++
                                            }
                                        } else {
                                            // Wrong tile tapped!
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            livesRemaining--
                                            if (livesRemaining <= 0) {
                                                isGameOver = true
                                                val elapsedSec =
                                                    ((System.currentTimeMillis() - startTimestampMs) / 1000L)
                                                        .toInt()
                                                        .coerceAtLeast(1)
                                                onComplete(peakLevelReached, elapsedSec)
                                            } else {
                                                // Reset current level
                                                cellToNumberMap = generateChimpBoard(currentLevel)
                                                clearedCells = emptySet()
                                                nextExpectedNumber = 1
                                                isMaskedPhase = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasActiveTile) {
                                    Text(
                                        text = if (isMaskedPhase) "?" else assignedNumber.toString(),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp,
                                        color = if (isMaskedPhase) Color(0xFF001F24) else CyberCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Footer
        if (isGameOver) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CyberEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.5.dp, CyberEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🦍 CHIMP VAULT COMPLETE • PEAK SPAN: LEVEL $peakLevelReached (+25 XP)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = CyberEmerald
                    )
                    if (personalBestLevel > 0) {
                        Text(
                            text = "All-Time Best Chimp Vault Level: $personalBestLevel",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                currentLevel = 5
                                peakLevelReached = 5
                                livesRemaining = 3
                                cellToNumberMap = generateChimpBoard(5)
                                clearedCells = emptySet()
                                nextExpectedNumber = 1
                                isMaskedPhase = false
                                isGameOver = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001F24)
                            )
                        ) {
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceElevated,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text("Back to Neuro-Gym", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Peak Span This Run: Level $peakLevelReached • All-Time Record: Level ${maxOf(personalBestLevel, peakLevelReached)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================================
// DRILL 6: OCULOMOTOR FIGURE-8 PURSUIT & PHYSIOLOGICAL SIGH (Eye Strain & Stress Reset)
// ============================================================================================
@Composable
private fun OculomotorFigure8PursuitDrill(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var elapsedMillisForCanvas by remember { mutableLongStateOf(0L) }
    var secondsLeft by remember { mutableIntStateOf(60) }
    var currentCycleNumber by remember { mutableIntStateOf(1) }
    var breathPhase by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    var lastTopUpCycleTriggered by remember { mutableIntStateOf(-1) }

    val totalDurationMs = 60_000L

    LaunchedEffect(isCompleted) {
        if (isCompleted) return@LaunchedEffect
        val startNs = System.nanoTime()
        while (elapsedMillisForCanvas < totalDurationMs && !isCompleted) {
            delay(16L) // ~60fps smooth pursuit
            val newElapsed = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtMost(totalDurationMs)
            elapsedMillisForCanvas = newElapsed

            val newSec = ((totalDurationMs - newElapsed) / 1000L).toInt()
            if (newSec != secondsLeft) {
                secondsLeft = newSec
            }

            val cycleMs = newElapsed % 10_000L
            val currentCycleIndex = (newElapsed / 10_000L).toInt()
            val newPhase = when {
                cycleMs < 2500L -> 0
                cycleMs < 3500L -> 1
                else -> 2
            }
            if (newPhase != breathPhase) {
                breathPhase = newPhase
            }

            val newCycleNum = (currentCycleIndex + 1).coerceIn(1, 6)
            if (newCycleNum != currentCycleNumber) {
                currentCycleNumber = newCycleNum
            }

            // Check 10-second Physiological Sigh cycle for the 2.5s double-inhale haptic cue
            if (cycleMs in 2500L..2750L && lastTopUpCycleTriggered != currentCycleIndex) {
                lastTopUpCycleTriggered = currentCycleIndex
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            if (newElapsed >= totalDurationMs && !isCompleted) {
                isCompleted = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete()
                break
            }
        }
    }

    // Discrete phase info for top & bottom HUDs (recomposes only on second/phase transitions)
    val (breathInstruction, breathAccentColor) = when (breathPhase) {
        0 -> "1️⃣ INHALE NOSE (80% LUNG FILL)" to CyberCyan
        1 -> "2️⃣ TOP-UP SECOND INHALE! (100% MAX FILL)" to Color(0xFFF3C6FF)
        else -> "3️⃣ LONG SLOW EXHALE MOUTH (RELEASE TENSION)" to CyberEmerald
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04070E))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "🌊 OCULOMOTOR ∞ PURSUIT & PHYSIOLOGICAL SIGH",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = CyberCyan
                    )
                    Text(
                        text = "Track the ∞ plasma orb smoothly with your eyes while following the breathing ring.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "${secondsLeft}s",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = breathAccentColor
            )
        }

        // 60fps Lemniscate of Bernoulli (∞) + Physiological Sigh Breathing Ring Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentElapsed = elapsedMillisForCanvas
                val cycleSeconds = (currentElapsed % 10_000L) / 1000f
                val (canvasRadiusFraction, canvasAccentColor) = when {
                    cycleSeconds < 2.5f -> {
                        val progress = (cycleSeconds / 2.5f).coerceIn(0f, 1f)
                        (0.35f + 0.45f * progress) to CyberCyan
                    }
                    cycleSeconds < 3.5f -> {
                        val progress = ((cycleSeconds - 2.5f) / 1.0f).coerceIn(0f, 1f)
                        (0.80f + 0.20f * progress) to Color(0xFFF3C6FF)
                    }
                    else -> {
                        val progress = ((cycleSeconds - 3.5f) / 6.5f).coerceIn(0f, 1f)
                        (1.00f - 0.65f * progress) to CyberEmerald
                    }
                }

                val center = Offset(size.width / 2f, size.height / 2f)
                val a = size.width * 0.38f

                // 1. Physiological Sigh Breathing Ring in the center background
                val maxRingRadius = size.minDimension * 0.36f
                val currentRingRadius = maxRingRadius * canvasRadiusFraction
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            canvasAccentColor.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentRingRadius
                    ),
                    radius = currentRingRadius,
                    center = center
                )
                drawCircle(
                    color = canvasAccentColor.copy(alpha = 0.75f),
                    radius = currentRingRadius,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // 2. Full Lemniscate of Bernoulli (∞ Figure-8) path guide
                val lemniscatePath = Path()
                val steps = 160
                for (i in 0..steps) {
                    val theta = (i.toDouble() / steps.toDouble()) * 2.0 * PI
                    val denom = 1.0 + sin(theta) * sin(theta)
                    val x = center.x + (a * cos(theta) / denom).toFloat()
                    val y = center.y + (a * sin(theta) * cos(theta) / denom).toFloat()
                    if (i == 0) {
                        lemniscatePath.moveTo(x, y)
                    } else {
                        lemniscatePath.lineTo(x, y)
                    }
                }
                lemniscatePath.close()

                drawPath(
                    path = lemniscatePath,
                    color = CyberCyan.copy(alpha = 0.25f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3. Moving Plasma Orb along the Lemniscate (1 full figure-8 every 6.5 seconds)
                val t = (currentElapsed.toDouble() / 6_500.0) * 2.0 * PI
                val denom = 1.0 + sin(t) * sin(t)
                val orbX = center.x + (a * cos(t) / denom).toFloat()
                val orbY = center.y + (a * sin(t) * cos(t) / denom).toFloat()
                val orbCenter = Offset(orbX, orbY)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            canvasAccentColor.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        center = orbCenter,
                        radius = 28.dp.toPx()
                    ),
                    radius = 28.dp.toPx(),
                    center = orbCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = orbCenter
                )
            }
        }

        // Bottom Breathing HUD / Completion Card
        if (isCompleted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CyberEmerald.copy(alpha = 0.18f),
                border = BorderStroke(1.5.dp, CyberEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✅ 60s AUTONOMIC & OCULOMOTOR RESET COMPLETE (+25 XP)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = CyberEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                elapsedMillisForCanvas = 0L
                                secondsLeft = 60
                                currentCycleNumber = 1
                                breathPhase = 0
                                isCompleted = false
                                lastTopUpCycleTriggered = -1
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001F24)
                            )
                        ) {
                            Text("Repeat 60s Reset", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceElevated,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text("Back to Neuro-Gym", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.5.dp, breathAccentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CYCLE $currentCycleNumber OF 6 • PHYSIOLOGICAL SIGH",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = breathInstruction,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = breathAccentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
