package com.chronos.hsc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.domain.HeatmapDayCell
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.HeatmapLevel0
import com.chronos.hsc.ui.theme.HeatmapLevel1
import com.chronos.hsc.ui.theme.HeatmapLevel2
import com.chronos.hsc.ui.theme.HeatmapLevel3
import com.chronos.hsc.ui.theme.HeatmapLevel4
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun EffortHeatmapCanvas(
    cells: List<HeatmapDayCell>,
    modifier: Modifier = Modifier
) {
    val paddedCells = remember(cells) {
        if (cells.size >= 84) {
            cells.takeLast(84)
        } else {
            val missing = 84 - cells.size
            val firstDate = cells.firstOrNull()?.date ?: LocalDate.now()
            val dummyList = (missing downTo 1).map { offset ->
                val d = firstDate.minusDays(offset.toLong())
                HeatmapDayCell(
                    date = d,
                    epochDay = d.toEpochDay(),
                    completedSlicesCount = 0,
                    problemsSolvedCount = 0,
                    weightedScore = 0,
                    intensityLevel = 0
                )
            }
            dummyList + cells
        }
    }
    var selectedCell by remember(cells, paddedCells) {
        mutableStateOf(cells.lastOrNull() ?: paddedCells.lastOrNull())
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    val totalWeightedScore = remember(paddedCells) {
        paddedCells.sumOf { it.weightedScore }
    }
    val activeDaysCount = remember(paddedCells) {
        paddedCells.count { it.weightedScore > 0 }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔥 12-WEEK WEIGHTED EFFORT HEATMAP",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = CyberEmerald
                )
                Text(
                    text = "Formula: (Slices × 25) + (Problems × 5)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$totalWeightedScore pts",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = CyberCyan
                )
                Text(
                    text = "$activeDaysCount / 84 active days",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7 rows x 12 columns Custom Compose Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .pointerInput(paddedCells) {
                    detectTapGestures { tapOffset ->
                        val cols = 12
                        val rows = 7
                        val gapPx = 4.dp.toPx()
                        val cellWidth = (size.width - gapPx * (cols - 1)) / cols.toFloat()
                        val cellHeight = (size.height - gapPx * (rows - 1)) / rows.toFloat()

                        val col = (tapOffset.x / (cellWidth + gapPx)).toInt().coerceIn(0, cols - 1)
                        val row = (tapOffset.y / (cellHeight + gapPx)).toInt().coerceIn(0, rows - 1)
                        val idx = col * rows + row
                        paddedCells.getOrNull(idx)?.let { clicked ->
                            selectedCell = clicked
                        }
                    }
                }
        ) {
            val cols = 12
            val rows = 7
            val gapPx = 4.dp.toPx()
            val cellWidth = ((size.width - gapPx * (cols - 1)) / cols.toFloat()).coerceAtLeast(4f)
            val cellHeight = ((size.height - gapPx * (rows - 1)) / rows.toFloat()).coerceAtLeast(4f)
            val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            for (col in 0 until cols) {
                for (row in 0 until rows) {
                    val idx = col * rows + row
                    val cell = paddedCells.getOrNull(idx)
                    val x = col * (cellWidth + gapPx)
                    val y = row * (cellHeight + gapPx)

                    val levelColor = levelToColor(cell?.intensityLevel ?: 0)
                    drawRoundRect(
                        color = levelColor,
                        topLeft = Offset(x, y),
                        size = Size(cellWidth, cellHeight),
                        cornerRadius = corner
                    )

                    if (cell != null && selectedCell?.epochDay == cell.epochDay) {
                        drawRoundRect(
                            color = CyberCyan,
                            topLeft = Offset(x, y),
                            size = Size(cellWidth, cellHeight),
                            cornerRadius = corner,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend + Selected Day Inspector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val inspected = selectedCell
            if (inspected != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${inspected.date.format(dateFormatter)}: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${inspected.weightedScore} effort (${inspected.completedSlicesCount} slices, ${inspected.problemsSolvedCount} probs)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberEmerald
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Less", fontSize = 10.sp, color = TextMuted)
                (0..4).forEach { lvl ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(levelToColor(lvl))
                    )
                }
                Text("More", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

private fun levelToColor(level: Int): Color = when (level) {
    0 -> HeatmapLevel0
    1 -> HeatmapLevel1
    2 -> HeatmapLevel2
    3 -> HeatmapLevel3
    else -> HeatmapLevel4
}
