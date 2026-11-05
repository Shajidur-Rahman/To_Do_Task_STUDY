package com.chronos.hsc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.ui.theme.BossShieldPurple
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberOrange
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.brandColor
import com.chronos.hsc.ui.theme.AppThemeMode
import com.chronos.hsc.ui.theme.LocalAppThemeMode
import kotlin.math.max

@Composable
fun BossHealthBar(
    chain: StudyTopicChain,
    coreSlices: List<DailyTaskSlice>,
    currentBossHp: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    barHeight: Dp = if (compact) 8.dp else 12.dp
) {
    if (LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER) {
        NatureWormBossBar(
            chain = chain,
            coreSlices = coreSlices,
            currentBossHp = currentBossHp,
            modifier = modifier,
            compact = compact,
            barHeight = barHeight
        )
        return
    }

    val totalSegments = max(1, chain.totalDays)
    val sortedCore = coreSlices.sortedBy { it.sliceNumber }
    val subjectColor = chain.subject.brandColor()
    val hasPurpleShield = chain.bossBonusShieldHp > 0

    val infiniteTransition = rememberInfiniteTransition(label = "bossShieldPulse")
    val shieldPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldPulseAlpha"
    )

    // Compute per-segment remaining HP ratio (1.0f = full boss segment, 0.0f = defeated segment)
    val targetRatios = remember(sortedCore, totalSegments) {
        (0 until totalSegments).map { index ->
            val slice = sortedCore.getOrNull(index)
            when {
                slice == null -> 1f
                slice.isCompleted -> 0f
                slice.problemsTarget > 0 -> {
                    // Sub-counter chip damage depletes up to 75% of the segment before final slash
                    (1f - (0.75f * slice.problemProgressFraction)).coerceIn(0.25f, 1f)
                }
                else -> 1f
            }
        }
    }

    var oldRatios by remember { mutableStateOf(targetRatios) }
    val animProgress = remember { Animatable(1f) }

    LaunchedEffect(targetRatios) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        oldRatios = targetRatios
    }

    Column(modifier = modifier.fillMaxWidth()) {
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
                    text = when {
                        chain.isChainCompleted -> "⚔️ BOSS SLAIN"
                        compact && hasPurpleShield -> "👹 BOSS HP"
                        else -> "👹 CHAPTER BOSS HP"
                    },
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (chain.isChainCompleted) CyberEmerald else CyberCrimson,
                    maxLines = 1,
                    softWrap = false
                )
                if (hasPurpleShield) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BossShieldPurple.copy(alpha = 0.22f))
                            .border(
                                width = 1.dp,
                                color = BossShieldPurple.copy(alpha = shieldPulseAlpha),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (compact) "🛡️ +${chain.bossBonusShieldHp}" else "🛡️ +${chain.bossBonusShieldHp} SHIELD",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA80FC),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = when {
                    hasPurpleShield && compact -> "$currentBossHp/${chain.bossMaxHp} (+${chain.bossBonusShieldHp})"
                    hasPurpleShield -> "$currentBossHp + ${chain.bossBonusShieldHp} / ${chain.bossMaxHp} HP"
                    else -> "$currentBossHp / ${chain.bossMaxHp} HP"
                },
                fontSize = if (compact) 10.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (currentBossHp == 0) CyberEmerald else TextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianBackground)
                .border(
                    width = 1.dp,
                    color = if (hasPurpleShield) BossShieldPurple.copy(alpha = shieldPulseAlpha) else ObsidianBorder,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            val gapPx = 3.dp.toPx()
            val totalGapsWidth = gapPx * (totalSegments - 1).coerceAtLeast(0)
            val segmentWidth = ((size.width - totalGapsWidth) / totalSegments.toFloat()).coerceAtLeast(2f)

            for (i in 0 until totalSegments) {
                val startX = i * (segmentWidth + gapPx)
                val p = animProgress.value
                val startVal = oldRatios.getOrElse(i) { targetRatios[i] }
                val endVal = targetRatios[i]
                val remainingRatio = startVal + (endVal - startVal) * p

                // Segment empty track
                drawRoundRect(
                    color = Color(0xFF161C2E),
                    topLeft = Offset(startX, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                if (remainingRatio > 0.01f) {
                    val fillWidth = segmentWidth * remainingRatio
                    val barBrush = if (remainingRatio < 1f) {
                        // Taking chip damage from problem sub-counter!
                        Brush.horizontalGradient(
                            colors = listOf(CyberOrange, CyberCrimson),
                            startX = startX,
                            endX = startX + fillWidth
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(CyberCrimson, subjectColor),
                            startX = startX,
                            endX = startX + fillWidth
                        )
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(startX, 0f),
                        size = Size(fillWidth, size.height),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                } else {
                    // Slashed / Depleted segment indicator
                    drawLine(
                        color = CyberEmerald.copy(alpha = 0.7f),
                        start = Offset(startX + 4f, size.height - 2f),
                        end = Offset(startX + segmentWidth - 4f, 2f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // If Boss has an Overdue Purple Shield, render glowing top shield overlay
            if (hasPurpleShield) {
                val shieldFraction = (chain.bossBonusShieldHp.toFloat() / max(1, chain.bossMaxHp).toFloat())
                    .coerceIn(0.1f, 1f)
                drawRoundRect(
                    color = BossShieldPurple.copy(alpha = 0.55f * shieldPulseAlpha),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width * shieldFraction, size.height * 0.42f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        if (!compact) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val completedSegments = sortedCore.count { it.isCompleted }
                Text(
                    text = "$completedSegments / $totalSegments Slices Slashed",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "${((1f - (currentBossHp.toFloat() / max(1, chain.bossMaxHp).toFloat())) * 100).toInt()}% Damage Dealt",
                    fontSize = 10.sp,
                    color = subjectColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
