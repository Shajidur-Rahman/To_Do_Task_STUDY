package com.chronos.hsc.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscSyllabusCatalog
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.local.TopicChainWithSlices
import com.chronos.hsc.data.local.UserStats
import com.chronos.hsc.notification.ChronosNotificationHelper
import com.chronos.hsc.ui.components.AccountabilityCardModal
import com.chronos.hsc.ui.components.AddHscTopicModal
import com.chronos.hsc.ui.components.BossHealthBar
import com.chronos.hsc.ui.components.CascadeDeleteDialog
import com.chronos.hsc.ui.components.EbbinghausEchoDialog
import com.chronos.hsc.ui.components.EffortHeatmapCanvas
import com.chronos.hsc.ui.components.ExamCountdownModal
import com.chronos.hsc.ui.components.SwipeToSlashTaskCard
import com.chronos.hsc.ui.components.TaskRouletteModal
import com.chronos.hsc.ui.theme.BossShieldPurple
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.CyberOrange
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.CompositionLocalProvider
import com.chronos.hsc.ui.components.NatureAmbientOverlayCanvas
import com.chronos.hsc.ui.screens.UiSettingsScreen
import com.chronos.hsc.ui.theme.AppThemeMode
import com.chronos.hsc.ui.theme.FoliageDensity
import com.chronos.hsc.ui.theme.LocalAppThemeMode
import com.chronos.hsc.ui.theme.LocalNatureCritterType
import com.chronos.hsc.ui.theme.NatureLeafGreen
import com.chronos.hsc.ui.theme.NatureSproutLime
import com.chronos.hsc.ui.theme.TerrariumBackground
import com.chronos.hsc.ui.theme.TerrariumBorder
import com.chronos.hsc.ui.theme.TerrariumSurface
import com.chronos.hsc.ui.theme.TerrariumSurfaceElevated
import com.chronos.hsc.ui.theme.ThemePreferences
import com.chronos.hsc.ui.theme.brandColor
import com.chronos.hsc.ui.viewmodel.StudyUiEvent
import com.chronos.hsc.ui.viewmodel.StudyUiState
import com.chronos.hsc.ui.viewmodel.StudyViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class ChronosTab(val label: String) {
    BATTLEGROUND("Battleground"),
    SYLLABUS_BOSSES("Syllabus Bosses"),
    ANALYTICS("Analytics"),
    NEURO_GYM("Neuro-Gym"),
    UI_SETTINGS("UI Settings")
}

