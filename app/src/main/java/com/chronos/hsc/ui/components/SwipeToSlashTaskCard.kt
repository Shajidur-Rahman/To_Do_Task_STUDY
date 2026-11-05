package com.chronos.hsc.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSyllabusCatalog
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import com.chronos.hsc.ui.theme.brandColor
import com.chronos.hsc.ui.theme.AppThemeMode
import com.chronos.hsc.ui.theme.LocalAppThemeMode
import com.chronos.hsc.ui.theme.NatureAutumnOrange
import com.chronos.hsc.ui.theme.NatureCardShape
import com.chronos.hsc.ui.theme.NatureFireflyGold
import com.chronos.hsc.ui.theme.NatureLagRust
import com.chronos.hsc.ui.theme.NatureLeafGreen
import com.chronos.hsc.ui.theme.NatureSproutLime
import com.chronos.hsc.ui.theme.TerrariumBorder
import com.chronos.hsc.ui.theme.TerrariumSurface
import com.chronos.hsc.ui.theme.TerrariumSurfaceElevated
import com.chronos.hsc.ui.components.NatureLadybugBorder
import com.chronos.hsc.ui.components.NatureVineSwipeOverlay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class SlashParticle(
    val angleRad: Float,
    val speed: Float,
    val radiusPx: Float,
    val color: Color
)

