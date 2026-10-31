package com.chronos.hsc.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlin.math.max
import kotlin.math.roundToInt

@Entity(tableName = "study_topic_chains")
data class StudyTopicChain(
    @PrimaryKey
    val chainId: String,
    val subject: HscSubject,
    val paper: HscPaper,
    val chapterNumber: Int,
    val chapterName: String,
    val totalDays: Int,
    val distributionCurve: DistributionCurve,
    val energyLevel: EnergyLevel,
    val dailyProblemTarget: Int,
    val createdAtEpochDay: Long,
    val bossMaxHp: Int,
    val bossBonusShieldHp: Int = 0,
    val isChainCompleted: Boolean = false
)

@Entity(
    tableName = "daily_task_slices",
    foreignKeys = [
        ForeignKey(
            entity = StudyTopicChain::class,
            parentColumns = ["chainId"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chainId"]),
        Index(value = ["scheduledDateEpochDay"])
    ]
)
data class DailyTaskSlice(
    @PrimaryKey
    val sliceId: String,
    val chainId: String,
    val sliceNumber: Int,
    val totalSlicesInChain: Int,
    val displayTitle: String,
    val shortBadge: String,
    val phaseTag: String,
    val scheduledDateEpochDay: Long,
    val problemsCompleted: Int = 0,
    val problemsTarget: Int = 5,
    val isCompleted: Boolean = false,
    val completedAtTimestamp: Long? = null,
    val isRetentionEcho: Boolean = false,
    val echoIntervalDays: Int? = null,
    val sliceEnergyOverride: EnergyLevel? = null,
    val xpMultiplier: Int = 1
) {
    val problemProgressFraction: Float
        get() = when {
            isCompleted -> 1f
            problemsTarget <= 0 -> 0f
            else -> (problemsCompleted.toFloat() / problemsTarget.toFloat()).coerceIn(0f, 1f)
        }
}

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val streakDays: Int = 0,
    val lastStudyEpochDay: Long? = null
) {
    val xpIntoCurrentLevel: Int
        get() = totalXp % 500

    val xpForNextLevel: Int
        get() = 500

    val levelProgressFraction: Float
        get() = (xpIntoCurrentLevel.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
}

data class TopicChainWithSlices(
    @Embedded
    val chain: StudyTopicChain,
    @Relation(
        parentColumn = "chainId",
        entityColumn = "chainId"
    )
    val slices: List<DailyTaskSlice>
) {
    val coreSlices: List<DailyTaskSlice>
        get() = slices.filter { !it.isRetentionEcho }.sortedBy { it.sliceNumber }

    val echoSlices: List<DailyTaskSlice>
        get() = slices.filter { it.isRetentionEcho }.sortedBy { it.scheduledDateEpochDay }

    val completedCoreSlicesCount: Int
        get() = coreSlices.count { it.isCompleted }

    /**
     * Calculates remaining Boss HP across all segments, taking into account
     * full segment completions and partial sub-counter chip damage on active slices.
     */
    val currentBossHp: Int
        get() {
            val core = coreSlices
            if (core.isEmpty()) return 0
            val hpPerSlice = (chain.bossMaxHp.toFloat() / max(1, chain.totalDays).toFloat())
            var damageDealt = 0f
            for (slice in core) {
                damageDealt += if (slice.isCompleted) {
                    hpPerSlice
                } else if (slice.problemsTarget > 0) {
                    // Chip damage up to 75% of the segment before final slash completion
                    hpPerSlice * 0.75f * slice.problemProgressFraction
                } else {
                    0f
                }
            }
            return (chain.bossMaxHp - damageDealt.roundToInt()).coerceIn(0, chain.bossMaxHp)
        }

    val bossHpFraction: Float
        get() = if (chain.bossMaxHp <= 0) 0f else (currentBossHp.toFloat() / chain.bossMaxHp.toFloat()).coerceIn(0f, 1f)
}

data class SliceWithParentChain(
    @Embedded
    val slice: DailyTaskSlice,
    @Relation(
        parentColumn = "chainId",
        entityColumn = "chainId"
    )
    val chain: StudyTopicChain,
    @Relation(
        parentColumn = "chainId",
        entityColumn = "chainId"
    )
    val siblingSlices: List<DailyTaskSlice>
) {
    val effectiveEnergyLevel: EnergyLevel
        get() = when {
            slice.sliceEnergyOverride != null -> slice.sliceEnergyOverride
            slice.isRetentionEcho -> EnergyLevel.ZOMBIE_MODE
            else -> chain.energyLevel
        }

    val isLightReviewEligibleInZombieMode: Boolean
        get() = effectiveEnergyLevel == EnergyLevel.ZOMBIE_MODE ||
            slice.isRetentionEcho ||
            slice.phaseTag.contains("Theory", ignoreCase = true) ||
            slice.phaseTag.contains("Lecture", ignoreCase = true) ||
            slice.phaseTag.contains("Light Review", ignoreCase = true)

    val coreSiblingSlices: List<DailyTaskSlice>
        get() = siblingSlices.filter { !it.isRetentionEcho }.sortedBy { it.sliceNumber }

    val currentBossHp: Int
        get() {
            val core = coreSiblingSlices
            if (core.isEmpty()) return 0
            val hpPerSlice = chain.bossMaxHp.toFloat() / max(1, chain.totalDays).toFloat()
            var damageDealt = 0f
            for (s in core) {
                damageDealt += if (s.isCompleted) {
                    hpPerSlice
                } else if (s.problemsTarget > 0) {
                    hpPerSlice * 0.75f * s.problemProgressFraction
                } else {
                    0f
                }
            }
            return (chain.bossMaxHp - damageDealt.roundToInt()).coerceIn(0, chain.bossMaxHp)
        }
}
