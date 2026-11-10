package com.chronos.hsc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.store.CognitiveLoadCategory
import com.chronos.hsc.data.store.CustomSubject
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.ObsidianBackground
import com.chronos.hsc.ui.theme.ObsidianBorder
import com.chronos.hsc.ui.theme.ObsidianSurface
import com.chronos.hsc.ui.theme.ObsidianSurfaceElevated
import com.chronos.hsc.ui.theme.TextMuted
import com.chronos.hsc.ui.theme.TextPrimary
import com.chronos.hsc.ui.theme.TextSecondary
import java.util.UUID

@Composable
fun CreateCustomSubjectDialog(
    onDismiss: () -> Unit,
    onSubjectCreated: (CustomSubject) -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    var shortCode by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CognitiveLoadCategory.BALANCED_CONCEPTUAL) }
    var isSinglePaperOnly by remember { mutableStateOf(true) }
    var selectedColorHex by remember { mutableLongStateOf(CustomSubjectAndAiStore.COLOR_PRESETS.first()) }
    var nameError by remember { mutableStateOf(false) }

    val activeColor = Color(selectedColorHex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = activeColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, activeColor),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ADD CUSTOM SUBJECT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = activeColor
                            )
                            Text(
                                text = "Custom topic tracking with AI cognitive balancing",
                                fontSize = 10.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Subject Name Input
                Text(
                    text = "SUBJECT NAME",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { input ->
                        subjectName = input
                        nameError = false
                        if (shortCode.isEmpty() || shortCode.length <= 4) {
                            // Auto-derive candidate shortcode from uppercase letters or first letters
                            val words = input.trim().split(" ").filter { it.isNotEmpty() }
                            shortCode = when {
                                words.size >= 2 -> words.take(4).map { it.first().uppercaseChar() }.joinToString("")
                                input.length >= 3 -> input.take(4).uppercase()
                                else -> input.uppercase()
                            }
                        }
                    },
                    placeholder = { Text("e.g. Admission GK, Programming, Statistics", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = ObsidianSurfaceElevated,
                        unfocusedContainerColor = ObsidianSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Short Badge Code Input
                Text(
                    text = "SHORT BADGE CODE (2–5 CHARS)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = shortCode,
                    onValueChange = {
                        if (it.length <= 5) {
                            shortCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() }
                        }
                    },
                    placeholder = { Text("e.g. GK, PROG, STAT", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = ObsidianSurfaceElevated,
                        unfocusedContainerColor = ObsidianSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Cognitive Load Category Selector (Used by AI Roulette)
                Text(
                    text = "COGNITIVE LOAD CATEGORY (AI ROULETTE BALANCER)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CognitiveLoadCategory.entries.forEach { cat ->
                        val isCatSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCatSelected) activeColor.copy(alpha = 0.18f) else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                width = if (isCatSelected) 1.5.dp else 1.dp,
                                color = if (isCatSelected) activeColor else ObsidianBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = cat }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cat.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = cat.label,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isCatSelected) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        text = cat.shortDesc,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Paper Structure Toggle
                Text(
                    text = "PAPER STRUCTURE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Single Paper Only
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSinglePaperOnly) activeColor.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSinglePaperOnly) activeColor else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSinglePaperOnly = true }
                    ) {
                        Text(
                            text = "Single Paper Only",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSinglePaperOnly) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSinglePaperOnly) activeColor else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    // Has 1st & 2nd Paper
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isSinglePaperOnly) activeColor.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, if (!isSinglePaperOnly) activeColor else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSinglePaperOnly = false }
                    ) {
                        Text(
                            text = "Has 1st & 2nd Paper",
                            fontSize = 11.5.sp,
                            fontWeight = if (!isSinglePaperOnly) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isSinglePaperOnly) activeColor else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Neon Color Swatch Picker
                Text(
                    text = "ACCENT COLOR SWATCH",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomSubjectAndAiStore.COLOR_PRESETS.forEach { colorHex ->
                        val swatchColor = Color(colorHex)
                        val isCurrentSelected = selectedColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isCurrentSelected) 2.5.dp else 1.dp,
                                    color = if (isCurrentSelected) Color.White else ObsidianBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ObsidianBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        val cleanName = subjectName.trim()
                        if (cleanName.isEmpty()) {
                            nameError = true
                            return@Button
                        }
                        val cleanCode = shortCode.trim().ifEmpty {
                            cleanName.take(3).uppercase()
                        }
                        val customSubject = CustomSubject(
                            id = UUID.randomUUID().toString(),
                            name = cleanName,
                            shortCode = cleanCode,
                            colorHex = selectedColorHex,
                            cognitiveCategory = selectedCategory,
                            isSinglePaperOnly = isSinglePaperOnly,
                            baseSubject = HscSubject.ICT
                        )
                        CustomSubjectAndAiStore.saveCustomSubject(customSubject)
                        onSubjectCreated(customSubject)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        contentColor = ObsidianBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save & Select Subject",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
