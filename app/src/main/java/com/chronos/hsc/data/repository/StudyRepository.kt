package com.chronos.hsc.data.repository

import android.content.Context
import com.chronos.hsc.data.local.ConfidenceRating
import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.DistributionCurve
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.NeuroDrillType
import com.chronos.hsc.data.local.NeuroSessionEntity
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.local.StudyDao
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.data.local.TopicChainWithSlices
import com.chronos.hsc.data.local.UserStats
import com.chronos.hsc.domain.DeletedChainSnapshot
import com.chronos.hsc.domain.DominoShiftResult
import com.chronos.hsc.domain.HeatmapDayCell
import com.chronos.hsc.domain.NeuroTelemetrySummary
import com.chronos.hsc.domain.SliceCompletionResult
import com.chronos.hsc.domain.SubjectAnalyticsSummary
import com.chronos.hsc.widget.ChronosWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class StudyRepository(
    private val dao: StudyDao,
    private val appContext: Context
) {

    fun observeAllChainsWithSlices(): Flow<List<TopicChainWithSlices>> =
        dao.observeAllChainsWithSlices()

    fun observeAllSlicesWithParent(): Flow<List<SliceWithParentChain>> =
        dao.observeAllSlicesWithParent()

    fun observeSlicesForDate(epochDay: Long): Flow<List<SliceWithParentChain>> =
        dao.observeSlicesForDate(epochDay)

    fun observeOverdueSlices(todayEpochDay: Long): Flow<List<SliceWithParentChain>> =
        dao.observeOverdueSlices(todayEpochDay)

    fun observeUpcomingEchoes(todayEpochDay: Long): Flow<List<SliceWithParentChain>> =
        dao.observeUpcomingEchoes(todayEpochDay)

    fun observeUserStats(): Flow<UserStats> =
        dao.observeUserStats().map { it ?: UserStats() }

    fun observeAllExams(): Flow<List<ExamCountdownEntity>> =
        dao.observeAllExams()

    fun observePinnedExam(): Flow<ExamCountdownEntity?> =
        dao.observePinnedExam()

    /**
     * Builds the 12-week (84-day) weighted effort heatmap cells.
     * Weighted formula: (completedSlices * 25) + (problemsSolved * 5)
     */
    fun observeHeatmapCells(todayEpochDay: Long): Flow<List<HeatmapDayCell>> {
        val startEpochDay = todayEpochDay - 83L
        return dao.observeSlicesInDateRange(startEpochDay, todayEpochDay).map { slices ->
            val slicesByDay = slices.groupBy { it.scheduledDateEpochDay }
            (startEpochDay..todayEpochDay).map { dayEpoch ->
                val daySlices = slicesByDay[dayEpoch].orEmpty()
                val completedCount = daySlices.count { it.isCompleted }
                val problemsSolved = daySlices.sumOf { it.problemsCompleted }
                val weightedScore = (completedCount * 25) + (problemsSolved * 5)
                val level = when {
                    weightedScore <= 0 -> 0
                    weightedScore < 35 -> 1
                    weightedScore < 75 -> 2
                    weightedScore < 125 -> 3
                    else -> 4
                }
                HeatmapDayCell(
                    date = LocalDate.ofEpochDay(dayEpoch),
                    epochDay = dayEpoch,
                    completedSlicesCount = completedCount,
                    problemsSolvedCount = problemsSolved,
                    weightedScore = weightedScore,
                    intensityLevel = level
                )
            }
        }
    }

    /**
     * Aggregates subject analytics across all chains and slices.
     */
    fun observeSubjectAnalytics(): Flow<List<SubjectAnalyticsSummary>> {
        return dao.observeAllChainsWithSlices().map { chainsWithSlices ->
            HscSubject.entries.map { subject ->
                val subjectChains = chainsWithSlices.filter { it.chain.subject == subject }
                val subjectSlices = subjectChains.flatMap { it.slices }
                SubjectAnalyticsSummary(
                    subject = subject,
                    totalSlices = subjectSlices.size,
                    completedSlices = subjectSlices.count { it.isCompleted },
                    totalProblemsSolved = subjectSlices.sumOf { it.problemsCompleted },
                    activeChainsCount = subjectChains.count { !it.chain.isChainCompleted }
                )
            }
        }
    }

    /**
     * Aggregates Neuro-Gym cognitive telemetry across all recorded sessions.
     */
    fun observeNeuroTelemetry(): Flow<NeuroTelemetrySummary> {
        return dao.observeAllNeuroSessions().map { sessions ->
            val fovealSeconds = sessions
                .filter { it.drillType == NeuroDrillType.FOVEAL_LOCK }
                .sumOf { it.durationSeconds }
            val schulteTimes = sessions
                .filter { it.drillType == NeuroDrillType.SCHULTE_GRID && it.scoreMetric > 0.0 }
                .map { it.scoreMetric }
            val quantQpms = sessions
                .filter { it.drillType == NeuroDrillType.QUANT_REFLEX }
                .map { it.scoreMetric.roundToInt() }
            val nBackAccuracies = sessions
                .filter { it.drillType == NeuroDrillType.SPATIAL_N_BACK }
                .map { it.scoreMetric.roundToInt() }
            val chimpLevels = sessions
                .filter { it.drillType == NeuroDrillType.CHIMP_MEMORY }
                .map { it.scoreMetric.roundToInt() }
            val sighSeconds = sessions
                .filter { it.drillType == NeuroDrillType.OCULOMOTOR_SIGH }
                .sumOf { it.durationSeconds }

            NeuroTelemetrySummary(
                totalFocusLockSeconds = fovealSeconds,
                totalSessionsCompleted = sessions.size,
                fastestSchulteSeconds = schulteTimes.minOrNull(),
                highestQuantQpm = quantQpms.maxOrNull() ?: 0,
                highestNBackAccuracyPercent = nBackAccuracies.maxOrNull() ?: 0,
                maxChimpMemorySpan = chimpLevels.maxOrNull() ?: 0,
                totalOculomotorResetSeconds = sighSeconds,
                recentSessions = sessions.take(8)
            )
        }
    }

    /**
     * Syncs any overdue Boss Shields on application startup without inserting any fake data.
     */
    suspend fun initializeOnLaunch() {
        syncOverdueBossShields(LocalDate.now().toEpochDay())
        ChronosWidgetUpdater.requestUpdate(appContext)
    }

    // =========================================================================
    // EXAM COUNTDOWN REPOSITORY METHODS
    // =========================================================================

    suspend fun saveAndPinExam(
        examTitle: String,
        targetDateEpochDay: Long
    ): ExamCountdownEntity {
        val cleanTitle = examTitle.trim().ifEmpty { "HSC Exam" }
        val exam = ExamCountdownEntity(
            examId = UUID.randomUUID().toString(),
            examTitle = cleanTitle,
            targetDateEpochDay = targetDateEpochDay,
            createdAtEpochDay = LocalDate.now().toEpochDay(),
            isPrimaryPinned = true
        )
        dao.upsertAndPinExam(exam)
        return exam
    }

    suspend fun pinExam(examId: String) {
        dao.pinExistingExam(examId)
    }

    suspend fun deleteExam(examId: String) {
        dao.deleteExam(examId)
        val remaining = dao.getAllExamsOnce()
        if (remaining.isNotEmpty() && remaining.none { it.isPrimaryPinned }) {
            dao.pinExistingExam(remaining.first().examId)
        }
    }

    // =========================================================================
    // NEURO-GYM SESSION RECORDING
    // =========================================================================

    suspend fun recordCompletedNeuroSession(
        drillType: NeuroDrillType,
        scoreMetric: Double,
        durationSeconds: Int
    ): Int {
        val session = NeuroSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            drillType = drillType,
            scoreMetric = scoreMetric,
            durationSeconds = durationSeconds,
            timestamp = System.currentTimeMillis()
        )
        dao.insertNeuroSession(session)
        val xpAwarded = 25
        awardXp(xpAwarded)
        return xpAwarded
    }

    /**
     * Auto-Slicing Execution:
     * Inserts the StudyTopicChain parent and loops i from 1 to totalDays,
     * creating DailyTaskSlice rows for startDate.plusDays(i - 1).
     */
    suspend fun createStructuredTopicChain(
        subject: HscSubject,
        paper: HscPaper,
        chapterNumber: Int,
        chapterName: String,
        totalDays: Int,
        startDateEpochDay: Long,
        distributionCurve: DistributionCurve,
        dailyProblemTarget: Int,
        energyLevel: EnergyLevel
    ): StudyTopicChain {
        val effectivePaper = if (subject.isSinglePaperOnly) HscPaper.SINGLE else paper
        val safeTotalDays = totalDays.coerceIn(1, 15)
        val safeChapterNo = chapterNumber.coerceIn(1, 15)
        val cleanChapterName = chapterName.trim()
        val chainId = UUID.randomUUID().toString()
        val bossMaxHp = safeTotalDays * 100

        val parentChain = StudyTopicChain(
            chainId = chainId,
            subject = subject,
            paper = effectivePaper,
            chapterNumber = safeChapterNo,
            chapterName = cleanChapterName,
            totalDays = safeTotalDays,
            distributionCurve = distributionCurve,
            energyLevel = energyLevel,
            dailyProblemTarget = dailyProblemTarget.coerceIn(0, 30),
            createdAtEpochDay = LocalDate.now().toEpochDay(),
            bossMaxHp = bossMaxHp,
            bossBonusShieldHp = 0,
            isChainCompleted = false
        )

        val slices = (1..safeTotalDays).map { dayIndex ->
            val sliceDateEpoch = startDateEpochDay + (dayIndex - 1).toLong()
            DailyTaskSlice(
                sliceId = UUID.randomUUID().toString(),
                chainId = chainId,
                sliceNumber = dayIndex,
                totalSlicesInChain = safeTotalDays,
                displayTitle = formatSliceDisplayTitle(
                    subject = subject,
                    paper = effectivePaper,
                    chapterNumber = safeChapterNo,
                    sliceNumber = dayIndex,
                    chapterName = cleanChapterName
                ),
                shortBadge = formatShortBadge(
                    subject = subject,
                    paper = effectivePaper,
                    chapterNumber = safeChapterNo,
                    sliceNumber = dayIndex
                ),
                phaseTag = distributionCurve.phaseTagForDay(dayIndex, safeTotalDays),
                scheduledDateEpochDay = sliceDateEpoch,
                problemsCompleted = 0,
                problemsTarget = dailyProblemTarget.coerceIn(0, 30),
                isCompleted = false,
                completedAtTimestamp = null,
                isRetentionEcho = false,
                echoIntervalDays = null,
                sliceEnergyOverride = null,
                xpMultiplier = 1
            )
        }

        dao.insertChainWithSlices(parentChain, slices)
        ChronosWidgetUpdater.requestUpdate(appContext)
        return parentChain
    }

    /**
     * Full-Topic Cascade Delete Engine:
     * Captures a snapshot of the parent chain and all its child slices (for Undo),
     * then executes dao.deleteTopicChainById(chainId) which triggers ForeignKey.CASCADE
     * to permanently remove the parent topic AND all past, today, and future slices + echoes.
     */
    suspend fun deleteEntireTopicChainWithSnapshot(chainId: String): DeletedChainSnapshot? {
        val current = dao.getChainWithSlices(chainId) ?: return null
        val snapshot = DeletedChainSnapshot(
            chain = current.chain,
            slices = current.slices
        )
        dao.deleteTopicChainById(chainId)
        ChronosWidgetUpdater.requestUpdate(appContext)
        return snapshot
    }

    /**
     * Restores a cascade-deleted topic chain and all of its slices.
     */
    suspend fun restoreDeletedChainSnapshot(snapshot: DeletedChainSnapshot) {
        dao.insertChainWithSlices(snapshot.chain, snapshot.slices)
        ChronosWidgetUpdater.requestUpdate(appContext)
    }

    /**
     * Delete Only This Slice & Re-Index:
     * Deletes only the clicked slice and re-numbers subsequent slices
     * (e.g., 4-3 becomes 4-2, pulling their scheduled dates back by 1 day).
     */
    suspend fun deleteSingleSliceAndReindex(sliceId: String): Int {
        val targetSlice = dao.getSliceById(sliceId) ?: return 0
        val chainWithSlices = dao.getChainWithSlices(targetSlice.chainId) ?: return 0
        val parent = chainWithSlices.chain

        if (targetSlice.isRetentionEcho) {
            dao.deleteSliceById(sliceId)
            ChronosWidgetUpdater.requestUpdate(appContext)
            return 0
        }

        val remainingCoreSlices = chainWithSlices.coreSlices
            .filter { it.sliceId != sliceId }
            .sortedBy { it.sliceNumber }

        if (remainingCoreSlices.isEmpty() && chainWithSlices.echoSlices.isEmpty()) {
            dao.deleteTopicChainById(parent.chainId)
            ChronosWidgetUpdater.requestUpdate(appContext)
            return 0
        }

        val subsequentSlicesCount = remainingCoreSlices.count { it.sliceNumber > targetSlice.sliceNumber }
        dao.deleteSliceById(sliceId)

        val newTotalDays = max(1, remainingCoreSlices.size)
        val reindexedSlices = remainingCoreSlices.mapIndexed { idx, slice ->
            val newSliceNumber = idx + 1
            val wasAfterDeleted = slice.sliceNumber > targetSlice.sliceNumber
            val updatedEpochDay = if (wasAfterDeleted && !slice.isCompleted) {
                slice.scheduledDateEpochDay - 1L
            } else {
                slice.scheduledDateEpochDay
            }
            slice.copy(
                sliceNumber = newSliceNumber,
                totalSlicesInChain = newTotalDays,
                displayTitle = formatSliceDisplayTitle(
                    subject = parent.subject,
                    paper = parent.paper,
                    chapterNumber = parent.chapterNumber,
                    sliceNumber = newSliceNumber,
                    chapterName = parent.chapterName
                ),
                shortBadge = formatShortBadge(
                    subject = parent.subject,
                    paper = parent.paper,
                    chapterNumber = parent.chapterNumber,
                    sliceNumber = newSliceNumber
                ),
                phaseTag = parent.distributionCurve.phaseTagForDay(newSliceNumber, newTotalDays),
                scheduledDateEpochDay = updatedEpochDay
            )
        }

        if (reindexedSlices.isNotEmpty()) {
            dao.updateSlices(reindexedSlices)
        }

        val allRemainingDone = reindexedSlices.isNotEmpty() && reindexedSlices.all { it.isCompleted }
        dao.updateChain(
            parent.copy(
                totalDays = newTotalDays,
                bossMaxHp = newTotalDays * 100,
                isChainCompleted = allRemainingDone
            )
        )
        ChronosWidgetUpdater.requestUpdate(appContext)
        return subsequentSlicesCount
    }

    /**
     * The "Domino / Auto-Shift" Engine:
     * Groups incomplete slices by chainId ordered by sliceNumber ASC.
     * For each chain with overdue slices, calculates delta = todayEpochDay - earliestIncompleteSlice.scheduledDateEpochDay
     * and adds delta to scheduledDateEpochDay for EVERY incomplete slice in that chain,
     * keeping the sequence intact without stacking 4-2 and 4-3 on the same day.
     */
    suspend fun executeDominoShift(todayEpochDay: Long): DominoShiftResult {
        val overdueSlices = dao.getOverdueIncompleteSlicesOnce(todayEpochDay)
        if (overdueSlices.isEmpty()) return DominoShiftResult(0, 0)

        val affectedChainIds = overdueSlices.map { it.chainId }.distinct()
        var totalSlicesShifted = 0

        for (chainId in affectedChainIds) {
            val incompleteChainSlices = dao.getIncompleteSlicesForChain(chainId)
            if (incompleteChainSlices.isEmpty()) continue

            val coreIncomplete = incompleteChainSlices
                .filter { !it.isRetentionEcho }
                .sortedBy { it.sliceNumber }

            if (coreIncomplete.isNotEmpty()) {
                val earliestOverdue = coreIncomplete.firstOrNull { it.scheduledDateEpochDay < todayEpochDay }
                if (earliestOverdue != null) {
                    val delta = todayEpochDay - earliestOverdue.scheduledDateEpochDay
                    if (delta > 0) {
                        var previousDate = todayEpochDay - 1L
                        val shiftedCore = coreIncomplete.map { slice ->
                            val candidateDate = slice.scheduledDateEpochDay + delta
                            val resolvedDate = max(candidateDate, previousDate + 1L)
                            previousDate = resolvedDate
                            slice.copy(scheduledDateEpochDay = resolvedDate)
                        }
                        dao.updateSlices(shiftedCore)
                        totalSlicesShifted += shiftedCore.size
                    }
                }
            }

            // Also shift any overdue retention echoes to today
            val overdueEchoes = incompleteChainSlices.filter {
                it.isRetentionEcho && it.scheduledDateEpochDay < todayEpochDay
            }
            if (overdueEchoes.isNotEmpty()) {
                val shiftedEchoes = overdueEchoes.map { it.copy(scheduledDateEpochDay = todayEpochDay) }
                dao.updateSlices(shiftedEchoes)
                totalSlicesShifted += overdueEchoes.size
            }
        }

        ChronosWidgetUpdater.requestUpdate(appContext)
        return DominoShiftResult(
            shiftedChainsCount = affectedChainIds.size,
            shiftedSlicesCount = totalSlicesShifted
        )
    }

    /**
     * Per-Card "Push Domino (+1 Day)":
     * Shifts the selected slice and all subsequent incomplete slices in that chain forward by +1 day.
     */
    suspend fun pushSliceDominoPlusOneDay(sliceId: String): Int {
        val targetSlice = dao.getSliceById(sliceId) ?: return 0
        if (targetSlice.isRetentionEcho) {
            dao.updateSlice(
                targetSlice.copy(scheduledDateEpochDay = targetSlice.scheduledDateEpochDay + 1L)
            )
            ChronosWidgetUpdater.requestUpdate(appContext)
            return 1
        }

        val incompleteChainSlices = dao.getIncompleteSlicesForChain(targetSlice.chainId)
            .filter { !it.isRetentionEcho && it.sliceNumber >= targetSlice.sliceNumber }
            .sortedBy { it.sliceNumber }

        if (incompleteChainSlices.isEmpty()) return 0

        val updated = incompleteChainSlices.map { slice ->
            slice.copy(scheduledDateEpochDay = slice.scheduledDateEpochDay + 1L)
        }
        dao.updateSlices(updated)
        ChronosWidgetUpdater.requestUpdate(appContext)
        return updated.size
    }

    /**
     * Overdue Boss Heal:
     * If any slice in a chain is overdue (scheduledDateEpochDay < todayEpochDay and not completed),
     * adds a +10% Purple Shield per overdue slice to the Boss Bar that only breaks once the user
     * completes a shifted/overdue slice.
     */
    suspend fun syncOverdueBossShields(todayEpochDay: Long) {
        val overdueSlices = dao.getOverdueIncompleteSlicesOnce(todayEpochDay)
        val overdueByChain = overdueSlices.groupBy { it.chainId }
        val allChains = dao.getAllChainsOnce()

        for (chain in allChains) {
            val overdueCount = overdueByChain[chain.chainId]?.size ?: 0
            if (overdueCount > 0) {
                val requiredShield = (chain.bossMaxHp * 0.10f * overdueCount)
                    .roundToInt()
                    .coerceAtLeast(chain.bossMaxHp / 10)
                if (chain.bossBonusShieldHp < requiredShield) {
                    dao.updateChain(chain.copy(bossBonusShieldHp = requiredShield))
                }
            }
        }
    }

    /**
     * Sub-Counter Micro-Chips:
     * Increments problemsCompleted in Room, grants +10 XP, and updates Glance widget.
     */
    suspend fun incrementProblemCounter(sliceId: String): DailyTaskSlice? {
        val slice = dao.getSliceById(sliceId) ?: return null
        if (slice.isCompleted) return slice

        val nextCount = if (slice.problemsTarget > 0) {
            (slice.problemsCompleted + 1).coerceAtMost(slice.problemsTarget)
        } else {
            slice.problemsCompleted + 1
        }

        if (nextCount == slice.problemsCompleted) return slice

        val updatedSlice = slice.copy(problemsCompleted = nextCount)
        dao.updateSlice(updatedSlice)
        awardXp(10 * slice.xpMultiplier)
        ChronosWidgetUpdater.requestUpdate(appContext)
        return updatedSlice
    }

    /**
     * Allows decrementing problemsCompleted if tapped by mistake.
     */
    suspend fun decrementProblemCounter(sliceId: String): DailyTaskSlice? {
        val slice = dao.getSliceById(sliceId) ?: return null
        if (slice.isCompleted || slice.problemsCompleted <= 0) return slice

        val updatedSlice = slice.copy(problemsCompleted = (slice.problemsCompleted - 1).coerceAtLeast(0))
        dao.updateSlice(updatedSlice)
        ChronosWidgetUpdater.requestUpdate(appContext)
        return updatedSlice
    }

    /**
     * Swipe-to-Slash / 1-Tap Task Completion:
     * Marks slice as completed, breaks any Boss Purple Shield, grants XP,
     * and checks if this was the final slice of the chain to trigger Ebbinghaus Echo spawning.
     */
    suspend fun completeTaskSlice(sliceId: String): SliceCompletionResult? {
        val slice = dao.getSliceById(sliceId) ?: return null
        val chainWithSlices = dao.getChainWithSlices(slice.chainId) ?: return null
        val parentChain = chainWithSlices.chain

        if (slice.isCompleted) {
            return SliceCompletionResult(
                completedSlice = slice,
                parentChain = parentChain,
                xpEarned = 0,
                didConquerFinalCoreSlice = false
            )
        }

        val remainingProblemXp = max(0, slice.problemsTarget - slice.problemsCompleted) * 10
        val baseCompletionXp = 50
        val totalSliceXp = (baseCompletionXp + remainingProblemXp) * slice.xpMultiplier

        val completedSlice = slice.copy(
            problemsCompleted = max(slice.problemsCompleted, slice.problemsTarget),
            isCompleted = true,
            completedAtTimestamp = System.currentTimeMillis()
        )
        dao.updateSlice(completedSlice)

        val coreSlicesAfterUpdate = chainWithSlices.coreSlices.map {
            if (it.sliceId == slice.sliceId) completedSlice else it
        }
        val allCoreCompleted = coreSlicesAfterUpdate.isNotEmpty() && coreSlicesAfterUpdate.all { it.isCompleted }

        val didConquerFinalCoreSlice = !slice.isRetentionEcho &&
            (slice.sliceNumber == slice.totalSlicesInChain || allCoreCompleted)

        val updatedChain = parentChain.copy(
            bossBonusShieldHp = 0, // Completing a slice shatters the purple overdue shield
            isChainCompleted = allCoreCompleted
        )
        dao.updateChain(updatedChain)

        val bossBonusXp = if (allCoreCompleted && !parentChain.isChainCompleted) 100 else 0
        val totalAwardedXp = totalSliceXp + bossBonusXp
        awardXp(totalAwardedXp)

        ChronosWidgetUpdater.requestUpdate(appContext)

        return SliceCompletionResult(
            completedSlice = completedSlice,
            parentChain = updatedChain,
            xpEarned = totalAwardedXp,
            didConquerFinalCoreSlice = didConquerFinalCoreSlice
        )
    }

    /**
     * Spaced Repetition Auto-Spawner (Ebbinghaus Retention Echo):
     * Spawns 3 review slices at intervals determined by ConfidenceRating
     * (+2/+5/+14, +3/+7/+21, or +5/+14/+30) linked to the SAME chainId
     * so Cascade Delete wipes them cleanly if the chain is ever deleted.
     */
    suspend fun spawnEbbinghausEchoes(
        chainId: String,
        confidenceRating: ConfidenceRating,
        fromEpochDay: Long = LocalDate.now().toEpochDay()
    ): List<DailyTaskSlice> {
        val parent = dao.getChainById(chainId) ?: return emptyList()
        val paperPrefix = if (parent.subject.isSinglePaperOnly || parent.paper == HscPaper.SINGLE) {
            parent.subject.fullName
        } else {
            "${parent.subject.fullName} ${parent.paper.compactTitleLabel}"
        }
        val suffixTopic = if (parent.chapterName.isNotBlank()) ": ${parent.chapterName}" else ""

        val echoes = confidenceRating.intervalsInDays.mapIndexed { index, intervalDays ->
            DailyTaskSlice(
                sliceId = UUID.randomUUID().toString(),
                chainId = parent.chainId,
                sliceNumber = parent.totalDays + index + 1,
                totalSlicesInChain = parent.totalDays,
                displayTitle = "$paperPrefix - Ch ${parent.chapterNumber} Echo (+${intervalDays}d)$suffixTopic",
                shortBadge = "${parent.subject.shortCode}${parent.paper.shortCode} Ch ${parent.chapterNumber} ⟳+${intervalDays}d",
                phaseTag = "Retention Echo (+${intervalDays}d • 15m)",
                scheduledDateEpochDay = fromEpochDay + intervalDays.toLong(),
                problemsCompleted = 0,
                problemsTarget = 3,
                isCompleted = false,
                completedAtTimestamp = null,
                isRetentionEcho = true,
                echoIntervalDays = intervalDays,
                sliceEnergyOverride = EnergyLevel.ZOMBIE_MODE,
                xpMultiplier = 1
            )
        }

        dao.insertSlices(echoes)
        ChronosWidgetUpdater.requestUpdate(appContext)
        return echoes
    }

    /**
     * Zombie Mode (15% Battery) helper:
     * Converts a heavy HIGH_FOCUS task slice into a low-friction 15-minute light review for today.
     */
    suspend fun convertSliceToLightReview(sliceId: String) {
        val slice = dao.getSliceById(sliceId) ?: return
        val newTarget = min(2, max(1, slice.problemsTarget))
        val updated = slice.copy(
            sliceEnergyOverride = EnergyLevel.ZOMBIE_MODE,
            phaseTag = "Light Review (15m • Notes/Flashcards)",
            problemsTarget = newTarget,
            problemsCompleted = min(slice.problemsCompleted, newTarget)
        )
        dao.updateSlice(updated)
        ChronosWidgetUpdater.requestUpdate(appContext)
    }

    /**
     * Task Roulette 2x XP Multiplier:
     * Attaches a 2x XP multiplier when the user accepts the Roulette Micro-Sprint within 60 seconds.
     */
    suspend fun activateRouletteSprintMultiplier(sliceId: String) {
        val slice = dao.getSliceById(sliceId) ?: return
        if (slice.xpMultiplier < 2) {
            dao.updateSlice(slice.copy(xpMultiplier = 2))
            ChronosWidgetUpdater.requestUpdate(appContext)
        }
    }

    private suspend fun awardXp(xpDelta: Int) {
        if (xpDelta <= 0) return
        val today = LocalDate.now().toEpochDay()
        val current = dao.getUserStatsOnce() ?: UserStats(
            id = 1,
            totalXp = 0,
            currentLevel = 1,
            streakDays = 0,
            lastStudyEpochDay = null
        )
        val newXp = current.totalXp + xpDelta
        val newLevel = (newXp / 500) + 1
        val newStreak = when (current.lastStudyEpochDay) {
            null -> 1
            today -> max(1, current.streakDays)
            today - 1L -> current.streakDays + 1
            else -> 1
        }
        dao.upsertUserStats(
            current.copy(
                totalXp = newXp,
                currentLevel = newLevel,
                streakDays = newStreak,
                lastStudyEpochDay = today
            )
        )
    }

    companion object {
        fun formatSliceDisplayTitle(
            subject: HscSubject,
            paper: HscPaper,
            chapterNumber: Int,
            sliceNumber: Int,
            chapterName: String
        ): String {
            val subjectAndPaper = if (subject.isSinglePaperOnly || paper == HscPaper.SINGLE) {
                subject.fullName
            } else {
                "${subject.fullName} ${paper.compactTitleLabel}"
            }
            val cleanTopic = chapterName.trim()
            return if (cleanTopic.isNotEmpty()) {
                "$subjectAndPaper - Chapter $chapterNumber-$sliceNumber: $cleanTopic"
            } else {
                "$subjectAndPaper - Chapter $chapterNumber-$sliceNumber"
            }
        }

        fun formatShortBadge(
            subject: HscSubject,
            paper: HscPaper,
            chapterNumber: Int,
            sliceNumber: Int
        ): String {
            val codePrefix = if (subject.isSinglePaperOnly || paper == HscPaper.SINGLE) {
                subject.shortCode
            } else {
                "${subject.shortCode}${paper.shortCode}"
            }
            return "$codePrefix Ch $chapterNumber-$sliceNumber"
        }
    }
}
