package com.chronos.hsc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chronos.hsc.data.local.ConfidenceRating
import com.chronos.hsc.data.local.DistributionCurve
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.NeuroDrillType
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.data.local.TopicChainWithSlices
import com.chronos.hsc.data.local.UserStats
import com.chronos.hsc.data.repository.StudyRepository
import com.chronos.hsc.domain.DeletedChainSnapshot
import com.chronos.hsc.domain.HeatmapDayCell
import com.chronos.hsc.domain.NeuroTelemetrySummary
import com.chronos.hsc.domain.SubjectAnalyticsSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.chronos.hsc.notification.ChronosWorkScheduler

data class SprintTimerState(
    val targetSliceId: String,
    val targetSliceTitle: String,
    val targetShortBadge: String,
    val acceptanceSecondsRemaining: Int = 60,
    val isSprintRunning: Boolean = false,
    val isSprintPaused: Boolean = false,
    val sprintSecondsRemaining: Int = 25 * 60,
    val has2xMultiplier: Boolean = false
)

sealed interface StudyUiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val isUndoCascadeDelete: Boolean = false
    ) : StudyUiEvent
}

data class StudyUiState(
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
    val selectedDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val batteryMode: EnergyLevel = EnergyLevel.HIGH_FOCUS,
    val userStats: UserStats = UserStats(),
    val visibleDateSlices: List<SliceWithParentChain> = emptyList(),
    val hiddenZombieSlices: List<SliceWithParentChain> = emptyList(),
    val rawDateSlices: List<SliceWithParentChain> = emptyList(),
    val overdueSlices: List<SliceWithParentChain> = emptyList(),
    val overdueChainsCount: Int = 0,
    val allChainsWithSlices: List<TopicChainWithSlices> = emptyList(),
    val heatmapCells: List<HeatmapDayCell> = emptyList(),
    val subjectAnalytics: List<SubjectAnalyticsSummary> = emptyList(),
    val upcomingEchoes: List<SliceWithParentChain> = emptyList(),
    val pinnedExam: ExamCountdownEntity? = null,
    val allExams: List<ExamCountdownEntity> = emptyList(),
    val neuroTelemetry: NeuroTelemetrySummary = NeuroTelemetrySummary(),
    val isAddTopicModalOpen: Boolean = false,
    val isExamModalOpen: Boolean = false,
    val isAccountabilityModalOpen: Boolean = false,
    val morningBriefingEnabled: Boolean = true,
    val dominoAlertsEnabled: Boolean = true,
    val slicePendingDelete: SliceWithParentChain? = null,
    val chainPendingDelete: TopicChainWithSlices? = null,
    val conqueredChainForEchoDialog: StudyTopicChain? = null,
    val isTaskRouletteOpen: Boolean = false,
    val sprintTimerState: SprintTimerState? = null
)