@Composable
fun MainScreen(
    viewModel: StudyViewModel,
    isUpdateAvailable: Boolean = false,
    onOpenUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val themePreferences = remember { ThemePreferences.getInstance(context) }
    val currentTheme by themePreferences.themeMode.collectAsState()
    val critterType by themePreferences.critterType.collectAsState()
    val foliageDensity by themePreferences.foliageDensity.collectAsState()
    val isCompactHeader by themePreferences.compactHeader.collectAsState()
    val ambientAnimations by themePreferences.ambientAnimationsEnabled.collectAsState()

    val isNature = currentTheme == AppThemeMode.NATURE_LOVER
    val containerBg = if (isNature) TerrariumBackground else ObsidianBackground
    val surfaceBg = if (isNature) TerrariumSurface else ObsidianSurface
    val accentColor = if (isNature) NatureLeafGreen else CyberCyan

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!ChronosNotificationHelper.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is StudyUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed && event.isUndoCascadeDelete) {
                        viewModel.undoLastCascadeDelete()
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppThemeMode provides currentTheme,
        LocalNatureCritterType provides critterType
    ) {
        Scaffold(
            containerColor = containerBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (ChronosTab.entries[selectedTabIndex] != ChronosTab.NEURO_GYM &&
                    ChronosTab.entries[selectedTabIndex] != ChronosTab.UI_SETTINGS
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Upper Button: Exam Countdown Pill Button
                        ExamCountdownFloatingButton(
                            todayEpochDay = state.todayEpochDay,
                            activeExam = state.pinnedExam,
                            onClick = { viewModel.openExamCountdownModal() }
                        )

                        // Bottom Button: Existing cyan / green + Add Study Topic FAB
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openAddTopicModal() },
                            containerColor = accentColor,
                            contentColor = if (isNature) Color(0xFF00210E) else Color(0xFF001F24),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Study Topic"
                                )
                            },
                            text = {
                                Text(
                                    text = "Add Study Topic",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = surfaceBg,
                    tonalElevation = 8.dp
                ) {
                    ChronosTab.entries.forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                val icon = when (tab) {
                                    ChronosTab.BATTLEGROUND -> Icons.Default.Bolt
                                    ChronosTab.SYLLABUS_BOSSES -> Icons.Default.AutoAwesomeMotion
                                    ChronosTab.ANALYTICS -> Icons.Default.Analytics
                                    ChronosTab.NEURO_GYM -> Icons.Default.Psychology
                                    ChronosTab.UI_SETTINGS -> Icons.Default.Tune
                                }
                                Icon(imageVector = icon, contentDescription = tab.label)
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accentColor,
                                selectedTextColor = accentColor,
                                indicatorColor = accentColor.copy(alpha = 0.16f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (ChronosTab.entries[selectedTabIndex]) {
                    ChronosTab.BATTLEGROUND -> TodayBattlegroundTab(
                        state = state,
                        isUpdateAvailable = isUpdateAvailable,
                        onOpenUpdate = onOpenUpdate,
                        isCompactHeader = isCompactHeader,
                        isNature = isNature,
                        foliageDensity = foliageDensity,
                        ambientAnimations = ambientAnimations,
                        onSelectBatteryMode = viewModel::selectBatteryMode,
                        onSelectDate = viewModel::selectDate,
                        onExecuteDominoShift = viewModel::executeGlobalDominoShift,
                        onOpenRoulette = viewModel::openTaskRoulette,
                        onOpenAddTopic = viewModel::openAddTopicModal,
                        onOpenNeuroGymQuickRescue = {
                            selectedTabIndex = ChronosTab.NEURO_GYM.ordinal
                        },
                        onOpenAccountability = viewModel::openAccountabilityModal,
                        onIncrementProblem = viewModel::incrementProblemSubCounter,
                        onDecrementProblem = viewModel::decrementProblemSubCounter,
                        onCompleteSlice = viewModel::completeTaskSlice,
                        onUncompleteSlice = viewModel::uncompleteTaskSlice,
                        onPushDominoPlusOne = viewModel::pushSliceDominoPlusOneDay,
                        onRequestDeleteSlice = viewModel::requestDeleteForSlice,
                        onConvertHiddenToLightReview = viewModel::convertHiddenSliceToLightReview,
                        onCatchUpSlice = viewModel::catchUpSliceToToday,
                        onInstantCatchUpAll = viewModel::instantCatchUpAllLaggingChains
                    )

                    ChronosTab.SYLLABUS_BOSSES -> ActiveChainsTab(
                        state = state,
                        onOpenAddTopic = viewModel::openAddTopicModal,
                        onRequestDeleteChain = viewModel::requestDeleteForEntireChain,
                        onJumpToSliceDate = { epochDay ->
                            viewModel.selectDate(epochDay)
                            selectedTabIndex = 0
                        }
                    )

                    ChronosTab.ANALYTICS -> AnalyticsAndHeatmapTab(
                        state = state,
                        onJumpToSliceDate = { epochDay ->
                            viewModel.selectDate(epochDay)
                            selectedTabIndex = 0
                        },
                        onOpenAccountabilityModal = viewModel::openAccountabilityModal
                    )

                    ChronosTab.NEURO_GYM -> NeuroGymScreen(
                        telemetry = state.neuroTelemetry,
                        onDrillCompleted = viewModel::recordNeuroDrillCompleted
                    )

                    ChronosTab.UI_SETTINGS -> UiSettingsScreen(
                        preferences = themePreferences,
                        viewModel = viewModel
                    )
                }
            }

        // 1. Structured HSC Topic Creation Modal
        if (state.isAddTopicModalOpen) {
            AddHscTopicModal(
                todayEpochDay = state.todayEpochDay,
                initialSelectedDateEpochDay = state.selectedDateEpochDay,
                existingChains = state.allChainsWithSlices,
                onDismiss = viewModel::closeAddTopicModal,
                onGenerateChain = viewModel::createStructuredTopicChain
            )
        }

        // 2. Full-Topic Cascade Delete vs Single Slice Re-Index Modal
        if (state.slicePendingDelete != null || state.chainPendingDelete != null) {
            CascadeDeleteDialog(
                slicePendingDelete = state.slicePendingDelete,
                chainPendingDelete = state.chainPendingDelete,
                onDismiss = viewModel::dismissDeleteDialog,
                onDeleteEntireChain = viewModel::confirmCascadeDeleteEntireChain,
                onDeleteSingleSliceAndReindex = viewModel::confirmDeleteSingleSliceAndReindex
            )
        }

        // 3. Task Roulette & 25m Micro-Sprint Modal
        if (state.isTaskRouletteOpen) {
            TaskRouletteModal(
                incompleteSlices = state.visibleDateSlices.filter { !it.slice.isCompleted },
                sprintTimerState = state.sprintTimerState,
                onDismiss = viewModel::closeTaskRoulette,
                onLockedSlice = viewModel::onRouletteLockedSlice,
                onAcceptAndStartSprint = viewModel::acceptAndStartMicroSprint,
                onTogglePauseSprint = viewModel::togglePauseSprintTimer,
                onCompleteSprintSlice = viewModel::completeTaskSlice,
                onStopSprint = viewModel::stopSprintTimer
            )
        }

        // 4. Spaced Repetition Auto-Spawner (Ebbinghaus Retention Echo) Dialog
        state.conqueredChainForEchoDialog?.let { conqueredChain ->
            EbbinghausEchoDialog(
                conqueredChain = conqueredChain,
                onConfirm = viewModel::dismissEbbinghausEchoDialog
            )
        }

        // 5. Interactive Exam Countdown Modal
        if (state.isExamModalOpen) {
            ExamCountdownModal(
                todayEpochDay = state.todayEpochDay,
                pinnedExam = state.pinnedExam,
                allExams = state.allExams,
                onDismiss = viewModel::closeExamCountdownModal,
                onSaveAndPinExam = viewModel::saveAndPinExam,
                onPinExam = viewModel::pinExam,
                onDeleteExam = viewModel::deleteExam
            )
        }

        // 6. Accountability, Shareable Cyberpunk Card & Full Offline Backup Modal
        if (state.isAccountabilityModalOpen) {
            AccountabilityCardModal(
                todayEpochDay = state.todayEpochDay,
                userStats = state.userStats,
                pinnedExam = state.pinnedExam,
                completedSlicesCount = state.visibleDateSlices.count { it.slice.isCompleted },
                totalProblemsToday = state.visibleDateSlices.sumOf { it.slice.problemsCompleted },
                activeBossesCount = state.allChainsWithSlices.count { !it.chain.isChainCompleted },
                onDismiss = viewModel::closeAccountabilityModal,
                onExportBackup = { uri ->
                    viewModel.exportDatabaseBackup(uri, context.contentResolver)
                },
                onRestoreBackup = { uri ->
                    viewModel.restoreDatabaseBackup(uri, context.contentResolver)
                },
                onToggleMorningBriefing = { enabled ->
                    viewModel.toggleMorningBriefing(context, enabled)
                },
                onToggleDominoAlerts = { enabled ->
                    viewModel.toggleDominoAlerts(context, enabled)
                },
                morningBriefingEnabled = state.morningBriefingEnabled,
                dominoAlertsEnabled = state.dominoAlertsEnabled
            )
        }
    }
}
}

