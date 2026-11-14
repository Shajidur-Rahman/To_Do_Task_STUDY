package com.chronos.hsc.data.repository

import android.content.Context
import com.chronos.hsc.data.backup.ChronosFullBackupDto
import com.chronos.hsc.data.local.ConfidenceRating
import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.DistributionCurve
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.HscSyllabusCatalog
import com.chronos.hsc.data.local.NeuroDrillType
import com.chronos.hsc.data.local.NeuroSessionEntity
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.local.StudyDao
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.data.local.TopicChainWithSlices
import com.chronos.hsc.data.local.UserStats
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.BatchExamRecord
import com.chronos.hsc.data.sync.FirebaseSyncManager
import com.chronos.hsc.domain.DeletedChainSnapshot
import com.chronos.hsc.domain.DominoShiftResult
import com.chronos.hsc.domain.HeatmapDayCell
import com.chronos.hsc.domain.NeuroTelemetrySummary
import com.chronos.hsc.domain.SliceCompletionResult
import com.chronos.hsc.domain.SubjectAnalyticsSummary
import com.chronos.hsc.widget.ChronosWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class StudyRepository(
    private val dao: StudyDao,
    private val appContext: Context,
    private val firebaseSyncManager: FirebaseSyncManager? = null
) {

    private fun syncMgr(): FirebaseSyncManager? =
        firebaseSyncManager ?: FirebaseSyncManager.getInstanceIfInitialized()

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
        return combine(
            dao.observeAllChainsWithSlices(),
            CustomSubjectAndAiStore.customSubjectsFlow,
            CustomSubjectAndAiStore.chainLinksFlow
        ) { chainsWithSlices, customSubjects, _ ->
            val standardList = HscSubject.entries.map { subject ->
                val subjectChains = chainsWithSlices.filter {
                    it.chain.subject == subject && CustomSubjectAndAiStore.getCustomSubjectForChain(it.chain.chainId) == null
                }
                val subjectSlices = subjectChains.flatMap { it.slices }
                SubjectAnalyticsSummary(
                    subject = subject,
                    totalSlices = subjectSlices.size,
                    completedSlices = subjectSlices.count { it.isCompleted },
                    totalProblemsSolved = subjectSlices.sumOf { it.problemsCompleted },
                    activeChainsCount = subjectChains.count { !it.chain.isChainCompleted }
                )
            }
            val customList = customSubjects.map { cs ->
                val csChains = chainsWithSlices.filter {
                    CustomSubjectAndAiStore.getCustomSubjectForChain(it.chain.chainId)?.id == cs.id
                }
                val csSlices = csChains.flatMap { it.slices }
                SubjectAnalyticsSummary(
                    subject = HscSubject.ICT,
                    totalSlices = csSlices.size,
                    completedSlices = csSlices.count { it.isCompleted },
                    totalProblemsSolved = csSlices.sumOf { it.problemsCompleted },
                    activeChainsCount = csChains.count { !it.chain.isChainCompleted },
                    customSubjectName = cs.name,
                    customColorHex = cs.colorHex
                )
            }
            standardList + customList
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
        syncMgr()?.scheduleCloudPush()
        return exam
    }

    suspend fun pinExam(examId: String) {
        dao.pinExistingExam(examId)
        syncMgr()?.scheduleCloudPush()
    }

    suspend fun deleteExam(examId: String) {
        dao.deleteExam(examId)
        val remaining = dao.getAllExamsOnce()
        if (remaining.isNotEmpty() && remaining.none { it.isPrimaryPinned }) {
            dao.pinExistingExam(remaining.first().examId)
        }
        syncMgr()?.recordDeletedExamCountdownTombstone(examId)
        syncMgr()?.scheduleCloudPush()
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
        syncMgr()?.scheduleCloudPush()
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
        energyLevel: EnergyLevel,
        linkedPreviousChainId: String? = null,
        customSubjectId: String? = null
    ): StudyTopicChain {
        val effectivePaper = if (subject.isSinglePaperOnly) HscPaper.SINGLE else paper
        val safeTotalDays = totalDays.coerceIn(1, 15)
        val safeChapterNo = chapterNumber.coerceIn(1, 15)
        val cleanChapterName = chapterName.trim().ifEmpty {
            HscSyllabusCatalog.getChapterNameBangla(subject, effectivePaper, safeChapterNo)
        }
        val chainId = UUID.randomUUID().toString()
        val bossMaxHp = safeTotalDays * 100

        val customSub = customSubjectId?.let { CustomSubjectAndAiStore.getCustomSubject(it) }
        if (customSub != null) {
            CustomSubjectAndAiStore.linkChainToCustomSubject(chainId, customSub.id)
        }

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
            isChainCompleted = false,
            linkedPreviousChainId = linkedPreviousChainId
        )

        val slices = (1..safeTotalDays).map { dayIndex ->
            val sliceDateEpoch = startDateEpochDay + (dayIndex - 1).toLong()
            val displayTitleStr = if (customSub != null) {
                val paperSuffix = if (customSub.hasSecondPaper && effectivePaper != HscPaper.SINGLE) " ${effectivePaper.compactTitleLabel}" else ""
                val bnCh = HscSyllabusCatalog.toBanglaDigits(safeChapterNo)
                val bnSlice = HscSyllabusCatalog.toBanglaDigits(dayIndex)
                "${customSub.name}$paperSuffix • অধ্যায় $bnCh ($bnSlice): $cleanChapterName"
            } else {
                formatSliceDisplayTitle(
                    subject = subject,
                    paper = effectivePaper,
                    chapterNumber = safeChapterNo,
                    sliceNumber = dayIndex,
                    chapterName = cleanChapterName
                )
            }

            val shortBadgeStr = if (customSub != null) {
                val paperCode = if (customSub.hasSecondPaper && effectivePaper != HscPaper.SINGLE) effectivePaper.shortCode else ""
                "${customSub.shortBadge}$paperCode Ch $safeChapterNo-$dayIndex"
            } else {
                formatShortBadge(
                    subject = subject,
                    paper = effectivePaper,
                    chapterNumber = safeChapterNo,
                    sliceNumber = dayIndex
                )
            }

            DailyTaskSlice(
                sliceId = UUID.randomUUID().toString(),
                chainId = chainId,
                sliceNumber = dayIndex,
                totalSlicesInChain = safeTotalDays,
                displayTitle = displayTitleStr,
                shortBadge = shortBadgeStr,
                phaseTag = distributionCurve.phaseTagForDay(dayIndex, safeTotalDays),
                scheduledDateEpochDay = sliceDateEpoch,
                problemsCompleted = 0,
                problemsTarget = dailyProblemTarget.coerceIn(0, 30),
                isCompleted = false,
                completedAtTimestamp = null,
                isRetentionEcho = false,
                echoIntervalDays = null,
                sliceEnergyOverride = null,
                xpMultiplier = 1,
                originalScheduledEpochDay = sliceDateEpoch,
                shiftLagCount = 0
            )
        }

        dao.insertChainWithSlices(parentChain, slices)
        syncMgr()?.recordChainModified(parentChain.chainId)
        slices.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)
        return parentChain
    }

    suspend fun getChainById(chainId: String): StudyTopicChain? {
        return dao.getChainById(chainId)
    }

    suspend fun createExamPrepSlicesForRecord(
        exam: BatchExamRecord,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): StudyTopicChain {
        val daysUntilExam = (exam.examDateEpochDay - todayEpochDay).toInt()
        val totalDays = if (daysUntilExam <= 0) 1 else daysUntilExam.coerceIn(1, 14)

        // Parse chapter number
        val regex = Regex("""\b(\d+)\b""")
        val match = regex.find(exam.chapterNumbersAndTitles)
        val chapterNum = match?.value?.toIntOrNull() ?: 1

        // Match subject
        val customSub = CustomSubjectAndAiStore.getCustomSubjects().firstOrNull {
            it.name.equals(exam.subjectName, ignoreCase = true)
        }
        val hscSub = if (customSub != null) {
            customSub.baseSubject
        } else {
            HscSubject.entries.firstOrNull {
                it.fullName.equals(exam.subjectName, ignoreCase = true) ||
                it.name.equals(exam.subjectName, ignoreCase = true) ||
                it.shortCode.equals(exam.subjectName, ignoreCase = true)
            } ?: HscSubject.PHYSICS
        }

        // Paper
        val paper = when {
            exam.paperName.contains("2nd", ignoreCase = true) || exam.paperName.contains("2", ignoreCase = true) -> HscPaper.SECOND_PAPER
            exam.paperName.contains("1st", ignoreCase = true) || exam.paperName.contains("1", ignoreCase = true) -> HscPaper.FIRST_PAPER
            else -> HscPaper.SINGLE
        }

        val prepTitle = "[🎯 EXAM PREP: ${exam.formatSummary}] ${exam.chapterNumbersAndTitles}"
        val dailyProblems = (exam.mcqCount / totalDays).coerceIn(5, 30)

        return createStructuredTopicChain(
            subject = hscSub,
            paper = paper,
            chapterNumber = chapterNum,
            chapterName = prepTitle,
            totalDays = totalDays,
            startDateEpochDay = todayEpochDay,
            distributionCurve = DistributionCurve.CONCEPT_TO_PROBLEM,
            dailyProblemTarget = dailyProblems,
            energyLevel = EnergyLevel.HIGH_FOCUS,
            customSubjectId = customSub?.id
        )
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
        syncMgr()?.recordDeletedChainTombstone(chainId, current.slices.map { it.sliceId })
        CustomSubjectAndAiStore.removeChainLink(chainId)
        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)
        return snapshot
    }

    /**
     * Restores a cascade-deleted topic chain and all of its slices.
     */
    suspend fun restoreDeletedChainSnapshot(snapshot: DeletedChainSnapshot) {
        dao.insertChainWithSlices(snapshot.chain, snapshot.slices)
        syncMgr()?.removeChainTombstonesForRestore(
            chainId = snapshot.chain.chainId,
            sliceIds = snapshot.slices.map { it.sliceId }
        )
        syncMgr()?.recordChainModified(snapshot.chain.chainId)
        snapshot.slices.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
        syncMgr()?.scheduleCloudPush()
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
            syncMgr()?.recordDeletedSliceTombstone(sliceId)
            syncMgr()?.scheduleCloudPush()
            ChronosWidgetUpdater.requestUpdate(appContext)
            return 0
        }

        val remainingCoreSlices = chainWithSlices.coreSlices
            .filter { it.sliceId != sliceId }
            .sortedBy { it.sliceNumber }

        if (remainingCoreSlices.isEmpty() && chainWithSlices.echoSlices.isEmpty()) {
            dao.deleteTopicChainById(parent.chainId)
            syncMgr()?.recordDeletedChainTombstone(parent.chainId, listOf(sliceId))
            CustomSubjectAndAiStore.removeChainLink(parent.chainId)
            syncMgr()?.scheduleCloudPush()
            ChronosWidgetUpdater.requestUpdate(appContext)
            return 0
        }

        val subsequentSlicesCount = remainingCoreSlices.count { it.sliceNumber > targetSlice.sliceNumber }
        dao.deleteSliceById(sliceId)
        syncMgr()?.recordDeletedSliceTombstone(sliceId)

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
            reindexedSlices.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
        }

        val allRemainingDone = reindexedSlices.isNotEmpty() && reindexedSlices.all { it.isCompleted }
        dao.updateChain(
            parent.copy(
                totalDays = newTotalDays,
                bossMaxHp = newTotalDays * 100,
                isChainCompleted = allRemainingDone
            )
        )
        syncMgr()?.recordChainModified(parent.chainId)
        syncMgr()?.scheduleCloudPush()
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
                            slice.copy(
                                scheduledDateEpochDay = resolvedDate,
                                shiftLagCount = slice.shiftLagCount + delta.toInt()
                            )
                        }
                        dao.updateSlices(shiftedCore)
                        shiftedCore.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
                        totalSlicesShifted += shiftedCore.size
                        totalSlicesShifted += shiftDownstreamLinkedChains(chainId, delta)
                    }
                }
            }

            // Also shift any overdue retention echoes to today
            val overdueEchoes = incompleteChainSlices.filter {
                it.isRetentionEcho && it.scheduledDateEpochDay < todayEpochDay
            }
            if (overdueEchoes.isNotEmpty()) {
                val shiftedEchoes = overdueEchoes.map {
                    val echoDelta = (todayEpochDay - it.scheduledDateEpochDay).toInt().coerceAtLeast(1)
                    it.copy(
                        scheduledDateEpochDay = todayEpochDay,
                        shiftLagCount = it.shiftLagCount + echoDelta
                    )
                }
                dao.updateSlices(shiftedEchoes)
                shiftedEchoes.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
                totalSlicesShifted += overdueEchoes.size
            }
        }

        syncOverdueBossShields(todayEpochDay)
        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)
        return DominoShiftResult(
            shiftedChainsCount = affectedChainIds.size,
            shiftedSlicesCount = totalSlicesShifted
        )
    }

    /**
     * Recursively shifts all downstream chains linked to [chainId] forward by [deltaDays].
     * Ensures that if Chain B is linked to Chain A, whenever Chain A shifts (+1d or overdue jump),
     * Chain B and all downstream linked chains shift forward together, preserving the
     * whole syllabus pipeline without overlapping.
     */
    suspend fun shiftDownstreamLinkedChains(chainId: String, deltaDays: Long): Int {
        if (deltaDays <= 0L) return 0
        var totalShifted = 0
        val downstreamChains = dao.getDownstreamLinkedChains(chainId)
        for (downstream in downstreamChains) {
            val incompleteSlices = dao.getIncompleteSlicesForChain(downstream.chainId)
            if (incompleteSlices.isNotEmpty()) {
                val shiftedSlices = incompleteSlices.map { slice ->
                    slice.copy(
                        scheduledDateEpochDay = slice.scheduledDateEpochDay + deltaDays,
                        shiftLagCount = slice.shiftLagCount + deltaDays.toInt()
                    )
                }
                dao.updateSlices(shiftedSlices)
                shiftedSlices.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
                totalShifted += shiftedSlices.size
            }
            // Recurse to any chains linked to this downstream chain
            totalShifted += shiftDownstreamLinkedChains(downstream.chainId, deltaDays)
        }
        return totalShifted
    }

    /**
     * Per-Card "Push Domino (+1 Day)":
     * Shifts the selected slice and all subsequent incomplete slices in that chain forward by +1 day.
     * Also pushes any downstream linked chapter chains forward by +1 day.
     */
    suspend fun pushSliceDominoPlusOneDay(sliceId: String): Int {
        val targetSlice = dao.getSliceById(sliceId) ?: return 0
        if (targetSlice.isRetentionEcho) {
            dao.updateSlice(
                targetSlice.copy(
                    scheduledDateEpochDay = targetSlice.scheduledDateEpochDay + 1L,
                    shiftLagCount = targetSlice.shiftLagCount + 1
                )
            )
            syncMgr()?.recordSliceModified(targetSlice.sliceId)
            syncOverdueBossShields(LocalDate.now().toEpochDay())
            syncMgr()?.scheduleCloudPush()
            ChronosWidgetUpdater.requestUpdate(appContext)
            return 1
        }

        val incompleteChainSlices = dao.getIncompleteSlicesForChain(targetSlice.chainId)
            .filter { !it.isRetentionEcho && it.sliceNumber >= targetSlice.sliceNumber }
            .sortedBy { it.sliceNumber }

        if (incompleteChainSlices.isEmpty()) return 0

        val updated = incompleteChainSlices.map { slice ->
            slice.copy(
                scheduledDateEpochDay = slice.scheduledDateEpochDay + 1L,
                shiftLagCount = slice.shiftLagCount + 1
            )
        }
        dao.updateSlices(updated)
        updated.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
        val downstreamShifted = shiftDownstreamLinkedChains(targetSlice.chainId, 1L)
        syncOverdueBossShields(LocalDate.now().toEpochDay())
        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)
        return updated.size + downstreamShifted
    }

    /**
     * Instant Catch-Up Engine:
     * Pulls the specified slice (and subsequent incomplete slices in the same chain)
     * forward to start from today, re-aligning the schedule and clearing lag debt.
     */
    suspend fun catchUpSliceToToday(sliceId: String, todayEpochDay: Long): Int {
        val targetSlice = dao.getSliceById(sliceId) ?: return 0
        val chainId = targetSlice.chainId
        val incomplete = dao.getIncompleteSlicesForChain(chainId)
            .filter { !it.isRetentionEcho }
            .sortedBy { it.sliceNumber }

        if (incomplete.isEmpty()) return 0

        val targetIndex = incomplete.indexOfFirst { it.sliceId == sliceId }
        val toReschedule = if (targetIndex >= 0) incomplete.subList(targetIndex, incomplete.size) else incomplete

        var currentDate = todayEpochDay
        val delta = (todayEpochDay - targetSlice.scheduledDateEpochDay).coerceAtLeast(0L)

        val updated = toReschedule.map { slice ->
            val newSlice = slice.copy(
                scheduledDateEpochDay = currentDate,
                originalScheduledEpochDay = currentDate,
                shiftLagCount = 0
            )
            currentDate += 1L
            newSlice
        }

        dao.updateSlices(updated)
        updated.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
        if (delta > 0L) {
            shiftDownstreamLinkedChains(chainId, delta)
        }
        syncOverdueBossShields(todayEpochDay)
        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)
        return updated.size
    }

    /**
     * Instant Catch-Up All Chains:
     * Re-baselines all lagging/overdue chains starting today so the student
     * can start fresh without overdue backlog anxiety, maintaining sequential non-overlapping order
     * for linked chapter chains.
     */
    suspend fun instantCatchUpAllLaggingChains(todayEpochDay: Long): Int {
        val allIncomplete = dao.getAllIncompleteSlicesOnce()
            .filter { !it.isRetentionEcho && (it.scheduledDateEpochDay < todayEpochDay || it.shiftLagCount > 0) }

        if (allIncomplete.isEmpty()) return 0

        val affectedChainIds = allIncomplete.map { it.chainId }.distinct()
        var count = 0
        val chainFinishDates = mutableMapOf<String, Long>()

        for (chainId in affectedChainIds) {
            val incompleteCore = dao.getIncompleteSlicesForChain(chainId)
                .filter { !it.isRetentionEcho }
                .sortedBy { it.sliceNumber }

            if (incompleteCore.isEmpty()) continue

            val parentChain = dao.getChainById(chainId)
            val parentFinish = parentChain?.linkedPreviousChainId?.let { chainFinishDates[it] }
            var currentDate = if (parentFinish != null) maxOf(todayEpochDay, parentFinish + 1L) else todayEpochDay

            val updated = incompleteCore.map { slice ->
                val newSlice = slice.copy(
                    scheduledDateEpochDay = currentDate,
                    originalScheduledEpochDay = currentDate,
                    shiftLagCount = 0
                )
                currentDate += 1L
                newSlice
            }
            dao.updateSlices(updated)
            updated.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
            chainFinishDates[chainId] = currentDate - 1L
            count += updated.size
        }

        syncOverdueBossShields(todayEpochDay)
        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)
        return count
    }

    /**
     * Overdue Boss Heal:
     * If any slice in a chain is overdue (scheduledDateEpochDay < todayEpochDay and not completed),
     * adds a +10% Purple Shield per overdue slice to the Boss Bar, and resets to 0 once shifted or cleared.
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
                if (chain.bossBonusShieldHp != requiredShield) {
                    dao.updateChain(chain.copy(bossBonusShieldHp = requiredShield))
                    syncMgr()?.recordChainModified(chain.chainId)
                }
            } else if (chain.bossBonusShieldHp != 0) {
                dao.updateChain(chain.copy(bossBonusShieldHp = 0))
                syncMgr()?.recordChainModified(chain.chainId)
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
        syncMgr()?.recordSliceModified(sliceId)
        awardXp(10 * slice.xpMultiplier)
        syncMgr()?.scheduleCloudPush()
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
        syncMgr()?.recordSliceModified(sliceId)
        syncMgr()?.scheduleCloudPush()
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
        syncMgr()?.recordSliceModified(sliceId)

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
        syncMgr()?.recordChainModified(parentChain.chainId)

        val bossBonusXp = if (allCoreCompleted && !parentChain.isChainCompleted) 100 else 0
        val totalAwardedXp = totalSliceXp + bossBonusXp
        awardXp(totalAwardedXp)

        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)

        return SliceCompletionResult(
            completedSlice = completedSlice,
            parentChain = updatedChain,
            xpEarned = totalAwardedXp,
            didConquerFinalCoreSlice = didConquerFinalCoreSlice
        )
    }

    /**
     * Slide-to-Left / Revert Task Completion (Undone):
     * Restores a completed slice back to incomplete, resets completion timestamp,
     * adjusts user XP (-50 XP), and re-opens parent chain if needed.
     */
    suspend fun uncompleteTaskSlice(sliceId: String): DailyTaskSlice? {
        val slice = dao.getSliceById(sliceId) ?: return null
        if (!slice.isCompleted) return slice

        val uncompletedSlice = slice.copy(
            isCompleted = false,
            completedAtTimestamp = null
        )
        dao.updateSlice(uncompletedSlice)
        syncMgr()?.recordSliceModified(sliceId)

        val chainWithSlices = dao.getChainWithSlices(slice.chainId)
        if (chainWithSlices != null) {
            val parentChain = chainWithSlices.chain
            val hasIncompleteCore = chainWithSlices.coreSlices.any { !it.isCompleted || it.sliceId == sliceId }
            if (hasIncompleteCore && parentChain.isChainCompleted) {
                dao.updateChain(parentChain.copy(isChainCompleted = false))
                syncMgr()?.recordChainModified(parentChain.chainId)
            }
        }

        // Deduct base slice XP
        val baseCompletionXp = 50 * slice.xpMultiplier
        awardXp(-baseCompletionXp)

        syncMgr()?.scheduleCloudPush()
        ChronosWidgetUpdater.requestUpdate(appContext)
        return uncompletedSlice
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
        val resolvedTopic = parent.chapterName.trim().ifEmpty {
            HscSyllabusCatalog.getChapterNameBangla(parent.subject, parent.paper, parent.chapterNumber)
        }
        val bnCh = HscSyllabusCatalog.toBanglaDigits(parent.chapterNumber)

        val echoes = confidenceRating.intervalsInDays.mapIndexed { index, intervalDays ->
            DailyTaskSlice(
                sliceId = UUID.randomUUID().toString(),
                chainId = parent.chainId,
                sliceNumber = parent.totalDays + index + 1,
                totalSlicesInChain = parent.totalDays,
                displayTitle = "$paperPrefix • অধ্যায় $bnCh: $resolvedTopic (Echo +${intervalDays}d)",
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
        echoes.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
        syncMgr()?.scheduleCloudPush()
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
        syncMgr()?.recordSliceModified(sliceId)
        syncMgr()?.scheduleCloudPush()
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
            syncMgr()?.recordSliceModified(sliceId)
            syncMgr()?.scheduleCloudPush()
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
        syncMgr()?.scheduleCloudPush()
    }

    // =========================================================================
    // DATABASE BACKUP & RESTORE
    // =========================================================================

    suspend fun createFullBackupDto(): ChronosFullBackupDto = withContext(Dispatchers.IO) {
        val userStats = dao.getUserStatsOnce() ?: UserStats()
        val chains = dao.getAllChainsOnce()
        val slices = dao.getAllSlicesOnce()
        val exams = dao.getAllExamsOnce()
        val neuroSessions = dao.getAllNeuroSessionsOnce()

        ChronosFullBackupDto(
            backupVersion = 1,
            exportTimestamp = System.currentTimeMillis(),
            userStats = userStats,
            chains = chains,
            slices = slices,
            exams = exams,
            neuroSessions = neuroSessions
        )
    }

    suspend fun exportBackupToJsonStream(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backupDto = createFullBackupDto()
            val jsonString = backupDto.toJsonString()
            outputStream.bufferedWriter().use { writer ->
                writer.write(jsonString)
                writer.flush()
            }
            Result.success(backupDto.slices.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromJsonStream(inputStream: InputStream): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val backupDto = ChronosFullBackupDto.fromJsonString(jsonString)

            dao.restoreFullDatabase(
                userStats = backupDto.userStats,
                chains = backupDto.chains,
                slices = backupDto.slices,
                exams = backupDto.exams,
                neuroSessions = backupDto.neuroSessions
            )

            backupDto.chains.forEach { syncMgr()?.recordChainModified(it.chainId) }
            backupDto.slices.forEach { syncMgr()?.recordSliceModified(it.sliceId) }
            syncMgr()?.scheduleCloudPush()
            ChronosWidgetUpdater.requestUpdate(appContext)
            Result.success("Restored ${backupDto.chains.size} chapters and ${backupDto.slices.size} slices cleanly!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTodaySlicesSnapshot(todayEpochDay: Long): List<SliceWithParentChain> {
        return dao.observeSlicesForDate(todayEpochDay).first()
    }

    suspend fun getOverdueSlicesSnapshot(todayEpochDay: Long): List<SliceWithParentChain> {
        return dao.observeOverdueSlices(todayEpochDay).first()
    }

    suspend fun getPinnedExamSnapshot(): ExamCountdownEntity? {
        return dao.observePinnedExam().first()
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
            val cleanTopic = chapterName.trim().ifEmpty {
                HscSyllabusCatalog.getChapterNameBangla(subject, paper, chapterNumber)
            }
            val bnCh = HscSyllabusCatalog.toBanglaDigits(chapterNumber)
            val bnSlice = HscSyllabusCatalog.toBanglaDigits(sliceNumber)
            return "$subjectAndPaper • অধ্যায় $bnCh ($bnSlice): $cleanTopic"
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
