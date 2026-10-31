package com.chronos.hsc.domain

import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.StudyTopicChain
import java.time.LocalDate

data class DeletedChainSnapshot(
    val chain: StudyTopicChain,
    val slices: List<DailyTaskSlice>
)

data class DominoShiftResult(
    val shiftedChainsCount: Int,
    val shiftedSlicesCount: Int
)

data class SliceCompletionResult(
    val completedSlice: DailyTaskSlice,
    val parentChain: StudyTopicChain,
    val xpEarned: Int,
    val didConquerFinalCoreSlice: Boolean
)

data class HeatmapDayCell(
    val date: LocalDate,
    val epochDay: Long,
    val completedSlicesCount: Int,
    val problemsSolvedCount: Int,
    val weightedScore: Int,
    val intensityLevel: Int
)

data class SubjectAnalyticsSummary(
    val subject: HscSubject,
    val totalSlices: Int,
    val completedSlices: Int,
    val totalProblemsSolved: Int,
    val activeChainsCount: Int
) {
    val completionFraction: Float
        get() = if (totalSlices <= 0) 0f else (completedSlices.toFloat() / totalSlices.toFloat()).coerceIn(0f, 1f)
}