@Composable
private fun ExamCountdownFloatingButton(
    todayEpochDay: Long,
    activeExam: ExamCountdownEntity?,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "examUrgencyPulse")
    val crimsonPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crimsonPulse"
    )

    if (activeExam == null) {
        // Unconfigured State: "⏳ Add Timer Day"
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121824),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(CyberMagenta, CyberCyan)
                )
            ),
            shadowElevation = 6.dp,
            modifier = Modifier.clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏳",
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Timer Day",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = CyberCyan
                )
            }
        }
    } else {
        // Configured State: Dynamically calculates daysRemaining
        val daysRemaining = ChronoUnit.DAYS.between(
            LocalDate.ofEpochDay(todayEpochDay),
            LocalDate.ofEpochDay(activeExam.targetDateEpochDay)
        )
        val primaryLine = when {
            daysRemaining > 0L -> "⏳ $daysRemaining Days Left"
            daysRemaining == 0L -> "🎯 EXAM TODAY!"
            else -> "✅ Completed"
        }

        // Dynamic Urgency Border & Glow Color:
        // > 60 days: Neon Cyan (#00E5FF)
        // 15 to 60 days: Electric Amber (#FFAB00)
        // < 15 days: Pulsing Crimson Neon (#FF1744)
        val borderColor = when {
            daysRemaining > 60L -> CyberCyan
            daysRemaining in 15L..60L -> CyberAmber
            else -> CyberCrimson.copy(alpha = crimsonPulseAlpha)
        }
        val textColor = when {
            daysRemaining > 60L -> CyberCyan
            daysRemaining in 15L..60L -> CyberAmber
            else -> CyberCrimson
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121824),
            border = BorderStroke(width = 1.5.dp, color = borderColor),
            shadowElevation = 8.dp,
            modifier = Modifier.clickable { onClick() }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = primaryLine,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = activeExam.examTitle,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TodayBattlegroundTab(
    state: StudyUiState,
    isUpdateAvailable: Boolean = false,
    onOpenUpdate: () -> Unit = {},
    isCompactHeader: Boolean = true,
    isNature: Boolean = false,
    foliageDensity: FoliageDensity = FoliageDensity.LUSH,
    ambientAnimations: Boolean = true,
    onSelectBatteryMode: (EnergyLevel) -> Unit,
    onSelectDate: (Long) -> Unit,
    onExecuteDominoShift: () -> Unit,
    onOpenRoulette: () -> Unit,
    onOpenAddTopic: () -> Unit,
    onOpenNeuroGymQuickRescue: () -> Unit,
    onOpenAccountability: () -> Unit,
    onIncrementProblem: (String) -> Unit,
    onDecrementProblem: (String) -> Unit,
    onCompleteSlice: (String) -> Unit,
    onUncompleteSlice: (String) -> Unit = {},
    onPushDominoPlusOne: (SliceWithParentChain) -> Unit,
    onRequestDeleteSlice: (SliceWithParentChain) -> Unit,
    onConvertHiddenToLightReview: (String) -> Unit,
    onCatchUpSlice: (String) -> Unit = {},
    onInstantCatchUpAll: () -> Unit = {}
) {
    var isZombieHiddenExpanded by remember { mutableStateOf(false) }
    var isToolsDeckExpanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isNature && ambientAnimations) {
            NatureAmbientOverlayCanvas(
                density = foliageDensity,
                modifier = Modifier.fillMaxSize()
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Header: Level/XP Bar, Streak Counter, Battery Mode Toggle & Neuro-Gym Quick-Rescue Trigger
            item {
                if (isCompactHeader) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactTopHudBar(
                            userStats = state.userStats,
                            selectedBatteryMode = state.batteryMode,
                            isToolsExpanded = isToolsDeckExpanded,
                            isUpdateAvailable = isUpdateAvailable,
                            isNature = isNature,
                            onToggleTools = { isToolsDeckExpanded = !isToolsDeckExpanded },
                            onCycleBatteryMode = {
                                val nextMode = when (state.batteryMode) {
                                    EnergyLevel.HIGH_FOCUS -> EnergyLevel.MEDIUM
                                    EnergyLevel.MEDIUM -> EnergyLevel.ZOMBIE_MODE
                                    EnergyLevel.ZOMBIE_MODE -> EnergyLevel.HIGH_FOCUS
                                }
                                onSelectBatteryMode(nextMode)
                            },
                            onOpenUpdate = onOpenUpdate
                        )

                        AnimatedVisibility(visible = isToolsDeckExpanded) {
                            CollapsibleQuickCommandDeck(
                                isNature = isNature,
                                isUpdateAvailable = isUpdateAvailable,
                                onOpenRoulette = onOpenRoulette,
                                onOpenNeuroGymQuickRescue = onOpenNeuroGymQuickRescue,
                                onOpenAccountability = onOpenAccountability,
                                onInstantCatchUpAll = onInstantCatchUpAll,
                                onOpenUpdate = onOpenUpdate
                            )
                        }
                    }
                } else {
                    BattlegroundTopHeader(
                        userStats = state.userStats,
                        selectedBatteryMode = state.batteryMode,
                        onSelectBatteryMode = onSelectBatteryMode,
                        onOpenNeuroGymQuickRescue = onOpenNeuroGymQuickRescue,
                        onOpenAccountability = onOpenAccountability,
                        isUpdateAvailable = isUpdateAvailable,
                        onOpenUpdate = onOpenUpdate
                    )
                }
            }

            // 2. Active Micro-Sprint Banner (if Task Roulette timer is running)
            state.sprintTimerState?.let { sprint ->
                if (sprint.isSprintRunning) {
                    item {
                        ActiveSprintBanner(
                            sprintBadge = sprint.targetShortBadge,
                            sprintTitle = sprint.targetSliceTitle,
                            isPaused = sprint.isSprintPaused,
                            secondsRemaining = sprint.sprintSecondsRemaining,
                            onOpenSprintModal = onOpenRoulette
                        )
                    }
                }
            }

            // 3. 1-Tap Domino Shift Conflict Banner (When past slices are unfinished)
            if (state.overdueSlices.isNotEmpty()) {
                item {
                    DominoConflictBanner(
                        overdueSlices = state.overdueSlices,
                        overdueChainsCount = state.overdueChainsCount,
                        onExecuteDominoShift = onExecuteDominoShift
                    )
                }
            }

            // 4. Quick Actions Row (Task Roulette + Adaptive Date Scrubber)
            item {
                DateScrubberAndRouletteSection(
                    todayEpochDay = state.todayEpochDay,
                    selectedDateEpochDay = state.selectedDateEpochDay,
                    allChainsWithSlices = state.allChainsWithSlices,
                    isCompact = isCompactHeader,
                    onSelectDate = onSelectDate,
                    onOpenRoulette = onOpenRoulette
                )
            }

            // 5. Active Bosses Mini Carousel
            val activeBossChains = state.allChainsWithSlices.filter { !it.chain.isChainCompleted }
            if (activeBossChains.isNotEmpty()) {
                item {
                    ActiveBossesMiniCarousel(activeChains = activeBossChains)
                }
            }

            // 6. Section Title for Selected Date Slices
            item {
                val selectedLocalDate = LocalDate.ofEpochDay(state.selectedDateEpochDay)
                val dateTitle = when (state.selectedDateEpochDay) {
                    state.todayEpochDay -> "TODAY'S BATTLEGROUND SLICES"
                    state.todayEpochDay - 1L -> "YESTERDAY'S SLICES"
                    state.todayEpochDay + 1L -> "TOMORROW'S SCHEDULED SLICES"
                    else -> selectedLocalDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")).uppercase() + " SLICES"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateTitle,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isNature) NatureLeafGreen else Color.White
                    )
                    Text(
                        text = "${state.visibleDateSlices.count { it.slice.isCompleted }}/${state.visibleDateSlices.size} Done",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // 7. Task Slice Cards List
            if (state.visibleDateSlices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isNature) TerrariumSurface else ObsidianSurface)
                            .border(1.dp, if (isNature) TerrariumBorder else ObsidianBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.hiddenZombieSlices.isNotEmpty()) {
                                    "🧟 All heavy tasks are hidden in 15% Zombie Mode"
                                } else {
                                    "⚡ No study slices scheduled for this date"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create an HSC topic chain (Bangla, English, Physics, Chemistry, Higher Math, Biology, or ICT) to auto-slice it across 1 to 15 days.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (state.hiddenZombieSlices.isEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onOpenAddTopic,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNature) NatureLeafGreen else CyberCyan,
                                        contentColor = if (isNature) Color(0xFF00210E) else Color(0xFF001F24)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Create Study Topic Chain",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = state.visibleDateSlices,
                    key = { it.slice.sliceId }
                ) { sliceWithChain ->
                    SwipeToSlashTaskCard(
                        item = sliceWithChain,
                        todayEpochDay = state.todayEpochDay,
                        onIncrementProblem = { onIncrementProblem(sliceWithChain.slice.sliceId) },
                        onDecrementProblem = { onDecrementProblem(sliceWithChain.slice.sliceId) },
                        onCompleteSlice = { onCompleteSlice(sliceWithChain.slice.sliceId) },
                        onUncompleteSlice = { onUncompleteSlice(sliceWithChain.slice.sliceId) },
                        onPushDominoPlusOneDay = { onPushDominoPlusOne(sliceWithChain) },
                        onRequestDelete = { onRequestDeleteSlice(sliceWithChain) },
                        onCatchUpToToday = { onCatchUpSlice(sliceWithChain.slice.sliceId) }
                    )
                }
            }

            // 8. Zombie Mode Protected Brain Footer (Collapsed heavy tasks + Convert to Light Review 15m)
            if (state.batteryMode == EnergyLevel.ZOMBIE_MODE && state.hiddenZombieSlices.isNotEmpty()) {
                item {
                    ZombieModeHiddenTasksFooter(
                        hiddenSlices = state.hiddenZombieSlices,
                        isExpanded = isZombieHiddenExpanded,
                        onToggleExpand = { isZombieHiddenExpanded = !isZombieHiddenExpanded },
                        onConvertToLightReview = onConvertHiddenToLightReview
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTopHudBar(
    userStats: UserStats,
    selectedBatteryMode: EnergyLevel,
    isToolsExpanded: Boolean,
    isUpdateAvailable: Boolean,
    isNature: Boolean,
    onToggleTools: () -> Unit,
    onCycleBatteryMode: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    val primaryColor = if (isNature) NatureLeafGreen else Color.White
    val secondaryColor = if (isNature) NatureSproutLime else Color(0xFFA1A1AA)
    val surfaceBg = if (isNature) TerrariumSurface else ObsidianSurface
    val borderCol = if (isNature) TerrariumBorder else ObsidianBorder

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surfaceBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Badge + Level + inline XP bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isNature) "🌿 CHRONOS" else "⚡ CHRONOS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = primaryColor
                )

                // LVL Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNature) secondaryColor.copy(alpha = 0.2f) else Color(0xFF18181B))
                        .border(1.dp, if (isNature) secondaryColor.copy(alpha = 0.7f) else Color(0xFF3F3F46), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "L${userStats.currentLevel}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = TextPrimary
                    )
                }

                // Thin Inline XP Progress Bar
                LinearProgressIndicator(
                    progress = { userStats.levelProgressFraction },
                    color = primaryColor,
                    trackColor = if (isNature) Color(0xFF08120B) else ObsidianBackground,
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            // Center: Streak badge + Compact Battery Mode Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Streak Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isNature) CyberOrange.copy(alpha = 0.16f) else Color(0xFF18181B),
                    border = BorderStroke(1.dp, if (isNature) CyberOrange.copy(alpha = 0.7f) else Color(0xFF3F3F46))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 ${userStats.streakDays}d",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isNature) CyberOrange else Color(0xFFE4E4E7)
                        )
                    }
                }

                // Active Critter Companion Badge in Nature Mode
                if (isNature) {
                    val critter = LocalNatureCritterType.current
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NatureLeafGreen.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, NatureLeafGreen.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "${critter.emoji} ${critter.displayName.substringBefore(" ")}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = NatureSproutLime,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Compact Battery Mode Pill (cycles on tap!)
                val (modeLabel, modeColor) = when (selectedBatteryMode) {
                    EnergyLevel.HIGH_FOCUS -> "⚡100%" to (if (isNature) primaryColor else Color.White)
                    EnergyLevel.MEDIUM -> "🔋50%" to (if (isNature) CyberAmber else Color(0xFFE4E4E7))
                    EnergyLevel.ZOMBIE_MODE -> "🧟15%" to (if (isNature) CyberEmerald else Color(0xFFA1A1AA))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isNature) modeColor.copy(alpha = 0.18f) else Color(0xFF27272A),
                    border = BorderStroke(1.dp, if (isNature) modeColor else Color(0xFF52525B)),
                    modifier = Modifier.clickable { onCycleBatteryMode() }
                ) {
                    Text(
                        text = modeLabel,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = modeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Right: Update Pill (if available) + Tools Toggle Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isUpdateAvailable) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (isNature) CyberEmerald else Color.White).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (isNature) CyberEmerald else Color.White),
                        modifier = Modifier.clickable { onOpenUpdate() }
                    ) {
                        Text(
                            text = "⚡ Upd",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            color = if (isNature) CyberEmerald else Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }
                }

                // 🎛️ Tools Toggle Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isToolsExpanded) primaryColor.copy(alpha = 0.22f) else (if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated),
                    border = BorderStroke(1.dp, if (isToolsExpanded) primaryColor else borderCol),
                    modifier = Modifier.clickable { onToggleTools() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isToolsExpanded) "🎛️ ▲" else "🎛️ ▼",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isToolsExpanded) primaryColor else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleQuickCommandDeck(
    isNature: Boolean,
    isUpdateAvailable: Boolean,
    onOpenRoulette: () -> Unit,
    onOpenNeuroGymQuickRescue: () -> Unit,
    onOpenAccountability: () -> Unit,
    onInstantCatchUpAll: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    val primaryColor = if (isNature) NatureLeafGreen else Color.White
    val secondaryColor = if (isNature) NatureSproutLime else Color(0xFFA1A1AA)
    val surfaceBg = if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated
    val borderCol = if (isNature) TerrariumBorder else ObsidianBorder

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = surfaceBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🎲 Spin Roulette
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isNature) secondaryColor.copy(alpha = 0.16f) else Color(0xFF18181B),
                border = BorderStroke(1.dp, if (isNature) secondaryColor.copy(alpha = 0.7f) else Color(0xFF3F3F46)),
                modifier = Modifier.clickable { onOpenRoulette() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎲 Spin Roulette",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNature) secondaryColor else Color.White
                    )
                }
            }

            // 🧠 60s Reset
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isNature) CyberCyan.copy(alpha = 0.14f) else Color(0xFF18181B),
                border = BorderStroke(1.dp, if (isNature) CyberCyan.copy(alpha = 0.6f) else Color(0xFF3F3F46)),
                modifier = Modifier.clickable { onOpenNeuroGymQuickRescue() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧠 60s Focus Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNature) CyberCyan else Color.White
                    )
                }
            }

            // 🎴 Card & Backup
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isNature) primaryColor.copy(alpha = 0.14f) else Color(0xFF18181B),
                border = BorderStroke(1.dp, if (isNature) primaryColor.copy(alpha = 0.6f) else Color(0xFF3F3F46)),
                modifier = Modifier.clickable { onOpenAccountability() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎴 Card & Backup",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNature) primaryColor else Color.White
                    )
                }
            }

            // ⚡ Catch Up All
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isNature) CyberAmber.copy(alpha = 0.14f) else Color(0xFF18181B),
                border = BorderStroke(1.dp, if (isNature) CyberAmber.copy(alpha = 0.7f) else Color(0xFF3F3F46)),
                modifier = Modifier.clickable { onInstantCatchUpAll() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNature) "🌿 Catch Up All" else "⚡ Catch Up All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNature) CyberAmber else Color.White
                    )
                }
            }

            if (isUpdateAvailable) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberEmerald.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CyberEmerald),
                    modifier = Modifier.clickable { onOpenUpdate() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚡ Update App", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                    }
                }
            }
        }
    }
}

