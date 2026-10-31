package com.chronos.hsc.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chronos.hsc.data.local.ConfidenceRating
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import com.chronos.hsc.ui.theme.brandColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EbbinghausEchoDialog(
    conqueredChain: StudyTopicChain,
    onConfirm: (spawnEchoes: Boolean, confidenceRating: ConfidenceRating) -> Unit
) {
    var spawnEchoes by remember { mutableStateOf(true) }
    var selectedRating by remember { mutableStateOf(ConfidenceRating.MEDIUM) }
    val subjectColor = conqueredChain.subject.brandColor()

    val infiniteTransition = rememberInfiniteTransition(label = "bossDefeatBurst")
    val burstPhase by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "burstPhase"
    )

    Dialog(
        onDismissRequest = {
            onConfirm(spawnEchoes, selectedRating)
        }
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, CyberEmerald),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Celebratory Particle Burst Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val center = Offset(size.width / 2f, size.height * 0.45f)
                    val colors = listOf(CyberEmerald, subjectColor, CyberAmber, CyberCyan)
                    val count = 20
                    for (i in 0 until count) {
                        val angle = (i * (360f / count)) * (Math.PI.toFloat() / 180f)
                        val radius = 24f + burstPhase * (size.width * 0.38f)
                        val px = center.x + cos(angle) * radius
                        val py = center.y + sin(angle) * (radius * 0.55f)
                        drawCircle(
                            color = colors[i % colors.size].copy(alpha = (1f - burstPhase).coerceIn(0f, 0.85f)),
                            radius = 5.dp.toPx() * (1.1f - burstPhase * 0.5f),
                            center = Offset(px, py)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆 CHAPTER BOSS CONQUERED!",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = CyberEmerald
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val chapterLabel = if (conqueredChain.chapterName.isNotBlank()) {
                        "Ch ${conqueredChain.chapterNumber}: ${conqueredChain.chapterName}"
                    } else {
                        "Chapter ${conqueredChain.chapterNumber}"
                    }
                    Text(
                        text = "${conqueredChain.subject.fullName} • $chapterLabel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "All ${conqueredChain.totalDays} daily slices slashed (+100 Boss Bonus XP)",
                        fontSize = 12.sp,
                        color = subjectColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle: Spawn Ebbinghaus Retention Echoes?
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianBackground)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Spawn Ebbinghaus Retention Echoes?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Auto-schedules 3 spaced 15-min review slices linked to this chainId",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = spawnEchoes,
                            onCheckedChange = { spawnEchoes = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBackground,
                                checkedTrackColor = CyberEmerald
                            )
                        )
                    }

                    if (spawnEchoes) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "SELECT CONFIDENCE RATING (EBBINGHAUS CURVE)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberCyan,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConfidenceRating.entries.forEach { rating ->
                                val isSelected = selectedRating == rating
                                val ratingEmoji = when (rating) {
                                    ConfidenceRating.BRUTAL -> "🔥"
                                    ConfidenceRating.MEDIUM -> "⚖️"
                                    ConfidenceRating.EASY -> "⚡"
                                }
                                val accentColor = when (rating) {
                                    ConfidenceRating.BRUTAL -> CyberCrimson
                                    ConfidenceRating.MEDIUM -> CyberAmber
                                    ConfidenceRating.EASY -> CyberEmerald
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) accentColor.copy(alpha = 0.16f) else ObsidianSurfaceElevated,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else ObsidianBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedRating = rating }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "$ratingEmoji ${rating.title}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) accentColor else TextPrimary
                                            )
                                            Text(
                                                text = rating.subtitle,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "15m • Zombie",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onConfirm(spawnEchoes, selectedRating)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberEmerald,
                            contentColor = Color(0xFF00210E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (spawnEchoes) {
                                "Claim Victory & Spawn 3 Echoes"
                            } else {
                                "Claim Victory (Skip Echoes)"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