private data class ModalControlState(
    val isAddTopicModalOpen: Boolean = false,
    val isExamModalOpen: Boolean = false,
    val isAccountabilityModalOpen: Boolean = false,
    val morningBriefingEnabled: Boolean = true,
    val dominoAlertsEnabled: Boolean = true,
    val slicePendingDelete: SliceWithParentChain? = null,
    val chainPendingDelete: TopicChainWithSlices? = null,
    val conqueredChainForEchoDialog: StudyTopicChain? = null,
    val isTaskRouletteOpen: Boolean = false,
    val sprintTimerState: SprintTimerState? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModel(
    private val repository: StudyRepository
) : ViewModel() {

    private val todayEpochDay = LocalDate.now().toEpochDay()
    private val selectedDateFlow = MutableStateFlow(todayEpochDay)
    private val batteryModeFlow = MutableStateFlow(EnergyLevel.HIGH_FOCUS)
    private val modalControlFlow = MutableStateFlow(ModalControlState())

    private val _events = MutableSharedFlow<StudyUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<StudyUiEvent> = _events.asSharedFlow()

    private var lastDeletedSnapshot: DeletedChainSnapshot? = null
    private var sprintCountdownJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeOnLaunch()
        }
    }

    private val slicesForSelectedDateFlow = selectedDateFlow.flatMapLatest { epochDay ->
        repository.observeSlicesForDate(epochDay)
    }

    private val coreDataFlow = combine(
        selectedDateFlow,
        batteryModeFlow,
        repository.observeUserStats(),
        slicesForSelectedDateFlow,
        repository.observeOverdueSlices(todayEpochDay)
    ) { selectedDate, batteryMode, userStats, dateSlices, overdueSlices ->
        CoreDashboardData(
            selectedDateEpochDay = selectedDate,
            batteryMode = batteryMode,
            userStats = userStats,
            dateSlices = dateSlices,
            overdueSlices = overdueSlices
        )
    }

    private val analyticsAndChainsFlow = combine(
        repository.observeAllChainsWithSlices(),
        repository.observeHeatmapCells(todayEpochDay),
        repository.observeSubjectAnalytics(),
        repository.observeUpcomingEchoes(todayEpochDay)
    ) { allChains, heatmapCells, subjectAnalytics, upcomingEchoes ->
        AnalyticsAndChainsData(
            allChains = allChains,
            heatmapCells = heatmapCells,
            subjectAnalytics = subjectAnalytics,
            upcomingEchoes = upcomingEchoes
        )
    }

    private val examAndNeuroFlow = combine(
        repository.observePinnedExam(),
        repository.observeAllExams(),
        repository.observeNeuroTelemetry()
    ) { pinnedExam, allExams, neuroTelemetry ->
        ExamAndNeuroData(
            pinnedExam = pinnedExam,
            allExams = allExams,
            neuroTelemetry = neuroTelemetry
        )
    }

    val uiState: StateFlow<StudyUiState> = combine(
        coreDataFlow,
        analyticsAndChainsFlow,
        examAndNeuroFlow,
        modalControlFlow
    ) { core, analytics, examAndNeuro, modals ->
        val (visible, hidden) = filterAndSortByBatteryMode(
            slices = core.dateSlices,
            batteryMode = core.batteryMode
        )
        StudyUiState(
            todayEpochDay = todayEpochDay,
            selectedDateEpochDay = core.selectedDateEpochDay,
            batteryMode = core.batteryMode,
            userStats = core.userStats,
            visibleDateSlices = visible,
            hiddenZombieSlices = hidden,
            rawDateSlices = core.dateSlices,
            overdueSlices = core.overdueSlices,
            overdueChainsCount = core.overdueSlices.map { it.chain.chainId }.distinct().size,
            allChainsWithSlices = analytics.allChains,
            heatmapCells = analytics.heatmapCells,
            subjectAnalytics = analytics.subjectAnalytics,
            upcomingEchoes = analytics.upcomingEchoes,
            pinnedExam = examAndNeuro.pinnedExam,
            allExams = examAndNeuro.allExams,
            neuroTelemetry = examAndNeuro.neuroTelemetry,
            isAddTopicModalOpen = modals.isAddTopicModalOpen,
            isExamModalOpen = modals.isExamModalOpen,
            isAccountabilityModalOpen = modals.isAccountabilityModalOpen,
            morningBriefingEnabled = modals.morningBriefingEnabled,
            dominoAlertsEnabled = modals.dominoAlertsEnabled,
            slicePendingDelete = modals.slicePendingDelete,
            chainPendingDelete = modals.chainPendingDelete,
            conqueredChainForEchoDialog = modals.conqueredChainForEchoDialog,
            isTaskRouletteOpen = modals.isTaskRouletteOpen,
            sprintTimerState = modals.sprintTimerState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudyUiState()
    )

    private fun filterAndSortByBatteryMode(
        slices: List<SliceWithParentChain>,
        batteryMode: EnergyLevel
    ): Pair<List<SliceWithParentChain>, List<SliceWithParentChain>> {
        return when (batteryMode) {
            EnergyLevel.HIGH_FOCUS -> {
                val sorted = slices.sortedWith(
                    compareBy<SliceWithParentChain> { it.slice.isCompleted }
                        .thenByDescending { it.effectiveEnergyLevel.sortWeight }
                        .thenBy { it.slice.isRetentionEcho }
                        .thenBy { it.slice.sliceNumber }
                )
                sorted to emptyList()
            }

            EnergyLevel.MEDIUM -> {
                val sorted = slices.sortedWith(
                    compareBy<SliceWithParentChain> { it.slice.isCompleted }
                        .thenBy { absWeightDistanceFromMedium(it.effectiveEnergyLevel) }
                        .thenBy { it.slice.sliceNumber }
                )
                sorted to emptyList()
            }

            EnergyLevel.ZOMBIE_MODE -> {
                val visible = mutableListOf<SliceWithParentChain>()
                val hidden = mutableListOf<SliceWithParentChain>()
                for (item in slices) {
                    if (item.slice.isCompleted || item.isLightReviewEligibleInZombieMode) {
                        visible.add(item)
                    } else {
                        hidden.add(item)
                    }
                }
                val sortedVisible = visible.sortedWith(
                    compareBy<SliceWithParentChain> { it.slice.isCompleted }
                        .thenBy { it.slice.sliceNumber }
                )
                sortedVisible to hidden
            }
        }
    }

    private fun absWeightDistanceFromMedium(level: EnergyLevel): Int =
        when (level) {
            EnergyLevel.MEDIUM -> 0
            EnergyLevel.ZOMBIE_MODE -> 1
            EnergyLevel.HIGH_FOCUS -> 2
        }

    fun selectDate(epochDay: Long) {
        selectedDateFlow.value = epochDay
    }

    fun selectBatteryMode(mode: EnergyLevel) {
        batteryModeFlow.value = mode
    }

    fun openAddTopicModal() {
        modalControlFlow.update { it.copy(isAddTopicModalOpen = true) }
    }

    fun closeAddTopicModal() {
        modalControlFlow.update { it.copy(isAddTopicModalOpen = false) }
    }

    fun createStructuredTopicChain(
        subject: HscSubject,
        paper: HscPaper,
        chapterNumber: Int,
        chapterName: String,
        totalDays: Int,
        startDateEpochDay: Long,
        distributionCurve: DistributionCurve,
        dailyProblemTarget: Int,
        energyLevel: EnergyLevel
    ) {
        viewModelScope.launch {
            val createdChain = repository.createStructuredTopicChain(
                subject = subject,
                paper = paper,
                chapterNumber = chapterNumber,
                chapterName = chapterName,
                totalDays = totalDays,
                startDateEpochDay = startDateEpochDay,
                distributionCurve = distributionCurve,
                dailyProblemTarget = dailyProblemTarget,
                energyLevel = energyLevel
            )
            modalControlFlow.update { it.copy(isAddTopicModalOpen = false) }
            selectedDateFlow.value = startDateEpochDay
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "Generated ${createdChain.totalDays}-day study chain for ${createdChain.subject.fullName} Ch ${createdChain.chapterNumber}!"
                )
            )
        }
    }

    fun requestDeleteForSlice(sliceWithChain: SliceWithParentChain) {
        modalControlFlow.update {
            it.copy(slicePendingDelete = sliceWithChain, chainPendingDelete = null)
        }
    }

    fun requestDeleteForEntireChain(chainWithSlices: TopicChainWithSlices) {
        modalControlFlow.update {
            it.copy(chainPendingDelete = chainWithSlices, slicePendingDelete = null)
        }
    }

    fun dismissDeleteDialog() {
        modalControlFlow.update {
            it.copy(slicePendingDelete = null, chainPendingDelete = null)
        }
    }

    fun confirmCascadeDeleteEntireChain(chainId: String) {
        viewModelScope.launch {
            modalControlFlow.update {
                it.copy(slicePendingDelete = null, chainPendingDelete = null)
            }
            val snapshot = repository.deleteEntireTopicChainWithSnapshot(chainId)
            if (snapshot != null) {
                lastDeletedSnapshot = snapshot
                val sliceCount = snapshot.slices.size
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "Deleted entire chain ($sliceCount slices removed across all dates)",
                        actionLabel = "Undo",
                        isUndoCascadeDelete = true
                    )
                )
            }
        }
    }

    fun undoLastCascadeDelete() {
        val snapshot = lastDeletedSnapshot ?: return
        viewModelScope.launch {
            repository.restoreDeletedChainSnapshot(snapshot)
            lastDeletedSnapshot = null
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "Restored ${snapshot.chain.subject.fullName} Ch ${snapshot.chain.chapterNumber} (${snapshot.slices.size} slices)"
                )
            )
        }
    }

    fun confirmDeleteSingleSliceAndReindex(sliceId: String) {
        viewModelScope.launch {
            modalControlFlow.update {
                it.copy(slicePendingDelete = null, chainPendingDelete = null)
            }
            val reindexedCount = repository.deleteSingleSliceAndReindex(sliceId)
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "Deleted single slice & pulled $reindexedCount subsequent slice(s) back by 1 day"
                )
            )
        }
    }

    fun executeGlobalDominoShift() {
        viewModelScope.launch {
            val result = repository.executeDominoShift(todayEpochDay)
            if (result.shiftedSlicesCount > 0) {
                selectedDateFlow.value = todayEpochDay
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "Domino Shift complete: Shifted ${result.shiftedSlicesCount} slices across ${result.shiftedChainsCount} chain(s) cleanly!"
                    )
                )
            }
        }
    }

    fun pushSliceDominoPlusOneDay(sliceWithChain: SliceWithParentChain) {
        viewModelScope.launch {
            val shiftedCount = repository.pushSliceDominoPlusOneDay(sliceWithChain.slice.sliceId)
            if (shiftedCount > 0) {
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "Domino Push (+1d): Shifted ${sliceWithChain.slice.shortBadge} and ${shiftedCount - 1} subsequent slice(s) forward!"
                    )
                )
            }
        }
    }

    fun catchUpSliceToToday(sliceId: String) {
        viewModelScope.launch {
            val count = repository.catchUpSliceToToday(sliceId, todayEpochDay)
            if (count > 0) {
                selectedDateFlow.value = todayEpochDay
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "Instant Catch-Up: Re-aligned $count slice(s) starting from today!"
                    )
                )
            }
        }
    }

    fun instantCatchUpAllLaggingChains() {
        viewModelScope.launch {
            val count = repository.instantCatchUpAllLaggingChains(todayEpochDay)
            if (count > 0) {
                selectedDateFlow.value = todayEpochDay
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "Instant Catch-Up: Re-baselined $count lagging slice(s) to today! Fresh start activated."
                    )
                )
            } else {
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "All caught up! No lagging chains found."
                    )
                )
            }
        }
    }

    fun incrementProblemSubCounter(sliceId: String) {
        viewModelScope.launch {
            repository.incrementProblemCounter(sliceId)
        }
    }

    fun decrementProblemSubCounter(sliceId: String) {
        viewModelScope.launch {
            repository.decrementProblemCounter(sliceId)
        }
    }

    fun completeTaskSlice(sliceId: String) {
        viewModelScope.launch {
            val result = repository.completeTaskSlice(sliceId) ?: return@launch
            val activeSprint = modalControlFlow.value.sprintTimerState
            if (activeSprint?.targetSliceId == sliceId) {
                stopSprintTimer()
            }

            if (result.didConquerFinalCoreSlice) {
                modalControlFlow.update {
                    it.copy(conqueredChainForEchoDialog = result.parentChain)
                }
            } else {
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "Slashed ${result.completedSlice.shortBadge}! +${result.xpEarned} XP"
                    )
                )
            }
        }
    }

    fun dismissEbbinghausEchoDialog(
        spawnEchoes: Boolean,
        confidenceRating: ConfidenceRating
    ) {
        val conqueredChain = modalControlFlow.value.conqueredChainForEchoDialog
        modalControlFlow.update { it.copy(conqueredChainForEchoDialog = null) }
        if (conqueredChain != null && spawnEchoes) {
            viewModelScope.launch {
                val spawned = repository.spawnEbbinghausEchoes(
                    chainId = conqueredChain.chainId,
                    confidenceRating = confidenceRating,
                    fromEpochDay = todayEpochDay
                )
                val intervalsText = confidenceRating.intervalsInDays.joinToString(", ") { "+${it}d" }
                _events.emit(
                    StudyUiEvent.ShowSnackbar(
                        message = "Spawned ${spawned.size} Ebbinghaus Retention Echoes ($intervalsText)!"
                    )
                )
            }
        }
    }

    fun convertHiddenSliceToLightReview(sliceId: String) {
        viewModelScope.launch {
            repository.convertSliceToLightReview(sliceId)
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "Converted task into a 15-minute Light Review for Zombie Mode!"
                )
            )
        }
    }

    fun openTaskRoulette() {
        modalControlFlow.update { it.copy(isTaskRouletteOpen = true) }
    }

    fun closeTaskRoulette() {
        modalControlFlow.update { it.copy(isTaskRouletteOpen = false) }
    }

    fun onRouletteLockedSlice(slice: SliceWithParentChain) {
        sprintCountdownJob?.cancel()
        modalControlFlow.update {
            it.copy(
                sprintTimerState = SprintTimerState(
                    targetSliceId = slice.slice.sliceId,
                    targetSliceTitle = slice.slice.displayTitle,
                    targetShortBadge = slice.slice.shortBadge,
                    acceptanceSecondsRemaining = 60,
                    isSprintRunning = false,
                    isSprintPaused = false,
                    sprintSecondsRemaining = 25 * 60,
                    has2xMultiplier = false
                )
            )
        }
        sprintCountdownJob = viewModelScope.launch {
            var remaining = 60
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                val currentSprint = modalControlFlow.value.sprintTimerState ?: break
                if (currentSprint.isSprintRunning) break
                modalControlFlow.update { state ->
                    state.copy(
                        sprintTimerState = state.sprintTimerState?.copy(
                            acceptanceSecondsRemaining = remaining
                        )
                    )
                }
            }
        }
    }

    fun acceptAndStartMicroSprint() {
        val currentSprint = modalControlFlow.value.sprintTimerState ?: return
        val acceptedWithinWindow = currentSprint.acceptanceSecondsRemaining > 0
        sprintCountdownJob?.cancel()

        sprintCountdownJob = viewModelScope.launch {
            if (acceptedWithinWindow) {
                repository.activateRouletteSprintMultiplier(currentSprint.targetSliceId)
            }
            modalControlFlow.update { state ->
                state.copy(
                    sprintTimerState = currentSprint.copy(
                        isSprintRunning = true,
                        isSprintPaused = false,
                        has2xMultiplier = acceptedWithinWindow,
                        sprintSecondsRemaining = 25 * 60
                    )
                )
            }
            while (true) {
                delay(1_000L)
                val timer = modalControlFlow.value.sprintTimerState ?: break
                if (!timer.isSprintRunning) break
                if (timer.isSprintPaused) continue
                val nextSeconds = (timer.sprintSecondsRemaining - 1).coerceAtLeast(0)
                modalControlFlow.update { state ->
                    state.copy(
                        sprintTimerState = state.sprintTimerState?.copy(
                            sprintSecondsRemaining = nextSeconds
                        )
                    )
                }
                if (nextSeconds <= 0) break
            }
        }
    }

    fun togglePauseSprintTimer() {
        modalControlFlow.update { state ->
            val current = state.sprintTimerState ?: return@update state
            state.copy(
                sprintTimerState = current.copy(isSprintPaused = !current.isSprintPaused)
            )
        }
    }

    fun stopSprintTimer() {
        sprintCountdownJob?.cancel()
        modalControlFlow.update { it.copy(sprintTimerState = null) }
    }

    fun openExamCountdownModal() {
        modalControlFlow.update { it.copy(isExamModalOpen = true) }
    }

    fun closeExamCountdownModal() {
        modalControlFlow.update { it.copy(isExamModalOpen = false) }
    }

    fun saveAndPinExam(examTitle: String, targetDateEpochDay: Long) {
        viewModelScope.launch {
            val saved = repository.saveAndPinExam(
                examTitle = examTitle,
                targetDateEpochDay = targetDateEpochDay
            )
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "Pinned '${saved.examTitle}' countdown to your Exam Timer button!"
                )
            )
        }
    }

    fun pinExam(examId: String) {
        viewModelScope.launch {
            repository.pinExam(examId)
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "Updated pinned countdown on the Exam Timer button!"
                )
            )
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            repository.deleteExam(examId)
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "Removed exam countdown timer."
                )
            )
        }
    }

    fun recordNeuroDrillCompleted(
        drillType: NeuroDrillType,
        scoreMetric: Double,
        durationSeconds: Int,
        completionMessage: String
    ) {
        viewModelScope.launch {
            repository.recordCompletedNeuroSession(
                drillType = drillType,
                scoreMetric = scoreMetric,
                durationSeconds = durationSeconds
            )
            _events.emit(
                StudyUiEvent.ShowSnackbar(
                    message = "$completionMessage (+25 XP Earned!)"
                )
            )
        }
    }

    fun openAccountabilityModal() {
        modalControlFlow.update { it.copy(isAccountabilityModalOpen = true) }
    }

    fun closeAccountabilityModal() {
        modalControlFlow.update { it.copy(isAccountabilityModalOpen = false) }
    }

    fun exportDatabaseBackup(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    _events.emit(StudyUiEvent.ShowSnackbar(message = "Failed to open destination file for export"))
                    return@launch
                }
                val result = repository.exportBackupToJsonStream(outputStream)
                result.onSuccess { sliceCount ->
                    _events.emit(StudyUiEvent.ShowSnackbar(message = "✅ Exported $sliceCount study slices to JSON backup!"))
                }.onFailure { err ->
                    _events.emit(StudyUiEvent.ShowSnackbar(message = "Export failed: ${err.localizedMessage}"))
                }
            } catch (e: Exception) {
                _events.emit(StudyUiEvent.ShowSnackbar(message = "Export error: ${e.localizedMessage}"))
            }
        }
    }

    fun restoreDatabaseBackup(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _events.emit(StudyUiEvent.ShowSnackbar(message = "Failed to open source file for restore"))
                    return@launch
                }
                val result = repository.restoreBackupFromJsonStream(inputStream)
                result.onSuccess { msg ->
                    _events.emit(StudyUiEvent.ShowSnackbar(message = "✅ $msg"))
                }.onFailure { err ->
                    _events.emit(StudyUiEvent.ShowSnackbar(message = "Restore failed: ${err.localizedMessage}"))
                }
            } catch (e: Exception) {
                _events.emit(StudyUiEvent.ShowSnackbar(message = "Restore error: ${e.localizedMessage}"))
            }
        }
    }

    fun toggleMorningBriefing(context: Context, enabled: Boolean) {
        modalControlFlow.update { it.copy(morningBriefingEnabled = enabled) }
        ChronosWorkScheduler.scheduleDailyBriefing(context, enabled, forceUpdate = true)
    }

    fun toggleDominoAlerts(context: Context, enabled: Boolean) {
        modalControlFlow.update { it.copy(dominoAlertsEnabled = enabled) }
        ChronosWorkScheduler.scheduleDominoAlerts(context, enabled, forceUpdate = true)
    }

    private data class CoreDashboardData(
        val selectedDateEpochDay: Long,
        val batteryMode: EnergyLevel,
        val userStats: UserStats,
        val dateSlices: List<SliceWithParentChain>,
        val overdueSlices: List<SliceWithParentChain>
    )

    private data class AnalyticsAndChainsData(
        val allChains: List<TopicChainWithSlices>,
        val heatmapCells: List<HeatmapDayCell>,
        val subjectAnalytics: List<SubjectAnalyticsSummary>,
        val upcomingEchoes: List<SliceWithParentChain>
    )

    private data class ExamAndNeuroData(
        val pinnedExam: ExamCountdownEntity?,
        val allExams: List<ExamCountdownEntity>,
        val neuroTelemetry: NeuroTelemetrySummary
    )

    companion object {
        fun provideFactory(repository: StudyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudyViewModel(repository) as T
                }
            }
    }
}