@Composable
private fun BattlegroundTopHeader(
    userStats: UserStats,
    selectedBatteryMode: EnergyLevel,
    onSelectBatteryMode: (EnergyLevel) -> Unit,
    onOpenNeuroGymQuickRescue: () -> Unit,
    onOpenAccountability: () -> Unit,
    isUpdateAvailable: Boolean = false,
    onOpenUpdate: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianSurfaceElevated, ObsidianSurface)
                )
            )
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHRONOS HSC",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberMagenta.copy(alpha = 0.22f))
                            .border(1.dp, CyberMagenta, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LVL ${userStats.currentLevel}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color(0xFFF3C6FF)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${userStats.totalXp} Total XP • ${userStats.xpIntoCurrentLevel}/${userStats.xpForNextLevel} to Next Level",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Streak & Card Hub Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Streak Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberOrange.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CyberOrange)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = CyberOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userStats.streakDays}d Streak",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = CyberOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 1-Tap Share Card & Backup Hub Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCyan.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CyberCyan),
                    modifier = Modifier.clickable { onOpenAccountability() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Progress",
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Card",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }
                }

                // If update is available, show stylish Update pill right beside Card button
                if (isUpdateAvailable) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val infiniteTransition = rememberInfiniteTransition(label = "updateBadgePulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.55f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "updatePulse"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberEmerald.copy(alpha = 0.22f),
                        border = BorderStroke(1.2.dp, CyberEmerald.copy(alpha = pulseAlpha)),
                        modifier = Modifier.clickable { onOpenUpdate() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Update Available",
                                tint = CyberEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Update",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = CyberEmerald
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { userStats.levelProgressFraction },
            color = CyberCyan,
            trackColor = ObsidianBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent 3-State Energy / Battery Mode Segmented Selector
        Text(
            text = "MENTAL BATTERY MODE FILTER",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianBackground)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EnergyLevel.entries.forEach { mode ->
                val isSelected = selectedBatteryMode == mode
                val modeEmoji = when (mode) {
                    EnergyLevel.HIGH_FOCUS -> "⚡ 100% Focus"
                    EnergyLevel.MEDIUM -> "🔋 50% Medium"
                    EnergyLevel.ZOMBIE_MODE -> "🧟 15% Zombie"
                }
                val accentColor = when (mode) {
                    EnergyLevel.HIGH_FOCUS -> CyberCyan
                    EnergyLevel.MEDIUM -> CyberAmber
                    EnergyLevel.ZOMBIE_MODE -> CyberEmerald
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.22f) else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { onSelectBatteryMode(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeEmoji,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) accentColor else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick-Rescue Trigger on Battleground: Opens Neuro-Gym directly
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CyberMagenta.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenNeuroGymQuickRescue() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Lost Focus? Run 60s Brain Reset >>",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color(0xFFF3C6FF)
                )
                Text(
                    text = "NEURO-GYM",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = CyberCyan
                )
            }
        }
    }
}

