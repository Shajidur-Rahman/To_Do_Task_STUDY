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
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHscTopicModal(
    todayEpochDay: Long,
    initialSelectedDateEpochDay: Long,
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
        energyLevel: EnergyLevel
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedSubject by remember { mutableStateOf(HscSubject.PHYSICS) }
    var selectedPaper by remember { mutableStateOf(HscPaper.FIRST_PAPER) }
    var selectedChapterNumber by remember { mutableIntStateOf(1) }
    var chapterTitle by remember { mutableStateOf("") }
    var selectedDays by remember { mutableIntStateOf(4) }
    var isDaysDropdownExpanded by remember { mutableStateOf(false) }
    var isChapterDropdownExpanded by remember { mutableStateOf(false) }
    var startDateEpochDay by remember { mutableLongStateOf(initialSelectedDateEpochDay) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedCurve by remember { mutableStateOf(DistributionCurve.CONCEPT_TO_PROBLEM) }
    var dailyProblems by remember { mutableIntStateOf(5) }
    var selectedEnergy by remember { mutableStateOf(EnergyLevel.HIGH_FOCUS) }

    val effectivePaper = if (selectedSubject.isSinglePaperOnly) HscPaper.SINGLE else selectedPaper
    val subjectColor = selectedSubject.brandColor()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

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
                                startDateEpochDay = pickedDate.toEpochDay()
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
                    val isSelected = selectedSubject == subject
                    val sColor = subject.brandColor()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) sColor.copy(alpha = 0.22f) else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) sColor else ObsidianBorder
                        ),
                        modifier = Modifier.clickable {
                            selectedSubject = subject
                            if (subject.isSinglePaperOnly) {
                                selectedPaper = HscPaper.SINGLE
                            } else if (selectedPaper == HscPaper.SINGLE) {
                                selectedPaper = HscPaper.FIRST_PAPER
                            }
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Paper Selector
            SectionLabel("2. PAPER SELECTION")
            Spacer(modifier = Modifier.height(6.dp))
            if (selectedSubject.isSinglePaperOnly) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = subjectColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, subjectColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ICT uses Single Paper automatically",
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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) subjectColor.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) subjectColor else ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPaper = paper }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${paper.fullName} (${selectedSubject.shortCode}${paper.shortCode})",
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

            // 3. Chapter Number & Days to Continue Dropdowns
            SectionLabel("3. CHAPTER NUMBER & DAYS TO CONTINUE")
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Chapter Number Dropdown (1..15)
                ExposedDropdownMenuBox(
                    expanded = isChapterDropdownExpanded,
                    onExpandedChange = { isChapterDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Chapter $selectedChapterNumber",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chapter No.") },
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
                        (1..15).forEach { chNum ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Chapter $chNum",
                                        color = if (chNum == selectedChapterNumber) subjectColor else TextPrimary,
                                        fontWeight = if (chNum == selectedChapterNumber) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedChapterNumber = chNum
                                    isChapterDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Days to Continue Dropdown (1..15 Days)
                ExposedDropdownMenuBox(
                    expanded = isDaysDropdownExpanded,
                    onExpandedChange = { isDaysDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chapter Topic Name Input
            OutlinedTextField(
                value = chapterTitle,
                onValueChange = { chapterTitle = it },
                label = { Text("Chapter Title / Topic Name (Optional)") },
                placeholder = { Text("e.g., Oparichita, Modifiers, Vector, Organic Chemistry") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = subjectColor,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Start Date Picker (Quick Chips: Today, Tomorrow, Pick Date)
            SectionLabel("4. START DATE")
            Spacer(modifier = Modifier.height(6.dp))
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
                    modifier = Modifier.clickable { startDateEpochDay = todayEpochDay }
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
                    modifier = Modifier.clickable { startDateEpochDay = todayEpochDay + 1L }
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
                            text = "👹 BOSS HP: ${selectedDays * 100}",
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
                        val titleStr = StudyRepository.formatSliceDisplayTitle(
                            selectedSubject,
                            effectivePaper,
                            selectedChapterNumber,
                            idx,
                            previewTopic
                        )
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
                        selectedEnergy
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
