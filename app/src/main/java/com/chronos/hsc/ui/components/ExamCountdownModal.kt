package com.chronos.hsc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCountdownModal(
    todayEpochDay: Long,
    pinnedExam: ExamCountdownEntity?,
    allExams: List<ExamCountdownEntity>,
    onDismiss: () -> Unit,
    onSaveAndPinExam: (title: String, targetDateEpochDay: Long) -> Unit,
    onPinExam: (examId: String) -> Unit,
    onDeleteExam: (examId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var examTitle by remember { mutableStateOf("") }
    var selectedTargetEpochDay by remember { mutableLongStateOf(todayEpochDay + 90L) }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            delay(15_000L)
        }
    }

    val presetTitles = remember {
        listOf("HSC Final", "Test Exam", "Semester Final", "Admission")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⏳ EXAM COUNTDOWN COMMAND CENTER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = CyberCyan
                    )
                    Text(
                        text = "Pin any upcoming exam directly above your + Add Study Topic button",
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

            // 1. Live Big Countdown Hero Card
            LiveCountdownHeroCard(
                todayEpochDay = todayEpochDay,
                pinnedExam = pinnedExam,
                currentDateTime = currentDateTime
            )

            HorizontalDivider(color = ObsidianBorder)

            // 2. Add New Exam Timer Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ADD NEW EXAM TIMER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = CyberCyan
                )

                // Preset Title Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetTitles.forEach { preset ->
                        val isSelected = examTitle.equals(preset, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else ObsidianBackground,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else ObsidianBorder
                            ),
                            modifier = Modifier.clickable { examTitle = preset }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) CyberCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Exam Title TextField
                OutlinedTextField(
                    value = examTitle,
                    onValueChange = { examTitle = it },
                    label = { Text("Exam Title (e.g., HSC 26, Pre-Test, BUET)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyberCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Exam Date Button + Quick Offset Chips (+30d, +90d, +180d)
                val formattedSelectedDate = remember(selectedTargetEpochDay) {
                    LocalDate.ofEpochDay(selectedTargetEpochDay)
                        .format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                }
                val daysFromToday = selectedTargetEpochDay - todayEpochDay

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianBackground,
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isDatePickerOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Select Exam Date",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "TARGET EXAM DATE",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = formattedSelectedDate,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                            Text(
                                text = if (daysFromToday >= 0) "+${daysFromToday}d" else "${daysFromToday}d",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = CyberCyan
                            )
                        }
                    }
                }

                // Quick +30d, +90d, +180d test buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10L to "+10d (Urgent)", 30L to "+30d", 90L to "+90d", 180L to "+180d").forEach { (offsetDays, label) ->
                        val target = todayEpochDay + offsetDays
                        val isChosen = selectedTargetEpochDay == target
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChosen) CyberMagenta.copy(alpha = 0.22f) else ObsidianBackground,
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) CyberMagenta else ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTargetEpochDay = target }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isChosen) Color(0xFFF3C6FF) else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Save & Pin to Button CTA
                Button(
                    onClick = {
                        val finalTitle = examTitle.trim().ifEmpty { "HSC Final" }
                        onSaveAndPinExam(finalTitle, selectedTargetEpochDay)
                        examTitle = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Color(0xFF001F24)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save & Pin to Button",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }

            // 3. Saved Exams Switcher List
            if (allExams.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SAVED EXAM TIMERS (${allExams.size})",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    allExams.forEach { exam ->
                        val daysLeft = ChronoUnit.DAYS.between(
                            LocalDate.ofEpochDay(todayEpochDay),
                            LocalDate.ofEpochDay(exam.targetDateEpochDay)
                        )
                        val urgencyColor = when {
                            daysLeft > 60 -> CyberCyan
                            daysLeft in 15..60 -> CyberAmber
                            else -> CyberCrimson
                        }
                        val formattedDate = LocalDate.ofEpochDay(exam.targetDateEpochDay)
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (exam.isPrimaryPinned) {
                                urgencyColor.copy(alpha = 0.12f)
                            } else {
                                ObsidianSurfaceElevated
                            },
                            border = BorderStroke(
                                width = if (exam.isPrimaryPinned) 1.5.dp else 1.dp,
                                color = if (exam.isPrimaryPinned) urgencyColor else ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = exam.examTitle,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (exam.isPrimaryPinned) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(urgencyColor.copy(alpha = 0.22f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "📌 PINNED",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp,
                                                    color = urgencyColor
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$formattedDate • ${
                                            when {
                                                daysLeft > 0L -> "$daysLeft Days Left"
                                                daysLeft == 0L -> "🎯 EXAM TODAY!"
                                                else -> "✅ Completed (${-daysLeft}d ago)"
                                            }
                                        }",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = urgencyColor
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!exam.isPrimaryPinned) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CyberCyan.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, CyberCyan),
                                            modifier = Modifier
                                                .clickable { onPinExam(exam.examId) }
                                                .padding(end = 4.dp)
                                        ) {
                                            Text(
                                                text = "📌 Pin to Button",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = CyberCyan,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 6.dp
                                                )
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteExam(exam.examId) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Exam",
                                            tint = CyberCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (isDatePickerOpen) {
        val initialMillis = LocalDate.ofEpochDay(selectedTargetEpochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            selectedTargetEpochDay = pickedDate.toEpochDay()
                        }
                        isDatePickerOpen = false
                    }
                ) {
                    Text("Confirm Date", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun LiveCountdownHeroCard(
    todayEpochDay: Long,
    pinnedExam: ExamCountdownEntity?,
    currentDateTime: LocalDateTime
) {
    if (pinnedExam == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF161F33), ObsidianBackground)
                    )
                )
                .border(1.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⏳ NO ACTIVE EXAM PINNED YET",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create and pin your target exam below to display a live countdown badge right above your + Add Study Topic button.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    val targetDate = LocalDate.ofEpochDay(pinnedExam.targetDateEpochDay)
    val targetDateTime = targetDate.atTime(9, 0) // 9:00 AM exam start time
    val duration = Duration.between(currentDateTime, targetDateTime)

    val daysRemaining = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(todayEpochDay), targetDate)
    val totalMinutesRemaining = duration.toMinutes().coerceAtLeast(0L)
    val displayDays = (totalMinutesRemaining / (60 * 24)).coerceAtLeast(0L)
    val displayHours = ((totalMinutesRemaining / 60) % 24).coerceAtLeast(0L)
    val displayMinutes = (totalMinutesRemaining % 60).coerceAtLeast(0L)

    val totalPrepSpanDays = (pinnedExam.targetDateEpochDay - pinnedExam.createdAtEpochDay).coerceAtLeast(1L)
    val elapsedPrepDays = (todayEpochDay - pinnedExam.createdAtEpochDay).coerceIn(0L, totalPrepSpanDays)
    val prepProgress = (elapsedPrepDays.toFloat() / totalPrepSpanDays.toFloat()).coerceIn(0f, 1f)

    val urgencyColor = when {
        daysRemaining > 60L -> CyberCyan
        daysRemaining in 15L..60L -> CyberAmber
        else -> CyberCrimson
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF161F33), ObsidianBackground)
                )
            )
            .border(1.5.dp, urgencyColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📌 PINNED EXAM TARGET",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = urgencyColor
                )
                Text(
                    text = pinnedExam.examTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = urgencyColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, urgencyColor)
            ) {
                Text(
                    text = targetDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = urgencyColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Big 3-Box Countdown: Days / Hours / Minutes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CountdownUnitBox(
                value = if (daysRemaining > 0L) displayDays.toString() else "0",
                label = "DAYS",
                accentColor = urgencyColor,
                modifier = Modifier.weight(1f)
            )
            CountdownUnitBox(
                value = String.format("%02d", displayHours),
                label = "HOURS",
                accentColor = urgencyColor,
                modifier = Modifier.weight(1f)
            )
            CountdownUnitBox(
                value = String.format("%02d", displayMinutes),
                label = "MINUTES",
                accentColor = urgencyColor,
                modifier = Modifier.weight(1f)
            )
        }

        // Preparation Days Elapsed Progress Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PREPARATION TIMELINE ELAPSED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${(prepProgress * 100).toInt()}% ($elapsedPrepDays/$totalPrepSpanDays days)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = urgencyColor
                )
            }
            LinearProgressIndicator(
                progress = { prepProgress },
                color = urgencyColor,
                trackColor = ObsidianSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun CountdownUnitBox(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = accentColor
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}
