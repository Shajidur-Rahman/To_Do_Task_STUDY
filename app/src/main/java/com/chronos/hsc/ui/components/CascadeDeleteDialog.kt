package com.chronos.hsc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.data.local.TopicChainWithSlices
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary

@Composable
fun CascadeDeleteDialog(
    slicePendingDelete: SliceWithParentChain?,
    chainPendingDelete: TopicChainWithSlices?,
    onDismiss: () -> Unit,
    onDeleteEntireChain: (chainId: String) -> Unit,
    onDeleteSingleSliceAndReindex: (sliceId: String) -> Unit
) {
    val chain: StudyTopicChain = slicePendingDelete?.chain
        ?: chainPendingDelete?.chain
        ?: return

    val allSlices: List<DailyTaskSlice> = slicePendingDelete?.siblingSlices
        ?: chainPendingDelete?.slices
        ?: emptyList()

    val coreBadges = allSlices
        .filter { !it.isRetentionEcho }
        .sortedBy { it.sliceNumber }
        .joinToString(", ") { "${ chain.chapterNumber }-${it.sliceNumber}" }

    val echoCount = allSlices.count { it.isRetentionEcho }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, CyberCrimson.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "⚠️ CASCADE DELETE ENGINE",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberCrimson
                )

                Spacer(modifier = Modifier.height(6.dp))

                val chapterLabel = if (chain.chapterName.isNotBlank()) {
                    "Ch ${chain.chapterNumber}: ${chain.chapterName}"
                } else {
                    "Chapter ${chain.chapterNumber}"
                }
                Text(
                    text = "${chain.subject.fullName} (${chain.paper.fullName}) • $chapterLabel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Chain Scope Breakdown Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBackground)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "LINKED SLICES IN THIS TOPIC CHAIN (${allSlices.size} TOTAL):",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Core Chapter Slices: [$coreBadges]",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        if (echoCount > 0) {
                            Text(
                                text = "• Spaced Repetition Echoes: $echoCount scheduled echo(es)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Primary Destructive Action - Delete Entire Topic Chain
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CyberCrimson.copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, CyberCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeleteEntireChain(chain.chainId) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔥 Delete Entire Topic Chain",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = CyberCrimson
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberCrimson)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RECOMMENDED",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Executes ForeignKey.CASCADE to wipe the parent topic AND all ${allSlices.size} past, today's, and future slices ($coreBadges) + retention echoes across all dates.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }

                // Option 2: Delete Only This Slice & Re-Index (shown when clicked from a specific slice card)
                if (slicePendingDelete != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val clickedSlice = slicePendingDelete.slice
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeleteSingleSliceAndReindex(clickedSlice.sliceId) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "✂️ Delete Only This Slice (${clickedSlice.shortBadge}) & Re-Index",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CyberAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Removes only ${clickedSlice.shortBadge} and re-numbers subsequent slices (pulling their scheduled dates back by 1 day).",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