@Composable
private fun ActiveSprintBanner(
    sprintBadge: String,
    sprintTitle: String,
    isPaused: Boolean,
    secondsRemaining: Int,
    onOpenSprintModal: () -> Unit
) {
    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val formatted = String.format("%02d:%02d", mins, secs)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isPaused) CyberAmber.copy(alpha = 0.16f) else CyberEmerald.copy(alpha = 0.16f),
        border = BorderStroke(1.5.dp, if (isPaused) CyberAmber else CyberEmerald),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSprintModal() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPaused) {
                        "⏸️ 25m ROULETTE MICRO-SPRINT PAUSED"
                    } else {
                        "⚡ 25m ROULETTE MICRO-SPRINT ACTIVE (2X XP)"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    color = if (isPaused) CyberAmber else CyberEmerald
                )
                Text(
                    text = "$sprintBadge • $sprintTitle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (isPaused) CyberAmber else CyberEmerald
            )
        }
    }
}

@Composable
private fun DominoConflictBanner(
    overdueSlices: List<SliceWithParentChain>,
    overdueChainsCount: Int,
    onExecuteDominoShift: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dominoAlertPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dominoPulse"
    )

    val badgesPreview = overdueSlices.joinToString(", ") { it.slice.shortBadge }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF231020),
        border = BorderStroke(1.5.dp, CyberCrimson.copy(alpha = pulseAlpha)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = CyberCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DOMINO CONFLICT DETECTED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = CyberCrimson
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BossShieldPurple.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🛡️ +10% BOSS SHIELD ACTIVE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA80FC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Missed slice(s): [$badgesPreview]. Don't let ${overdueSlices.first().slice.shortBadge} stack on top of today's next slice!",
                fontSize = 12.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onExecuteDominoShift,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "1-Tap Domino Shift ($overdueChainsCount Chain${if (overdueChainsCount > 1) "s" else ""} -> Cascade Forward)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DateScrubberAndRouletteSection(
    todayEpochDay: Long,
    selectedDateEpochDay: Long,
    allChainsWithSlices: List<TopicChainWithSlices>,
    isCompact: Boolean = false,
    onSelectDate: (Long) -> Unit,
    onOpenRoulette: () -> Unit
) {
    val isNature = LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER
    val allSlices = remember(allChainsWithSlices) {
        allChainsWithSlices.flatMap { it.slices }
    }
    val dayOfWeekFormatter = remember { DateTimeFormatter.ofPattern("EEE") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM") }

    // Build a 1.5-month (45-day) window starting from past 3 days up to today + 45 days
    val futureDaysWindow = 45L
    val baseCenterEpoch = if (selectedDateEpochDay in (todayEpochDay - 3L)..(todayEpochDay + futureDaysWindow)) {
        todayEpochDay
    } else {
        selectedDateEpochDay
    }
    val scrubberDays = remember(baseCenterEpoch, todayEpochDay) {
        if (baseCenterEpoch == todayEpochDay) {
            (-3L..futureDaysWindow).map { todayEpochDay + it }
        } else {
            (-3L..futureDaysWindow).map { baseCenterEpoch + it }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)) {
        // Task Roulette Button (only shown in full non-compact mode; in compact mode it lives inside Tools drawer)
        if (!isCompact) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isNature) NatureSproutLime.copy(alpha = 0.16f) else CyberMagenta.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, if (isNature) NatureSproutLime else CyberMagenta),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenRoulette() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Task Roulette",
                            tint = if (isNature) NatureSproutLime else CyberMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🎲 Spin Task Roulette (Beat Decision Paralysis)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Locks onto a random slice + 25m Micro-Sprint with 2x XP Multiplier",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = "SPIN >>",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = if (isNature) NatureSproutLime else CyberMagenta
                    )
                }
            }
        }

        // Date Scrubber Header & Jump to Today action
        if (selectedDateEpochDay != todayEpochDay) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isNature) NatureLeafGreen else Color.White).copy(alpha = if (isNature) 0.18f else 0.12f),
                    border = BorderStroke(1.dp, if (isNature) NatureLeafGreen else Color.White),
                    modifier = Modifier.clickable { onSelectDate(todayEpochDay) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Jump to Today",
                            tint = if (isNature) NatureLeafGreen else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Jump Back to Today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNature) NatureLeafGreen else Color.White
                        )
                    }
                }
            }
        }

        val scrubberScrollState = rememberScrollState()
        val density = LocalDensity.current
        LaunchedEffect(todayEpochDay) {
            val targetIndex = scrubberDays.indexOf(todayEpochDay).coerceAtLeast(0)
            if (targetIndex > 0) {
                val itemWidthPx = with(density) { 65.dp.toPx() }
                val paddingPx = with(density) { 16.dp.toPx() }
                scrubberScrollState.scrollTo((targetIndex * itemWidthPx - paddingPx).toInt().coerceAtLeast(0))
            }
        }

        // Horizontal Date Scrubber
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrubberScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            scrubberDays.forEach { dayEpoch ->
                val date = LocalDate.ofEpochDay(dayEpoch)
                val isSelected = selectedDateEpochDay == dayEpoch
                val isToday = dayEpoch == todayEpochDay
                val slicesOnDay = allSlices.filter { it.scheduledDateEpochDay == dayEpoch }
                val incompleteCount = slicesOnDay.count { !it.isCompleted }
                val hasOverdue = dayEpoch < todayEpochDay && incompleteCount > 0

                val cardSurface = when {
                    isSelected -> if (isNature) NatureLeafGreen.copy(alpha = 0.22f) else Color(0xFF27272A)
                    hasOverdue -> if (isNature) Color(0xFF331610) else Color(0xFF231015)
                    else -> if (isNature) TerrariumSurface else ObsidianSurface
                }

                val cardBorder = when {
                    isSelected -> if (isNature) NatureLeafGreen else Color.White
                    hasOverdue -> if (isNature) Color(0xFFEA580C) else Color(0xFFF43F5E)
                    isToday -> if (isNature) NatureSproutLime.copy(alpha = 0.7f) else Color(0xFF52525B)
                    else -> if (isNature) TerrariumBorder else ObsidianBorder
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardSurface,
                    border = BorderStroke(
                        width = if (isSelected || isToday) 1.5.dp else 1.dp,
                        color = cardBorder
                    ),
                    modifier = Modifier
                        .width(if (isCompact) 58.dp else 64.dp)
                        .clickable { onSelectDate(dayEpoch) }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = if (isCompact) 5.dp else 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isToday) "TODAY" else date.format(dayOfWeekFormatter).uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 8.5.sp else 9.sp,
                            color = when {
                                isSelected -> if (isNature) NatureLeafGreen else Color.White
                                isToday -> if (isNature) NatureSproutLime else Color(0xFFA1A1AA)
                                else -> TextSecondary
                            }
                        )
                        Spacer(modifier = Modifier.height(if (isCompact) 1.dp else 2.dp))
                        Text(
                            text = "${date.dayOfMonth}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isCompact) 14.sp else 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = date.format(monthFormatter),
                            fontSize = if (isCompact) 8.5.sp else 9.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 3.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        hasOverdue -> if (isNature) Color(0xFFEA580C) else CyberCrimson
                                        slicesOnDay.isNotEmpty() -> if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (slicesOnDay.isEmpty()) "—" else "${slicesOnDay.size} sl",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasOverdue) Color.White else (if (isNature) NatureLeafGreen else CyberCyan)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBossesMiniCarousel(
    activeChains: List<TopicChainWithSlices>
) {
    Column {
        Text(
            text = "⚔️ ACTIVE SYLLABUS BOSSES (${activeChains.size})",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = activeChains,
                key = { it.chain.chainId }
            ) { chainWithSlices ->
                val chain = chainWithSlices.chain
                val sColor = chain.subject.brandColor()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, sColor.copy(alpha = 0.5f)),
                    modifier = Modifier.width(235.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${chain.subject.fullName} Ch ${chain.chapterNumber}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = sColor
                            )
                            Text(
                                text = "${chainWithSlices.completedCoreSlicesCount}/${chain.totalDays} Days",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        val banglaBossChapter = chain.chapterName.trim().ifEmpty {
                            HscSyllabusCatalog.getChapterNameBangla(
                                chain.subject,
                                chain.paper,
                                chain.chapterNumber
                            )
                        }
                        Text(
                            text = "অধ্যায় ${HscSyllabusCatalog.toBanglaDigits(chain.chapterNumber)}: $banglaBossChapter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BossHealthBar(
                            chain = chain,
                            coreSlices = chainWithSlices.coreSlices,
                            currentBossHp = chainWithSlices.currentBossHp,
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZombieModeHiddenTasksFooter(
    hiddenSlices: List<SliceWithParentChain>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onConvertToLightReview: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Hidden to protect your brain (${hiddenSlices.size} heavy task${if (hiddenSlices.size > 1) "s" else ""})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CyberEmerald
                        )
                        Text(
                            text = "15% Zombie Mode active • Tap to inspect or convert to 15m Light Review",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = CyberEmerald
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hiddenSlices.forEach { hidden ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianSurfaceElevated)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hidden.slice.displayTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${hidden.effectiveEnergyLevel.label} • ${hidden.slice.problemsTarget} Problems",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberEmerald.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CyberEmerald),
                                modifier = Modifier.clickable {
                                    onConvertToLightReview(hidden.slice.sliceId)
                                }
                            ) {
                                Text(
                                    text = "Convert to Light Review (15m)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChainsTab(
    state: StudyUiState,
    onOpenAddTopic: () -> Unit,
    onRequestDeleteChain: (TopicChainWithSlices) -> Unit,
    onJumpToSliceDate: (Long) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "⚔️ ACTIVE CHAINS & SYLLABUS BOSSES",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = CyberCyan
                )
                Text(
                    text = "Relational parent-child chains • Deleting a chain wipes all past, today, and future slices + echoes",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        if (state.allChainsWithSlices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Active Syllabus Boss Chains Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add a multi-day chapter to track its segmented Boss HP bar and full daily timeline here.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenAddTopic,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001F24)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Study Topic",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = state.allChainsWithSlices,
                key = { it.chain.chainId }
            ) { item ->
                val chain = item.chain
                val subjectColor = chain.subject.brandColor()
                val resolvedBanglaName = chain.chapterName.trim().ifEmpty {
                    HscSyllabusCatalog.getChapterNameBangla(
                        chain.subject,
                        chain.paper,
                        chain.chapterNumber
                    )
                }
                val chapterHeading = "অধ্যায় ${HscSyllabusCatalog.toBanglaDigits(chain.chapterNumber)}: $resolvedBanglaName"

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(subjectColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${chain.subject.fullName} • ${chain.paper.fullName}",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = subjectColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${chain.totalDays}-Day Chain",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = chapterHeading,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                            }

                            // 1-Tap Delete Entire Topic Chain Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CyberCrimson.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, CyberCrimson),
                                modifier = Modifier.clickable { onRequestDeleteChain(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Delete Entire Chain",
                                        tint = CyberCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delete Chain",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyberCrimson
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        BossHealthBar(
                            chain = chain,
                            coreSlices = item.coreSlices,
                            currentBossHp = item.currentBossHp,
                            compact = false
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full N-Day Timeline Pills: (4-1 ✅ -> 4-2 🎯 -> 4-3 ⏳ -> 4-4 ⏳)
                        Text(
                            text = "DAILY SLICE TIMELINE (TAP TO INSPECT DATE):",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item.coreSlices.forEach { slice ->
                                val statusEmoji = when {
                                    slice.isCompleted -> "✅"
                                    slice.scheduledDateEpochDay < state.todayEpochDay -> "⚠️"
                                    slice.scheduledDateEpochDay == state.todayEpochDay -> "🎯"
                                    else -> "⏳"
                                }
                                val pillColor = when {
                                    slice.isCompleted -> CyberEmerald
                                    slice.scheduledDateEpochDay < state.todayEpochDay -> CyberCrimson
                                    slice.scheduledDateEpochDay == state.todayEpochDay -> CyberCyan
                                    else -> TextSecondary
                                }
                                val dateStr = LocalDate.ofEpochDay(slice.scheduledDateEpochDay).format(dateFormatter)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = pillColor.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, pillColor.copy(alpha = 0.6f)),
                                    modifier = Modifier.clickable {
                                        onJumpToSliceDate(slice.scheduledDateEpochDay)
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${chain.chapterNumber}-${slice.sliceNumber} $statusEmoji",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = pillColor
                                        )
                                        Text(
                                            text = dateStr,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Show Ebbinghaus Retention Echoes if spawned for this chain
                        if (item.echoSlices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🧠 Echoes:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberMagenta
                                )
                                item.echoSlices.forEach { echo ->
                                    val echoDateStr = LocalDate.ofEpochDay(echo.scheduledDateEpochDay).format(dateFormatter)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CyberMagenta.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable {
                                            onJumpToSliceDate(echo.scheduledDateEpochDay)
                                        }
                                    ) {
                                        Text(
                                            text = "+${echo.echoIntervalDays ?: 3}d ($echoDateStr) ${if (echo.isCompleted) "✅" else "⏳"}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFEA80FC),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsAndHeatmapTab(
    state: StudyUiState,
    onJumpToSliceDate: (Long) -> Unit,
    onOpenAccountabilityModal: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "📊 ANALYTICS, HEATMAP & RETENTION ECHOES",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = CyberCyan
                )
                Text(
                    text = "12-Week GitHub weighted effort grid & HSC syllabus mastery",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Accountability, Card Sharing & Full Backup Hub Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAccountabilityModal() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🛡️ ACCOUNTABILITY & BACKUP HUB",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1-Tap 1080x1350 Cyberpunk Card • JSON Full Backup & Restore • 08:00 AM & 09:00 PM Notifications",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onOpenAccountabilityModal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ObsidianBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Open Hub 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 1. 12-Week Weighted Effort Heatmap Canvas
        item {
            EffortHeatmapCanvas(cells = state.heatmapCells)
        }

        // 2. Subject Distribution Breakdown
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🧪 HSC SUBJECT DISTRIBUTION BREAKDOWN",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = CyberCyan
                    )

                    state.subjectAnalytics.forEach { stat ->
                        val sColor = stat.subject.brandColor()
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(sColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stat.subject.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${stat.completedSlices}/${stat.totalSlices} Slices • ${stat.totalProblemsSolved} Problems",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { stat.completionFraction },
                                color = sColor,
                                trackColor = ObsidianBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }

        // 3. Upcoming Ebbinghaus Retention Echo Timeline
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🧠 UPCOMING EBBINGHAUS RETENTION ECHOES (${state.upcomingEchoes.size})",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = CyberMagenta
                    )

                    if (state.upcomingEchoes.isEmpty()) {
                        Text(
                            text = "Complete the final slice of any topic chain to spawn +2/+5/+14d or +3/+7/+21d spaced repetition echoes.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        state.upcomingEchoes.forEach { echoItem ->
                            val dateStr = LocalDate.ofEpochDay(echoItem.slice.scheduledDateEpochDay).format(dateFormatter)
                            val sColor = echoItem.chain.subject.brandColor()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianSurfaceElevated)
                                    .clickable {
                                        onJumpToSliceDate(echoItem.slice.scheduledDateEpochDay)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = echoItem.slice.shortBadge,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = sColor
                                    )
                                    Text(
                                        text = echoItem.slice.displayTitle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dateStr,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