@Composable
fun SwipeToSlashTaskCard(
    item: SliceWithParentChain,
    todayEpochDay: Long,
    onIncrementProblem: () -> Unit,
    onDecrementProblem: () -> Unit = {},
    onCompleteSlice: () -> Unit,
    onPushDominoPlusOneDay: () -> Unit,
    onRequestDelete: () -> Unit,
    onCatchUpToToday: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val slice = item.slice
    val chain = item.chain
    val subjectColor = chain.subject.brandColor()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val isNature = LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER

    var cardWidthPx by remember { mutableFloatStateOf(1000f) }
    var dragOffsetX by remember(slice.sliceId) { mutableFloatStateOf(0f) }
    val burstProgress = remember(slice.sliceId) { Animatable(0f) }
    var isBursting by remember(slice.sliceId) { mutableStateOf(false) }

    val particles = remember(slice.sliceId) {
        val palette = listOf(subjectColor, CyberEmerald, CyberCyan, CyberAmber, Color.White)
        List(28) { idx ->
            SlashParticle(
                angleRad = (idx * (360f / 28f) + Random.nextFloat() * 12f) * (Math.PI.toFloat() / 180f),
                speed = 90f + Random.nextFloat() * 190f,
                radiusPx = 4f + Random.nextFloat() * 7f,
                color = palette[idx % palette.size]
            )
        }
    }

    val isReadyToSlash = !slice.isCompleted &&
        slice.problemsTarget > 0 &&
        slice.problemsCompleted >= slice.problemsTarget

    val isOverdue = !slice.isCompleted && slice.scheduledDateEpochDay < todayEpochDay

    // Animated left-to-right background fill gradient from Sub-Counter progress
    val animatedFillFraction by animateFloatAsState(
        targetValue = slice.problemProgressFraction,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "problemFillGradient"
    )

    // Neon green border pulse when problemsCompleted == problemsTarget
    val infiniteTransition = rememberInfiniteTransition(label = "readyBorderPulse")
    val readyPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "readyPulseAlpha"
    )

    // Cap threshold at 240.dp equivalent or 30% of card width so tablets don't require huge swipes
    val swipeThresholdPx = (cardWidthPx * 0.28f).coerceIn(160f, 320f)
    val effectiveDragX = dragOffsetX
    val slashRatio = (effectiveDragX / swipeThresholdPx).coerceIn(0f, 1.15f)

    fun triggerSlashCompletion() {
        if (slice.isCompleted) return
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            isBursting = true
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutLinearInEasing)
            )
            isBursting = false
        }
        onCompleteSlice()
    }

    val defaultBorder = if (isNature) TerrariumBorder else ObsidianBorder
    val borderColor = when {
        slice.isCompleted -> (if (isNature) NatureLeafGreen else CyberEmerald).copy(alpha = 0.45f)
        slashRatio >= 0.85f -> if (isNature) NatureLeafGreen else CyberEmerald
        isReadyToSlash -> (if (isNature) NatureLeafGreen else CyberEmerald).copy(alpha = readyPulseAlpha)
        slice.isLagging -> (if (isNature) NatureLagRust else CyberCrimson).copy(alpha = 0.9f)
        isOverdue -> (if (isNature) NatureAutumnOrange else CyberCrimson).copy(alpha = 0.85f)
        slice.xpMultiplier > 1 -> (if (isNature) NatureFireflyGold else CyberAmber).copy(alpha = 0.85f)
        else -> defaultBorder
    }

    val borderWidth = if (isReadyToSlash || slashRatio > 0.2f || isOverdue || slice.isLagging) 1.6.dp else 1.dp
    val cardShape = if (isNature) NatureCardShape else RoundedCornerShape(16.dp)
    val cardSurfaceBg = if (isNature) TerrariumSurface else ObsidianSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (size.width > 0) {
                    cardWidthPx = size.width.toFloat()
                }
            }
    ) {
        // Underlying Swipe-to-Slash track indicator
        if (effectiveDragX > 6f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(cardShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                (if (isNature) NatureLeafGreen else CyberEmerald).copy(alpha = 0.32f),
                                subjectColor.copy(alpha = 0.18f),
                                cardSurfaceBg
                            )
                        )
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (slashRatio >= 1f) {
                            if (isNature) "🌿 RELEASE TO BLOOM & CONQUER!" else "⚔️ RELEASE TO SLASH BOSS SEGMENT!"
                        } else {
                            if (isNature) "🌱 SWIPE RIGHT TO HARVEST >>" else "🗡️ SWIPE RIGHT TO SLASH >>"
                        },
                        color = if (slashRatio >= 1f) (if (isNature) NatureLeafGreen else CyberEmerald) else (if (isNature) NatureSproutLime else CyberCyan),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Foreground interactive card
        Box(
            modifier = Modifier
                .offset { IntOffset((effectiveDragX * 0.42f).roundToInt(), 0) }
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardSurfaceBg)
                .drawBehind {
                    // Sub-Counter left-to-right animated fill gradient
                    if (animatedFillFraction > 0.01f) {
                        val fillWidth = size.width * animatedFillFraction
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    subjectColor.copy(alpha = if (slice.isCompleted) 0.20f else 0.24f),
                                    (if (isNature) NatureLeafGreen else CyberEmerald).copy(alpha = if (isReadyToSlash) 0.28f else 0.12f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = fillWidth * 1.15f
                            ),
                            topLeft = Offset.Zero,
                            size = Size(fillWidth, size.height)
                        )
                    }
                    // Left neon / sprout subject accent bar
                    drawRect(
                        color = if (slice.isCompleted) (if (isNature) NatureLeafGreen else CyberEmerald) else subjectColor,
                        topLeft = Offset.Zero,
                        size = Size(5.dp.toPx(), size.height)
                    )
                }
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = cardShape
                )
                .pointerInput(slice.sliceId, slice.isCompleted, swipeThresholdPx) {
                    if (slice.isCompleted) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val current = dragOffsetX
                            val next = (current + dragAmount).coerceAtLeast(0f)
                            val crossedThresholdNow = current < swipeThresholdPx && next >= swipeThresholdPx
                            dragOffsetX = next
                            if (crossedThresholdNow) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragEnd = {
                            val finalDrag = dragOffsetX
                            scope.launch {
                                Animatable(finalDrag).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffsetX = value
                                }
                            }
                            if (finalDrag >= swipeThresholdPx) {
                                triggerSlashCompletion()
                            }
                        },
                        onDragCancel = {
                            val finalDrag = dragOffsetX
                            scope.launch {
                                Animatable(finalDrag).animateTo(0f) {
                                    dragOffsetX = value
                                }
                            }
                        }
                    )
                }
                .padding(start = 15.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Top Boss Health Bar for this task's parent chapter chain
                BossHealthBar(
                    chain = chain,
                    coreSlices = item.coreSiblingSlices,
                    currentBossHp = item.currentBossHp,
                    compact = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Badges Row: Subject ShortCode, Short Slice Badge, Phase Tag, Energy & Multipliers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Subject Emblem
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(subjectColor.copy(alpha = 0.2f))
                                .border(1.dp, subjectColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chain.subject.shortCode,
                                color = subjectColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }

                        // Slice Badge (e.g., HF Ch 4-2)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianSurfaceElevated)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = slice.shortBadge,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Phase Tag (e.g., [Textbook & Examples] or [Retention Echo])
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (slice.isRetentionEcho) CyberMagenta.copy(alpha = 0.18f)
                                    else CyberCyan.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "[${slice.phaseTag}]",
                                color = if (slice.isRetentionEcho) Color(0xFFEA80FC) else CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    // Lag Status Badge
                    if (slice.isLagging) {
                        val lagDays = slice.effectiveLagDays
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isNature) NatureLagRust.copy(alpha = 0.22f) else CyberCrimson.copy(alpha = 0.22f))
                                .border(1.dp, if (isNature) NatureLagRust else CyberCrimson, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isNature) "🍂 +${lagDays}d LATE" else "⚠️ +${lagDays}d LAG",
                                color = if (isNature) NatureLagRust else CyberCrimson,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    }

                    // 2x XP Multiplier or Ready-to-Slash Indicator
                    if (slice.xpMultiplier > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberAmber.copy(alpha = 0.22f))
                                .border(1.dp, CyberAmber, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⚡ ${slice.xpMultiplier}x XP",
                                color = CyberAmber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (isReadyToSlash) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberEmerald.copy(alpha = 0.22f))
                                .border(1.dp, CyberEmerald, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⚡ SLASH READY",
                                color = CyberEmerald,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Task Slice Title (Full Bangla Chapter Name) + Energy Indicator
                val banglaChapterName = chain.chapterName.trim().ifEmpty {
                    HscSyllabusCatalog.getChapterNameBangla(
                        chain.subject,
                        chain.paper,
                        chain.chapterNumber
                    )
                }
                val bnCh = HscSyllabusCatalog.toBanglaDigits(chain.chapterNumber)
                val bnSlice = HscSyllabusCatalog.toBanglaDigits(slice.sliceNumber)
                val paperLabel = if (chain.subject.isSinglePaperOnly || chain.paper == HscPaper.SINGLE) {
                    chain.subject.fullName
                } else {
                    "${chain.subject.fullName} ${chain.paper.compactTitleLabel}"
                }
                val resolvedCardTitle = if (slice.isRetentionEcho) {
                    "$paperLabel • অধ্যায় $bnCh: $banglaChapterName (Echo +${slice.echoIntervalDays ?: 3}d)"
                } else {
                    "$paperLabel • অধ্যায় $bnCh ($bnSlice): $banglaChapterName"
                }

                Text(
                    text = resolvedCardTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (slice.isCompleted) TextMuted else TextPrimary,
                    textDecoration = if (slice.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val energyEmoji = when (item.effectiveEnergyLevel) {
                    EnergyLevel.HIGH_FOCUS -> "⚡ 100% High Focus"
                    EnergyLevel.MEDIUM -> "🔋 50% Medium"
                    EnergyLevel.ZOMBIE_MODE -> "🧟 15% Zombie Friendly"
                }
                Text(
                    text = if (slice.isRetentionEcho) {
                        "🧠 Spaced Repetition Echo • $energyEmoji"
                    } else {
                        "Slice ${slice.sliceNumber} of ${slice.totalSlicesInChain} • $energyEmoji"
                    },
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Bottom Action Row: Sub-Counter Micro-Chip, +1d Domino Push, Slash Button, Cascade Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Interactive Sub-Counter Micro-Chip: [ - 2 / 5 Problems + ]
                        if (slice.problemsTarget > 0) {
                            val isTargetReached = slice.problemsCompleted >= slice.problemsTarget
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when {
                                    slice.isCompleted || isTargetReached -> CyberEmerald.copy(alpha = 0.2f)
                                    else -> ObsidianSurfaceElevated
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isTargetReached) CyberEmerald else subjectColor.copy(alpha = 0.6f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!slice.isCompleted && slice.problemsCompleted > 0) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrement Problem",
                                            tint = TextSecondary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onDecrementProblem()
                                                }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable(enabled = !slice.isCompleted && !isTargetReached) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            onIncrementProblem()
                                        }
                                    ) {
                                        Text(
                                            text = "${slice.problemsCompleted} / ${slice.problemsTarget} Problems",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTargetReached) CyberEmerald else TextPrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (!slice.isCompleted && !isTargetReached) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increment Problem",
                                                tint = subjectColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1-Tap Slash Completion Button (also triggered by Swipe-to-Slash)
                        if (!slice.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isReadyToSlash) CyberEmerald else ObsidianSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isReadyToSlash) CyberEmerald else CyberCyan.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.clickable {
                                    triggerSlashCompletion()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isReadyToSlash) Icons.Default.Bolt else Icons.Default.Check,
                                        contentDescription = "Slash Complete",
                                        tint = if (isReadyToSlash) Color(0xFF00210E) else CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isReadyToSlash) "Slash!" else "Slash",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReadyToSlash) Color(0xFF00210E) else CyberCyan,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(CyberEmerald.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "✅ SLASHED",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberEmerald,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Right Action Icons: +1 Day Domino Push, Catch Up & Cascade Delete (Trash)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!slice.isCompleted && slice.isLagging) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (isNature) NatureLeafGreen else CyberCyan).copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isNature) NatureLeafGreen else CyberCyan),
                                modifier = Modifier
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCatchUpToToday()
                                    }
                                    .padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isNature) "🌿 Catch Up" else "🚀 Catch Up",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isNature) NatureLeafGreen else CyberCyan,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (!slice.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated,
                                modifier = Modifier
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPushDominoPlusOneDay()
                                    }
                                    .padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Push Domino +1 Day",
                                        tint = if (isNature) NatureSproutLime else CyberAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "+1d",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNature) NatureSproutLime else CyberAmber,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRequestDelete()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Cascade Delete Options",
                                tint = CyberCrimson.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Nature Living Ladybug border companion along card perimeter
            if (isNature && !slice.isCompleted) {
                NatureLadybugBorder(
                    modifier = Modifier.matchParentSize()
                )
            }

            // Nature Vine Swipe Slash Overlay or Katana Slash Overlay & Particle Burst Canvas
            if (isNature && isBursting) {
                NatureVineSwipeOverlay(
                    progress = burstProgress.value,
                    modifier = Modifier.matchParentSize()
                )
            } else if (slashRatio > 0.04f || isBursting) {
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    if (slashRatio > 0.04f) {
                        val progress = slashRatio.coerceIn(0f, 1f)
                        val startPoint = Offset(12.dp.toPx(), size.height * 0.62f)
                        val endPoint = Offset(
                            12.dp.toPx() + (size.width - 24.dp.toPx()) * progress,
                            size.height * (0.62f - 0.28f * progress)
                        )
                        // Outer neon katana glow
                        drawLine(
                            color = CyberCyan.copy(alpha = 0.45f),
                            start = startPoint,
                            end = endPoint,
                            strokeWidth = 9.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Inner white-hot blade core
                        drawLine(
                            color = if (slashRatio >= 1f) CyberEmerald else Color.White,
                            start = startPoint,
                            end = endPoint,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    if (isBursting) {
                        val p = burstProgress.value
                        val center = Offset(size.width * 0.5f, size.height * 0.5f)
                        for (particle in particles) {
                            val distance = particle.speed * p
                            val px = center.x + cos(particle.angleRad) * distance
                            val py = center.y + sin(particle.angleRad) * distance
                            val alpha = (1f - p).coerceIn(0f, 1f)
                            drawCircle(
                                color = particle.color.copy(alpha = alpha),
                                radius = particle.radiusPx * (1f - p * 0.5f),
                                center = Offset(px, py)
                            )
                        }
                    }
                }
            }
        }
    }
}
