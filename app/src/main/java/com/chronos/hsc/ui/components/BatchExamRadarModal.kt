package com.chronos.hsc.ui.components

import android.app.DatePickerDialog
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.service.GeminiRouletteService
import com.chronos.hsc.data.store.BatchExamRecord
import com.chronos.hsc.data.store.BatchExamStore
import com.chronos.hsc.data.store.CustomSubject
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.ExamFormatMode
import com.chronos.hsc.ui.theme.AppThemeMode
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.LocalAppThemeMode
import com.chronos.hsc.ui.theme.NatureAutumnOrange
import com.chronos.hsc.ui.theme.NatureLagRust
import com.chronos.hsc.ui.theme.NatureLeafGreen
import com.chronos.hsc.ui.theme.NatureSproutLime
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TerrariumBackground
import com.chronos.hsc.ui.theme.TerrariumBorder
import com.chronos.hsc.ui.theme.TerrariumSurface
import com.chronos.hsc.ui.theme.TerrariumSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun BatchExamFloatingButton(
    activeExams: List<BatchExamRecord>,
    todayEpochDay: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNature = LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER
    val view = LocalView.current
    val mostUrgent = activeExams.firstOrNull()

    val baseBorderColor = if (isNature) NatureAutumnOrange else Color(0xFFE040FB)
    val alertCrimson = if (isNature) NatureLagRust else Color(0xFFFF1744)

    val daysLeft = mostUrgent?.daysLeft(todayEpochDay) ?: 999L
    val isUrgent = mostUrgent != null && daysLeft <= 2L && mostUrgent.remainingStudyHours > 0f

    val infiniteTransition = rememberInfiniteTransition(label = "batchExamPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val surfaceBg = if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated
    val borderColor = if (isUrgent) {
        alertCrimson.copy(alpha = pulseAlpha)
    } else {
        baseBorderColor.copy(alpha = 0.8f)
    }

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = surfaceBg,
        border = BorderStroke(if (isUrgent) 2.dp else 1.2.dp, borderColor),
        shadowElevation = if (isUrgent) 8.dp else 4.dp,
        modifier = modifier
    ) {
        if (mostUrgent == null) {
            // No upcoming exams state
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📝",
                    fontSize = 15.sp
                )
                Text(
                    text = "+ Batch Exam",
                    color = if (isNature) NatureAutumnOrange else Color(0xFFE040FB),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            // 1 or more upcoming exams: 2-line tactical format
            val daysText = when {
                daysLeft == 0L -> "🎯 EXAM TODAY"
                daysLeft < 0L -> "⚠️ ${-daysLeft}d Overdue"
                else -> "📝 ${daysLeft}d Left"
            }
            val moreBadge = if (activeExams.size > 1) " [+${activeExams.size - 1}]" else ""
            val line1 = "$daysText • ${mostUrgent.formatSummary}$moreBadge"
            val reqHoursStr = String.format(Locale.US, "%.1f", mostUrgent.remainingStudyHours)
            val line2 = "${mostUrgent.shortSubject} ${mostUrgent.chapterNumbersAndTitles.take(12)} • Need ~${reqHoursStr}h"

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = line1,
                        color = if (isUrgent) alertCrimson else if (isNature) NatureAutumnOrange else Color(0xFFE040FB),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = line2,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchExamRadarModal(
    activeExams: List<BatchExamRecord>,
    todayEpochDay: Long,
    onDismiss: () -> Unit,
    onSaveExam: (BatchExamRecord, autoCreateSlices: Boolean) -> Unit,
    onDeleteExam: (String) -> Unit,
    onMarkCompleted: (String, Boolean) -> Unit,
    onIncrementStudyHours: (String, Float) -> Unit,
    onAutoCreateSlicesForExam: (BatchExamRecord) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val isNature = LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER

    val customSubjects = remember { CustomSubjectAndAiStore.getCustomSubjects() }
    val defaultHscSubjects = remember {
        listOf(
            HscSubject.PHYSICS,
            HscSubject.CHEMISTRY,
            HscSubject.HIGHER_MATH,
            HscSubject.BIOLOGY,
            HscSubject.BANGLA,
            HscSubject.ENGLISH,
            HscSubject.ICT
        )
    }

    var selectedCustomSubject by remember { mutableStateOf<CustomSubject?>(null) }
    var selectedDefaultSubject by remember { mutableStateOf(HscSubject.PHYSICS) }

    var paperMode by remember { mutableStateOf("1st Paper") }
    val selectedChapterNumbers = remember { mutableStateOf(setOf(1)) }
    var chapterTopicInput by remember { mutableStateOf("") }
    var coachingNameInput by remember { mutableStateOf("Private Batch") }

    var examDateEpochDay by remember { mutableLongStateOf(todayEpochDay + 3) }
    var examFormatMode by remember { mutableStateOf(ExamFormatMode.MCQ_AND_CQ_BOTH) }
    var mcqCount by remember { mutableIntStateOf(30) }
    var cqCount by remember { mutableIntStateOf(2) }

    var autoCreateSlices by remember { mutableStateOf(true) }
    var isAnalyzingAi by remember { mutableStateOf(false) }

    val activeCount = activeExams.size
    val currentSubjectName = selectedCustomSubject?.name ?: selectedDefaultSubject.fullName

    // Calendar Picker launcher
    val showDatePicker = {
        val cal = Calendar.getInstance()
        val curDate = LocalDate.ofEpochDay(examDateEpochDay)
        cal.set(curDate.year, curDate.monthValue - 1, curDate.dayOfMonth)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = LocalDate.of(year, month + 1, dayOfMonth)
                examDateEpochDay = chosen.toEpochDay()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val primaryBg = if (isNature) TerrariumSurface else ObsidianSurface
    val cardBg = if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated
    val borderColor = if (isNature) TerrariumBorder else ObsidianBorder
    val accentViolet = if (isNature) NatureAutumnOrange else Color(0xFFE040FB)
    val accentCyan = if (isNature) NatureSproutLime else CyberCyan

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = primaryBg,
            border = BorderStroke(1.5.dp, accentViolet),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📝", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "BATCH EXAM RADAR",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = accentViolet,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Private/Coaching MCQ/CQ Weight Analyzer",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 1: Active Coaching / Batch Exams List
                if (activeExams.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE RADAR QUEUE ($activeCount)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = accentCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        activeExams.forEach { exam ->
                            BatchExamCard(
                                exam = exam,
                                todayEpochDay = todayEpochDay,
                                isNature = isNature,
                                onIncrementStudyHours = { onIncrementStudyHours(exam.examId, 0.5f) },
                                onAutoCreateSlices = { onAutoCreateSlicesForExam(exam) },
                                onMarkCompleted = { onMarkCompleted(exam.examId, true) },
                                onDelete = { onDeleteExam(exam.examId) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(borderColor)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // SECTION 2: Add New Exam Form
                Text(
                    text = "➕ SCHEDULE NEW BATCH EXAM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = accentViolet,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Subject Selector Chips (HSC + Custom)
                Text(
                    text = "Select Subject:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    defaultHscSubjects.forEach { sub ->
                        val isSelected = selectedCustomSubject == null && selectedDefaultSubject == sub
                        val chipBg = if (isSelected) accentViolet.copy(alpha = 0.25f) else cardBg
                        val chipBorder = if (isSelected) accentViolet else borderColor

                        Surface(
                            onClick = {
                                selectedCustomSubject = null
                                selectedDefaultSubject = sub
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorder)
                        ) {
                            Text(
                                text = sub.fullName,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }

                    customSubjects.forEach { custom ->
                        val isSelected = selectedCustomSubject?.id == custom.id
                        val customColor = Color(custom.colorHex)
                        val chipBg = if (isSelected) customColor.copy(alpha = 0.25f) else cardBg
                        val chipBorder = if (isSelected) customColor else borderColor

                        Surface(
                            onClick = { selectedCustomSubject = custom },
                            shape = RoundedCornerShape(8.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorder)
                        ) {
                            Text(
                                text = "⭐ ${custom.name}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Paper Selector
                val isSinglePaper = selectedCustomSubject?.isSinglePaperOnly == true ||
                        (selectedCustomSubject == null && selectedDefaultSubject.isSinglePaperOnly)

                if (!isSinglePaper) {
                    Text(
                        text = "Paper:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1st Paper", "2nd Paper").forEach { p ->
                            val isSelected = paperMode == p
                            OutlinedButton(
                                onClick = { paperMode = p },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) accentViolet.copy(alpha = 0.2f) else cardBg
                                ),
                                border = BorderStroke(1.dp, if (isSelected) accentViolet else borderColor)
                            ) {
                                Text(
                                    text = p,
                                    fontSize = 11.sp,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 3. Chapter Multi-Select Chips
                Text(
                    text = "Chapter Numbers (Multi-Select):",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..12).forEach { ch ->
                        val isSelected = selectedChapterNumbers.value.contains(ch)
                        Surface(
                            onClick = {
                                val current = selectedChapterNumbers.value.toMutableSet()
                                if (current.contains(ch)) {
                                    if (current.size > 1) current.remove(ch)
                                } else {
                                    current.add(ch)
                                }
                                selectedChapterNumbers.value = current
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) accentCyan.copy(alpha = 0.25f) else cardBg,
                            border = BorderStroke(1.dp, if (isSelected) accentCyan else borderColor)
                        ) {
                            Text(
                                text = "Ch $ch",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = chapterTopicInput,
                    onValueChange = { chapterTopicInput = it },
                    label = { Text("Chapter Topic Name (Optional, e.g. Dynamics & Vector)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Newtonian Mechanics", fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentViolet,
                        unfocusedBorderColor = borderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Coaching / Batch Name Chips + Input
                Text(
                    text = "Coaching / Batch Name:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Private Batch", "Udvash", "College Class Test", "Model Test", "Solo Prep").forEach { preset ->
                        Surface(
                            onClick = { coachingNameInput = preset },
                            shape = RoundedCornerShape(8.dp),
                            color = if (coachingNameInput == preset) accentViolet.copy(alpha = 0.25f) else cardBg,
                            border = BorderStroke(1.dp, if (coachingNameInput == preset) accentViolet else borderColor)
                        ) {
                            Text(
                                text = preset,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                color = if (coachingNameInput == preset) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = coachingNameInput,
                    onValueChange = { coachingNameInput = it },
                    placeholder = { Text("e.g. Physics Private Batch", fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentViolet,
                        unfocusedBorderColor = borderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Exam Date Picker with Quick Chips
                Text(
                    text = "Exam Date:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                val quickDateOffsets = listOf(
                    Pair("Tomorrow", 1L),
                    Pair("In 2 Days", 2L),
                    Pair("In 3 Days", 3L),
                    Pair("In 5 Days", 5L),
                    Pair("In 7 Days", 7L)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickDateOffsets.forEach { (label, offset) ->
                        val targetEpoch = todayEpochDay + offset
                        val isSelected = examDateEpochDay == targetEpoch
                        Surface(
                            onClick = { examDateEpochDay = targetEpoch },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) accentViolet.copy(alpha = 0.25f) else cardBg,
                            border = BorderStroke(1.dp, if (isSelected) accentViolet else borderColor)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormatted = LocalDate.ofEpochDay(examDateEpochDay).format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
                Surface(
                    onClick = showDatePicker,
                    shape = RoundedCornerShape(10.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, accentCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📅 $dateFormatted (${examDateEpochDay - todayEpochDay}d left)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Change",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Exam Format Selector
                Text(
                    text = "Exam Format:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExamFormatMode.entries.forEach { mode ->
                        val isSelected = examFormatMode == mode
                        Surface(
                            onClick = { examFormatMode = mode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) accentViolet.copy(alpha = 0.25f) else cardBg,
                            border = BorderStroke(1.dp, if (isSelected) accentViolet else borderColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${mode.icon} ${mode.label}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Dynamic Question Count Steppers & Presets
                // MCQ Count Section
                if (examFormatMode == ExamFormatMode.MCQ_ONLY || examFormatMode == ExamFormatMode.MCQ_AND_CQ_BOTH) {
                    Text(
                        text = "How Many MCQs? ($mcqCount MCQs)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { mcqCount = (mcqCount - 5).coerceAtLeast(0) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(cardBg)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-5", tint = TextPrimary)
                        }
                        Text(
                            text = "$mcqCount",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = accentCyan
                        )
                        IconButton(
                            onClick = { mcqCount = (mcqCount + 5).coerceAtMost(200) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(cardBg)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+5", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(15, 20, 25, 30, 50, 100).forEach { preset ->
                                Surface(
                                    onClick = { mcqCount = preset },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (mcqCount == preset) accentCyan.copy(alpha = 0.25f) else cardBg,
                                    border = BorderStroke(1.dp, if (mcqCount == preset) accentCyan else borderColor)
                                ) {
                                    Text(
                                        text = "$preset",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = if (mcqCount == preset) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // CQ Count Section
                if (examFormatMode == ExamFormatMode.CQ_ONLY || examFormatMode == ExamFormatMode.MCQ_AND_CQ_BOTH) {
                    Text(
                        text = "How Many CQs (Creative Questions)? ($cqCount CQs)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { cqCount = (cqCount - 1).coerceAtLeast(0) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(cardBg)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1", tint = TextPrimary)
                        }
                        Text(
                            text = "$cqCount",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = accentViolet
                        )
                        IconButton(
                            onClick = { cqCount = (cqCount + 1).coerceAtMost(20) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(cardBg)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(1, 2, 3, 4, 5, 8).forEach { preset ->
                                Surface(
                                    onClick = { cqCount = preset },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (cqCount == preset) accentViolet.copy(alpha = 0.25f) else cardBg,
                                    border = BorderStroke(1.dp, if (cqCount == preset) accentViolet else borderColor)
                                ) {
                                    Text(
                                        text = "$preset",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = if (cqCount == preset) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Auto-create Slices Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { autoCreateSlices = !autoCreateSlices }
                ) {
                    Checkbox(
                        checked = autoCreateSlices,
                        onCheckedChange = { autoCreateSlices = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentViolet)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Automatically add daily Exam Prep slices to Battleground until exam day",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 8. Live Gemini 3.8 Flash Evaluation on Save
                val handleSaveExam = {
                    if (!isAnalyzingAi) {
                        isAnalyzingAi = true
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                        val sortedChs = selectedChapterNumbers.value.sorted()
                        val chTitleStr = if (sortedChs.size == 1) {
                            "Ch ${sortedChs.first()}${if (chapterTopicInput.isNotBlank()) ": $chapterTopicInput" else ""}"
                        } else {
                            "Ch ${sortedChs.joinToString(", ")}${if (chapterTopicInput.isNotBlank()) " ($chapterTopicInput)" else ""}"
                        }

                        val daysLeft = (examDateEpochDay - todayEpochDay).coerceAtLeast(0L)

                        scope.launch {
                            val aiEval = GeminiRouletteService.evaluatePrivateExamDifficulty(
                                subjectName = currentSubjectName,
                                paperName = if (isSinglePaper) "Single Paper" else paperMode,
                                chapterNumbersAndTitles = chTitleStr,
                                examFormatMode = examFormatMode,
                                mcqCount = mcqCount,
                                cqCount = cqCount,
                                daysLeft = daysLeft
                            )

                            val record = BatchExamRecord(
                                batchOrCoachingName = coachingNameInput.ifBlank { "Private Batch" },
                                subjectName = currentSubjectName,
                                paperName = if (isSinglePaper) "Single Paper" else paperMode,
                                chapterNumbersAndTitles = chTitleStr,
                                examDateEpochDay = examDateEpochDay,
                                examFormatMode = examFormatMode,
                                mcqCount = mcqCount,
                                cqCount = cqCount,
                                aiDifficultyScore = aiEval.difficultyScore,
                                aiDifficultyLabel = aiEval.difficultyLabel,
                                aiEstimatedTotalStudyHours = aiEval.estimatedTotalStudyHours,
                                aiImportanceScore = aiEval.importanceScore,
                                aiPrepStrategyAdvice = aiEval.prepStrategyAdvice,
                                studyHoursLogged = 0f,
                                isCompleted = false
                            )

                            isAnalyzingAi = false
                            onSaveExam(record, autoCreateSlices)
                        }
                    }
                }

                Button(
                    onClick = { handleSaveExam() },
                    enabled = !isAnalyzingAi,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentViolet,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isAnalyzingAi) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini 3.8 Flash Analyzing Exam...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🤖 Analyze Chapter & Save Exam",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatchExamCard(
    exam: BatchExamRecord,
    todayEpochDay: Long,
    isNature: Boolean,
    onIncrementStudyHours: () -> Unit,
    onAutoCreateSlices: () -> Unit,
    onMarkCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated
    val borderColor = if (isNature) TerrariumBorder else ObsidianBorder
    val accentViolet = if (isNature) NatureAutumnOrange else Color(0xFFE040FB)
    val accentCyan = if (isNature) NatureSproutLime else CyberCyan

    val daysLeft = exam.daysLeft(todayEpochDay)
    val isUrgent = daysLeft <= 2L && exam.remainingStudyHours > 0f

    val progress = (exam.studyHoursLogged / exam.aiEstimatedTotalStudyHours.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
    val dailyNeeded = if (daysLeft > 0L) {
        String.format(Locale.US, "%.1f", exam.remainingStudyHours / daysLeft)
    } else {
        String.format(Locale.US, "%.1f", exam.remainingStudyHours)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isUrgent) Color(0xFFFF1744).copy(alpha = 0.8f) else borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Subject & Coaching Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${exam.subjectName} ${if (exam.paperName != "Single Paper") exam.paperName else ""}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentViolet.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = exam.batchOrCoachingName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentViolet
                        )
                    }
                }

                // Countdown Pill
                val countdownColor = when {
                    daysLeft == 0L -> Color(0xFFFF1744)
                    daysLeft <= 2L -> Color(0xFFFF6D00)
                    else -> accentCyan
                }
                val countdownText = when {
                    daysLeft == 0L -> "🎯 TODAY"
                    daysLeft < 0L -> "⚠️ ${-daysLeft}d Ago"
                    else -> "⏳ ${daysLeft}d Left"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(countdownColor.copy(alpha = 0.18f))
                        .border(1.dp, countdownColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = countdownText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = countdownColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Chapters & Format Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exam.chapterNumbersAndTitles,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianBackground)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${exam.examFormatMode.icon} ${exam.formatSummary}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Difficulty & Importance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exam.aiDifficultyLabel,
                    fontSize = 10.sp,
                    color = Color(0xFFFFB300),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Importance: ${exam.aiImportanceScore}/100",
                    fontSize = 10.sp,
                    color = accentCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time Readiness Bar & +30m Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Need: ${String.format(Locale.US, "%.1f", exam.aiEstimatedTotalStudyHours)}h total (${dailyNeeded}h/day left)",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Surface(
                    onClick = onIncrementStudyHours,
                    shape = RoundedCornerShape(6.dp),
                    color = accentCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, accentCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "+30m Studied",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentViolet,
                trackColor = borderColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${String.format(Locale.US, "%.1f", exam.studyHoursLogged)}h of ${String.format(Locale.US, "%.1f", exam.aiEstimatedTotalStudyHours)}h prepped (${(progress * 100).toInt()}%)",
                fontSize = 9.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            // AI Tactical Strategy Box
            if (exam.aiPrepStrategyAdvice.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBackground.copy(alpha = 0.8f))
                        .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = exam.aiPrepStrategyAdvice,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAutoCreateSlices,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentCyan),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Boost Slices", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onMarkCompleted,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberEmerald),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberEmerald)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "Done", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
