package com.chronos.hsc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Link
import com.chronos.hsc.data.store.CustomSubject
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.data.local.DistributionCurve
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSubject
import androidx.compose.foundation.shape.CircleShape
import com.chronos.hsc.data.local.HscSyllabusCatalog
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.data.local.TopicChainWithSlices
import com.chronos.hsc.data.repository.StudyRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TopicLinkOption {
    NONE,                 // Unlinked / Standalone
    LINK_EXACT_NEXT_DAY,  // Linked to previous chapter & auto-schedules to exact day after it finishes (no overlap)
    LINK_SELECTED_DATE    // Linked to previous chapter & scheduled at user's chosen date
}

private fun getChainFinishEpochDay(chainWithSlices: TopicChainWithSlices): Long {
    val maxScheduled = chainWithSlices.coreSlices.maxOfOrNull { it.scheduledDateEpochDay }
    return maxScheduled ?: (chainWithSlices.chain.createdAtEpochDay + chainWithSlices.chain.totalDays - 1)
}

private fun getChainExactNextDay(chainWithSlices: TopicChainWithSlices): Long {
    return getChainFinishEpochDay(chainWithSlices) + 1L
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHscTopicModal(
    todayEpochDay: Long,
    initialSelectedDateEpochDay: Long,
    existingChains: List<TopicChainWithSlices> = emptyList(),
    onDismiss: () -> Unit,
    onGenerateChain: (
        subject: HscSubject,
        paper: HscPaper,
        chapterNumber: Int,
        chapterName: String,
        totalDays: Int,
        startDateEpochDay: Long,
        distributionCurve: DistributionCurve,
        dailyProblemTarget: Int,
        energyLevel: EnergyLevel,
        linkedPreviousChainId: String?,
        customSubjectId: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customSubjectsList by remember { mutableStateOf(CustomSubjectAndAiStore.getCustomSubjects()) }
    var selectedCustomSubject by remember { mutableStateOf<CustomSubject?>(null) }
    var showCreateCustomDialog by remember { mutableStateOf(false) }
    var customSubjectToDelete by remember { mutableStateOf<CustomSubject?>(null) }

    var selectedSubject by remember { mutableStateOf(HscSubject.PHYSICS) }
    var selectedPaper by remember { mutableStateOf(HscPaper.FIRST_PAPER) }
    var selectedChapterNumber by remember { mutableIntStateOf(1) }
    var chapterTitle by remember {
        mutableStateOf(
            HscSyllabusCatalog.getChapterNameBangla(
                HscSubject.PHYSICS,
                HscPaper.FIRST_PAPER,
                1
            )
        )
    }
    var selectedDays by remember { mutableIntStateOf(4) }
    var isDaysDropdownExpanded by remember { mutableStateOf(false) }
    var isChapterDropdownExpanded by remember { mutableStateOf(false) }

    var userChosenDateEpochDay by remember { mutableLongStateOf(initialSelectedDateEpochDay) }
    var startDateEpochDay by remember { mutableLongStateOf(initialSelectedDateEpochDay) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedCurve by remember { mutableStateOf(DistributionCurve.CONCEPT_TO_PROBLEM) }
    var dailyProblems by remember { mutableIntStateOf(5) }
    var selectedEnergy by remember { mutableStateOf(EnergyLevel.HIGH_FOCUS) }

    val isSubjectSinglePaper = if (selectedCustomSubject != null) {
        !selectedCustomSubject!!.hasSecondPaper
    } else {
        selectedSubject.isSinglePaperOnly
    }
    val effectivePaper = if (isSubjectSinglePaper) HscPaper.SINGLE else selectedPaper
    val availableChapters = remember(selectedSubject, effectivePaper, selectedCustomSubject) {
        if (selectedCustomSubject != null) {
            (1..15).map { "অধ্যায় ${HscSyllabusCatalog.toBanglaDigits(it)}" }
        } else {
            HscSyllabusCatalog.getChaptersFor(selectedSubject, effectivePaper)
        }
    }
    val subjectColor = remember(selectedSubject, selectedCustomSubject) {
        if (selectedCustomSubject != null) {
            Color(selectedCustomSubject!!.colorHex)
        } else {
            selectedSubject.brandColor()
        }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    val matchingChains = remember(existingChains, selectedSubject, selectedCustomSubject, effectivePaper) {
        if (selectedCustomSubject != null) {
            existingChains.filter { chainWithSlices ->
                CustomSubjectAndAiStore.getCustomSubjectForChain(chainWithSlices.chain.chainId)?.id == selectedCustomSubject?.id &&
                    chainWithSlices.chain.paper == effectivePaper
            }
        } else {
            existingChains.filter { chainWithSlices ->
                chainWithSlices.chain.subject == selectedSubject &&
                    chainWithSlices.chain.paper == effectivePaper &&
                    CustomSubjectAndAiStore.getCustomSubjectForChain(chainWithSlices.chain.chainId) == null
            }
        }
    }
    var selectedLinkOption by remember { mutableStateOf(TopicLinkOption.NONE) }
    var selectedLinkedChainId by remember { mutableStateOf<String?>(null) }
    var isTestPaperSolving by remember { mutableStateOf(false) }

    LaunchedEffect(matchingChains) {
        if (matchingChains.isNotEmpty()) {
            if (selectedLinkedChainId == null || matchingChains.none { it.chain.chainId == selectedLinkedChainId }) {
                val latestFinishing = matchingChains.maxByOrNull { getChainFinishEpochDay(it) } ?: matchingChains.last()
                selectedLinkedChainId = latestFinishing.chain.chainId
            }
        } else {
            selectedLinkedChainId = null
            selectedLinkOption = TopicLinkOption.NONE
            startDateEpochDay = userChosenDateEpochDay
        }
    }

    val selectedTargetChain = remember(matchingChains, selectedLinkedChainId) {
        matchingChains.find { it.chain.chainId == selectedLinkedChainId }
            ?: matchingChains.maxByOrNull { getChainFinishEpochDay(it) }
            ?: matchingChains.lastOrNull()
    }

    val targetFinishEpochDay = selectedTargetChain?.let { getChainFinishEpochDay(it) } ?: todayEpochDay
    val exactNextDayEpochDay = targetFinishEpochDay + 1L
    val targetFinishFormatted = remember(targetFinishEpochDay) {
        LocalDate.ofEpochDay(targetFinishEpochDay).format(dateFormatter)
    }
    val exactNextDayFormatted = remember(exactNextDayEpochDay) {
        LocalDate.ofEpochDay(exactNextDayEpochDay).format(dateFormatter)
    }
    val userChosenDateFormatted = remember(userChosenDateEpochDay) {
        LocalDate.ofEpochDay(userChosenDateEpochDay).format(dateFormatter)
    }

    LaunchedEffect(selectedLinkOption, exactNextDayEpochDay, userChosenDateEpochDay) {
        when (selectedLinkOption) {
            TopicLinkOption.LINK_EXACT_NEXT_DAY -> {
                startDateEpochDay = exactNextDayEpochDay
            }
            TopicLinkOption.LINK_SELECTED_DATE, TopicLinkOption.NONE -> {
                startDateEpochDay = userChosenDateEpochDay
            }
        }
    }

    if (showDatePickerDialog) {
        key(startDateEpochDay) {
            val initialMillis = LocalDate.ofEpochDay(startDateEpochDay)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val pickedDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                val pickedEpochDay = pickedDate.toEpochDay()
                                userChosenDateEpochDay = pickedEpochDay
                                startDateEpochDay = pickedEpochDay
                                if (selectedLinkOption == TopicLinkOption.LINK_EXACT_NEXT_DAY) {
                                    selectedLinkOption = TopicLinkOption.LINK_SELECTED_DATE
                                }
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("Confirm", color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚡ NEW HSC SYLLABUS BOSS CHAIN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Auto-slices multi-day chapters with Relational Cascade links",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Subject Selector
            SectionLabel("1. HSC SUBJECT")
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HscSubject.entries.forEach { subject ->
                    val isSelected = selectedCustomSubject == null && selectedSubject == subject
                    val sColor = subject.brandColor()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) sColor.copy(alpha = 0.22f) else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) sColor else ObsidianBorder
                        ),
                        modifier = Modifier.clickable {
                            selectedCustomSubject = null
                            selectedSubject = subject
                            val nextPaper = when {
                                subject.isSinglePaperOnly -> HscPaper.SINGLE
                                selectedPaper == HscPaper.SINGLE -> HscPaper.FIRST_PAPER
                                else -> selectedPaper
                            }
                            selectedPaper = nextPaper
                            val nextChapters = HscSyllabusCatalog.getChaptersFor(subject, nextPaper)
                            if (selectedChapterNumber > nextChapters.size) {
                                selectedChapterNumber = 1
                            }
                            chapterTitle = HscSyllabusCatalog.getChapterNameBangla(
                                subject,
                                nextPaper,
                                selectedChapterNumber
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject.shortCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = sColor,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subject.fullName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // User-Created Custom Subjects
                customSubjectsList.forEach { customSub ->
                    val isSelected = selectedCustomSubject?.id == customSub.id
                    val csColor = Color(customSub.colorHex)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) csColor.copy(alpha = 0.22f) else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) csColor else ObsidianBorder
                        ),
                        modifier = Modifier.clickable {
                            selectedCustomSubject = customSub
                            selectedSubject = HscSubject.ICT
                            selectedPaper = if (customSub.hasSecondPaper) HscPaper.FIRST_PAPER else HscPaper.SINGLE
                            selectedChapterNumber = 1
                            chapterTitle = "অধ্যায় ১"
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = customSub.shortBadge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = csColor,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = customSub.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { customSubjectToDelete = customSub },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete ${customSub.name}",
                                    tint = TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // High-Visibility [+ Add Subject] Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CyberCyan.copy(alpha = 0.08f),
                    border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.8f)),
                    modifier = Modifier.clickable {
                        showCreateCustomDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Subject",
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "+ Add Subject",
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberCyan,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Paper Selector
            SectionLabel("2. PAPER SELECTION")
            Spacer(modifier = Modifier.height(6.dp))
            if (isSubjectSinglePaper) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = subjectColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, subjectColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedCustomSubject != null) {
                            "${selectedCustomSubject!!.name} uses Single Paper automatically"
                        } else {
                            "ICT uses Single Paper automatically"
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subjectColor
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(HscPaper.FIRST_PAPER, HscPaper.SECOND_PAPER).forEach { paper ->
                        val isSelected = effectivePaper == paper
                        val paperLabel = if (selectedCustomSubject != null) {
                            "${paper.fullName} (${selectedCustomSubject!!.shortBadge}${paper.shortCode})"
                        } else {
                            "${paper.fullName} (${selectedSubject.shortCode}${paper.shortCode})"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) subjectColor.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) subjectColor else ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedPaper = paper
                                    if (selectedCustomSubject == null) {
                                        val nextChapters = HscSyllabusCatalog.getChaptersFor(selectedSubject, paper)
                                        if (selectedChapterNumber > nextChapters.size) {
                                            selectedChapterNumber = 1
                                        }
                                        chapterTitle = HscSyllabusCatalog.getChapterNameBangla(
                                            selectedSubject,
                                            paper,
                                            selectedChapterNumber
                                        )
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = paperLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) subjectColor else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Exact HSC Chapter (in Bangla) & Days to Continue Dropdowns
            SectionLabel("3. HSC CHAPTER (বাংলা) & DAYS TO CONTINUE")
            Spacer(modifier = Modifier.height(6.dp))

            // Full-width HSC Chapter Dropdown showing exact Bangla chapter names
            ExposedDropdownMenuBox(
                expanded = isChapterDropdownExpanded,
                onExpandedChange = { isChapterDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedCustomSubject != null) {
                        "অধ্যায় ${HscSyllabusCatalog.toBanglaDigits(selectedChapterNumber)}"
                    } else {
                        HscSyllabusCatalog.getDropdownChapterLabel(
                            selectedSubject,
                            effectivePaper,
                            selectedChapterNumber
                        )
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("HSC অধ্যায় নির্বাচন করুন (Select Chapter)") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isChapterDropdownExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = subjectColor,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isChapterDropdownExpanded,
                    onDismissRequest = { isChapterDropdownExpanded = false },
                    modifier = Modifier.background(ObsidianSurfaceElevated)
                ) {
                    availableChapters.forEachIndexed { idx, banglaName ->
                        val chNum = idx + 1
                        val label = "অধ্যায় ${HscSyllabusCatalog.toBanglaDigits(chNum)}: $banglaName"
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (chNum == selectedChapterNumber) subjectColor else TextPrimary,
                                    fontWeight = if (chNum == selectedChapterNumber) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedChapterNumber = chNum
                                chapterTitle = banglaName
                                isChapterDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days to Continue Dropdown (1..15 Days)
            ExposedDropdownMenuBox(
                expanded = isDaysDropdownExpanded,
                onExpandedChange = { isDaysDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedDays == 1) "1 Day (1 Slice)" else "$selectedDays Days ($selectedDays Slices)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Days to Continue") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDaysDropdownExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isDaysDropdownExpanded,
                    onDismissRequest = { isDaysDropdownExpanded = false },
                    modifier = Modifier.background(ObsidianSurfaceElevated)
                ) {
                    (1..15).forEach { days ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (days == 1) "1 Day (Single Slice)" else "$days Days ($days Daily Slices • ${days * 100} HP)",
                                    color = if (days == selectedDays) CyberCyan else TextPrimary,
                                    fontWeight = if (days == selectedDays) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedDays = days
                                isDaysDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chapter Topic Name Input (Auto-populated with Bangla chapter name, editable)
            OutlinedTextField(
                value = chapterTitle,
                onValueChange = { chapterTitle = it },
                label = { Text("অধ্যায়ের নাম / নির্দিষ্ট টপিক (Chapter Title in Bangla)") },
                placeholder = { Text("যেমন: ভৌত জগৎ ও পরিমাপ, ভেক্টর, টেস্ট পেপার সমাধান...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = subjectColor,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Suffix/Type Selector: Test Paper Solving (টেস্ট পেপার সমাধান), Board CQ/MCQ, etc.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isTestPaperSolving) subjectColor.copy(alpha = 0.22f) else ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, if (isTestPaperSolving) subjectColor else ObsidianBorder),
                    modifier = Modifier.clickable {
                        isTestPaperSolving = !isTestPaperSolving
                        val tag = " (টেস্ট পেপার সমাধান)"
                        if (isTestPaperSolving) {
                            if (!chapterTitle.contains("টেস্ট পেপার")) {
                                chapterTitle = "${chapterTitle.trim()}$tag"
                            }
                        } else {
                            chapterTitle = chapterTitle.replace(tag, "").replace(" (টেস্ট পেপার সমাধান)", "").trim()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTestPaperSolving) "✓ টেস্ট পেপার সমাধান" else "📝 টেস্ট পেপার সমাধান (Test Paper)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (isTestPaperSolving) subjectColor else TextPrimary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.clickable {
                        val tag = " [বোর্ড CQ/MCQ]"
                        if (!chapterTitle.contains("বোর্ড")) {
                            chapterTitle = "${chapterTitle.trim()}$tag"
                        }
                    }
                ) {
                    Text(
                        text = "+ বোর্ড CQ/MCQ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            // Link with Previous Chapter Chain (Sequential Pipeline & Domino Cascading)
            if (matchingChains.isNotEmpty() && selectedTargetChain != null) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionLabel("LINK WITH PREVIOUS CHAPTER CHAIN")
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Context badge showing which previous chapter is being linked
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = if (selectedLinkOption != TopicLinkOption.NONE) subjectColor else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Previous Paper Topic Detected:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = when (selectedLinkOption) {
                                        TopicLinkOption.LINK_EXACT_NEXT_DAY -> "EXACT NEXT DAY 🔗"
                                        TopicLinkOption.LINK_SELECTED_DATE -> "CUSTOM DATE 🔗"
                                        TopicLinkOption.NONE -> "NOT LINKED"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = if (selectedLinkOption != TopicLinkOption.NONE) subjectColor else TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            val prevTitle = selectedTargetChain.chain.chapterName.ifEmpty { "অধ্যায় ${selectedTargetChain.chain.chapterNumber}" }
                            Text(
                                text = "• $prevTitle (${selectedTargetChain.chain.totalDays} days • Finishes: $targetFinishFormatted)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            // If more than 1 chain exists for this paper, let the student choose which one to link
                            if (matchingChains.size > 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Choose which previous chapter to chain from:",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    matchingChains.forEach { chainOption ->
                                        val isThisTarget = chainOption.chain.chainId == selectedLinkedChainId
                                        val optFinish = getChainFinishEpochDay(chainOption)
                                        val optFinishStr = LocalDate.ofEpochDay(optFinish).format(dateFormatter)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isThisTarget) subjectColor.copy(alpha = 0.22f) else ObsidianBackground,
                                            border = BorderStroke(1.dp, if (isThisTarget) subjectColor else ObsidianBorder),
                                            modifier = Modifier.clickable {
                                                selectedLinkedChainId = chainOption.chain.chainId
                                            }
                                        ) {
                                            Text(
                                                text = "${chainOption.chain.chapterName.ifEmpty { "অধ্যায় ${chainOption.chain.chapterNumber}" }} (Ends $optFinishStr)",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isThisTarget) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isThisTarget) TextPrimary else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Option 1: "Link with Exact Date" Button
                    val isExactActive = selectedLinkOption == TopicLinkOption.LINK_EXACT_NEXT_DAY
                    val prevName = selectedTargetChain.chain.chapterName.ifEmpty { "অধ্যায় ${selectedTargetChain.chain.chapterNumber}" }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isExactActive) subjectColor.copy(alpha = 0.16f) else ObsidianSurfaceElevated,
                        border = BorderStroke(1.5.dp, if (isExactActive) subjectColor else ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLinkOption = if (isExactActive) {
                                    TopicLinkOption.NONE
                                } else {
                                    TopicLinkOption.LINK_EXACT_NEXT_DAY
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isExactActive) subjectColor else ObsidianBackground,
                                    border = BorderStroke(1.dp, if (isExactActive) subjectColor else ObsidianBorder),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isExactActive) {
                                            Text("✓", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ObsidianBackground)
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Link with exact date (No Overlap)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isExactActive) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        text = "Starts on $exactNextDayFormatted — the day after $prevName finishes ($targetFinishFormatted)",
                                        fontSize = 10.5.sp,
                                        color = if (isExactActive) subjectColor else TextMuted
                                    )
                                }
                            }
                            if (isExactActive) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = subjectColor
                                )
                            }
                        }
                    }

                    // Option 2: "Link without Exact Date" (Link at Selected Date) Button
                    val isCustomActive = selectedLinkOption == TopicLinkOption.LINK_SELECTED_DATE
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCustomActive) subjectColor.copy(alpha = 0.16f) else ObsidianSurfaceElevated,
                        border = BorderStroke(1.5.dp, if (isCustomActive) subjectColor else ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLinkOption = if (isCustomActive) {
                                    TopicLinkOption.NONE
                                } else {
                                    TopicLinkOption.LINK_SELECTED_DATE
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCustomActive) subjectColor else ObsidianBackground,
                                    border = BorderStroke(1.dp, if (isCustomActive) subjectColor else ObsidianBorder),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCustomActive) {
                                            Text("✓", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ObsidianBackground)
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Link without exact date (At Selected Date)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isCustomActive) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        text = "Starts on your chosen date ($userChosenDateFormatted) & stays linked for domino shift cascade",
                                        fontSize = 10.5.sp,
                                        color = if (isCustomActive) subjectColor else TextMuted
                                    )
                                }
                            }
                            if (isCustomActive) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = subjectColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Start Date Picker (Quick Chips: Today, Tomorrow, Pick Date)
            SectionLabel("4. START DATE")
            Spacer(modifier = Modifier.height(6.dp))

            if (selectedLinkOption == TopicLinkOption.LINK_EXACT_NEXT_DAY) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = subjectColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = subjectColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Auto-set to: $exactNextDayFormatted (Day after previous chapter)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isTodaySelected = startDateEpochDay == todayEpochDay
                val isTomorrowSelected = startDateEpochDay == todayEpochDay + 1L
                val isCustomDateSelected = !isTodaySelected && !isTomorrowSelected

                // Today Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isTodaySelected) CyberCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, if (isTodaySelected) CyberCyan else ObsidianBorder),
                    modifier = Modifier.clickable {
                        userChosenDateEpochDay = todayEpochDay
                        startDateEpochDay = todayEpochDay
                        if (selectedLinkOption == TopicLinkOption.LINK_EXACT_NEXT_DAY) {
                            selectedLinkOption = TopicLinkOption.LINK_SELECTED_DATE
                        }
                    }
                ) {
                    Text(
                        text = "Today",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = if (isTodaySelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTodaySelected) CyberCyan else TextSecondary
                    )
                }

                // Tomorrow Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isTomorrowSelected) CyberCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, if (isTomorrowSelected) CyberCyan else ObsidianBorder),
                    modifier = Modifier.clickable {
                        userChosenDateEpochDay = todayEpochDay + 1L
                        startDateEpochDay = todayEpochDay + 1L
                        if (selectedLinkOption == TopicLinkOption.LINK_EXACT_NEXT_DAY) {
                            selectedLinkOption = TopicLinkOption.LINK_SELECTED_DATE
                        }
                    }
                ) {
                    Text(
                        text = "Tomorrow",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = if (isTomorrowSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTomorrowSelected) CyberCyan else TextSecondary
                    )
                }

                // Pick Date Chip (Opens Material 3 DatePickerDialog)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCustomDateSelected) CyberCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, if (isCustomDateSelected) CyberCyan else ObsidianBorder),
                    modifier = Modifier.clickable { showDatePickerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = if (isCustomDateSelected) CyberCyan else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCustomDateSelected) {
                                "Picked: ${LocalDate.ofEpochDay(startDateEpochDay).format(dateFormatter)}"
                            } else {
                                "Pick Date"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isCustomDateSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCustomDateSelected) CyberCyan else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Distribution Curve Toggle
            SectionLabel("5. WORKLOAD DISTRIBUTION CURVE")
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DistributionCurve.entries.forEach { curve ->
                    val isSelected = selectedCurve == curve
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyberCyan.copy(alpha = 0.14f) else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) CyberCyan else ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCurve = curve }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = curve.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) CyberCyan else TextPrimary
                            )
                            Text(
                                text = curve.subtitle,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Daily Problem Sub-Counter Target Stepper
            SectionLabel("6. DAILY PROBLEM SUB-COUNTER TARGET")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$dailyProblems Problems / Day",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Total chain problem target: ${dailyProblems * selectedDays} problems",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { dailyProblems = (dailyProblems - 1).coerceAtLeast(0) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Problems",
                                tint = CyberCyan
                            )
                        }
                        Text(
                            text = "$dailyProblems",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { dailyProblems = (dailyProblems + 1).coerceAtMost(30) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Problems",
                                tint = CyberCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8. Mental Battery Requirement
            SectionLabel("7. MENTAL BATTERY REQUIREMENT")
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnergyLevel.entries.forEach { energy ->
                    val isSelected = selectedEnergy == energy
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CyberEmerald.copy(alpha = 0.18f) else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyberEmerald else ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedEnergy = energy }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${energy.batteryPercent}%",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (isSelected) CyberEmerald else TextPrimary
                            )
                            Text(
                                text = energy.label.substringAfter(" "),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 9. Live Auto-Slice Preview Box
            val previewTopic = chapterTitle.trim()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBackground)
                    .border(1.dp, subjectColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AUTO-SLICE PREVIEW ($selectedDays SLICES)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = subjectColor
                        )
                        Text(
                            text = "⚔️ BOSS HP: ${selectedDays * 100}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val previewCount = selectedDays.coerceAtMost(4)
                    (1..previewCount).forEach { idx ->
                        val dateStr = LocalDate.ofEpochDay(startDateEpochDay + (idx - 1).toLong()).format(dateFormatter)
                        val titleStr = if (selectedCustomSubject != null) {
                            val paperSuffix = if (selectedCustomSubject!!.hasSecondPaper && effectivePaper != HscPaper.SINGLE) " ${effectivePaper.compactTitleLabel}" else ""
                            val bnCh = HscSyllabusCatalog.toBanglaDigits(selectedChapterNumber)
                            val bnSlice = HscSyllabusCatalog.toBanglaDigits(idx)
                            "${selectedCustomSubject!!.name}$paperSuffix • অধ্যায় $bnCh ($bnSlice): $previewTopic"
                        } else {
                            StudyRepository.formatSliceDisplayTitle(
                                selectedSubject,
                                effectivePaper,
                                selectedChapterNumber,
                                idx,
                                previewTopic
                            )
                        }
                        val phaseStr = selectedCurve.phaseTagForDay(idx, selectedDays)
                        Text(
                            text = "• [$dateStr] $titleStr ($phaseStr)",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                    if (selectedDays > 4) {
                        Text(
                            text = "  ... + ${selectedDays - 4} more daily slice(s) up to Chapter $selectedChapterNumber-$selectedDays",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Study Chain CTA Button
            Button(
                onClick = {
                    onGenerateChain(
                        selectedSubject,
                        effectivePaper,
                        selectedChapterNumber,
                        chapterTitle,
                        selectedDays,
                        startDateEpochDay,
                        selectedCurve,
                        dailyProblems,
                        selectedEnergy,
                        if (selectedLinkOption != TopicLinkOption.NONE) selectedLinkedChainId else null,
                        selectedCustomSubject?.id
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = subjectColor,
                    contentColor = Color(0xFF050811)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Study Chain ($selectedDays Slices)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showCreateCustomDialog) {
        CreateCustomSubjectDialog(
            onDismiss = { showCreateCustomDialog = false },
            onSubjectCreated = { created ->
                showCreateCustomDialog = false
                customSubjectsList = CustomSubjectAndAiStore.getCustomSubjects()
                selectedCustomSubject = created
                selectedSubject = HscSubject.ICT
                selectedPaper = if (created.hasSecondPaper) HscPaper.FIRST_PAPER else HscPaper.SINGLE
                selectedChapterNumber = 1
                chapterTitle = "অধ্যায় ১"
            }
        )
    }

    if (customSubjectToDelete != null) {
        val target = customSubjectToDelete
        AlertDialog(
            onDismissRequest = { customSubjectToDelete = null },
            title = {
                Text("Delete Custom Subject?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Remove '${target?.name}' from your subject list? Already scheduled topic chains and tasks in your Battleground will NOT be deleted.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (target != null) {
                            CustomSubjectAndAiStore.deleteCustomSubject(target.id)
                            customSubjectsList = CustomSubjectAndAiStore.getCustomSubjects()
                            if (selectedCustomSubject?.id == target.id) {
                                selectedCustomSubject = null
                                selectedSubject = HscSubject.PHYSICS
                                selectedPaper = HscPaper.FIRST_PAPER
                                selectedChapterNumber = 1
                                chapterTitle = HscSyllabusCatalog.getChapterNameBangla(HscSubject.PHYSICS, HscPaper.FIRST_PAPER, 1)
                            }
                        }
                        customSubjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customSubjectToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        color = CyberCyan
    )
}
