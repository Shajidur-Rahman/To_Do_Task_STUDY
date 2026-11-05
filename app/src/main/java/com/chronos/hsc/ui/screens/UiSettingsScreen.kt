package com.chronos.hsc.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronos.hsc.ui.components.NatureAmbientOverlayCanvas
import com.chronos.hsc.ui.theme.AppThemeMode
import com.chronos.hsc.ui.theme.CyberAmber
import com.chronos.hsc.ui.theme.CyberCrimson
import com.chronos.hsc.ui.theme.CyberCyan
import com.chronos.hsc.ui.theme.CyberEmerald
import com.chronos.hsc.ui.theme.CyberMagenta
import com.chronos.hsc.ui.theme.FoliageDensity
import com.chronos.hsc.ui.theme.NatureCritterType
import com.chronos.hsc.ui.theme.NatureFireflyGold
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
import com.chronos.hsc.ui.theme.ThemePreferences
import com.chronos.hsc.ui.viewmodel.StudyViewModel

@Composable
fun UiSettingsScreen(
    preferences: ThemePreferences,
    viewModel: StudyViewModel
) {
    val currentTheme by preferences.themeMode.collectAsState()
    val critterType by preferences.critterType.collectAsState()
    val foliageDensity by preferences.foliageDensity.collectAsState()
    val isCompactHeader by preferences.compactHeader.collectAsState()
    val ambientAnimations by preferences.ambientAnimationsEnabled.collectAsState()

    val isNature = currentTheme == AppThemeMode.NATURE_LOVER

    val containerBg = if (isNature) TerrariumBackground else ObsidianBackground
    val surfaceColor = if (isNature) TerrariumSurface else ObsidianSurface
    val surfaceElevated = if (isNature) TerrariumSurfaceElevated else ObsidianSurfaceElevated
    val borderColor = if (isNature) TerrariumBorder else ObsidianBorder
    val accentColor = if (isNature) NatureLeafGreen else CyberCyan

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerBg)
    ) {
        if (isNature && ambientAnimations) {
            NatureAmbientOverlayCanvas(
                density = foliageDensity,
                modifier = Modifier.fillMaxSize()
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Title
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SKINS & THEME ENGINE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch between the obsidian cyberpunk aesthetic and the living terrarium ecosystem.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // 2. Dual-Theme Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CHOOSE YOUR APP SKIN",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    // Black Lover Card
                    ThemeSelectionCard(
                        mode = AppThemeMode.BLACK_LOVER,
                        isSelected = currentTheme == AppThemeMode.BLACK_LOVER,
                        previewContent = {
                            CyberpunkThemePreview()
                        },
                        onSelect = { preferences.setThemeMode(AppThemeMode.BLACK_LOVER) }
                    )

                    // Nature Lover Card
                    ThemeSelectionCard(
                        mode = AppThemeMode.NATURE_LOVER,
                        isSelected = currentTheme == AppThemeMode.NATURE_LOVER,
                        previewContent = {
                            NatureThemePreview(critterType = critterType)
                        },
                        onSelect = { preferences.setThemeMode(AppThemeMode.NATURE_LOVER) }
                    )
                }
            }

            // 3. Nature Lover Customizer (Animated visibility when Nature selected)
            if (isNature) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceColor)
                            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌿 NATURE ECOSYSTEM CUSTOMIZER",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = NatureLeafGreen
                            )
                        }

                        // Critter Companion Selector
                        Text(
                            text = "Critter Companion (Poka):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NatureCritterType.entries.forEach { critter ->
                                val isCritterSelected = critterType == critter
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCritterSelected) NatureLeafGreen.copy(alpha = 0.22f) else surfaceElevated,
                                    border = BorderStroke(
                                        width = if (isCritterSelected) 1.5.dp else 1.dp,
                                        color = if (isCritterSelected) NatureLeafGreen else borderColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { preferences.setCritterType(critter) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = critter.emoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = critter.displayName.substringBefore(" "),
                                            fontSize = 10.sp,
                                            fontWeight = if (isCritterSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCritterSelected) NatureLeafGreen else TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Foliage Canopy Density Selector
                        Text(
                            text = "Foliage Density & Fireflies:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FoliageDensity.entries.forEach { density ->
                                val isDensitySelected = foliageDensity == density
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDensitySelected) NatureSproutLime.copy(alpha = 0.2f) else surfaceElevated,
                                    border = BorderStroke(
                                        width = if (isDensitySelected) 1.5.dp else 1.dp,
                                        color = if (isDensitySelected) NatureSproutLime else borderColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { preferences.setFoliageDensity(density) }
                                ) {
                                    Text(
                                        text = density.displayName.substringBefore(" ("),
                                        fontSize = 11.sp,
                                        fontWeight = if (isDensitySelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isDensitySelected) NatureSproutLime else TextSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Ambient Glow Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ambient Firefly Glow",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Render floating fireflies & swaying ferns",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = ambientAnimations,
                                onCheckedChange = { preferences.setAmbientAnimationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NatureLeafGreen,
                                    checkedTrackColor = NatureLeafGreen.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }
            }

            // 4. Mobile Layout Density Setting
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(surfaceColor)
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "📱 Compact Mobile HUD",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Saves screen height with single-row HUD & collapsible Tools deck, giving 78%+ space for study cards.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isCompactHeader,
                            onCheckedChange = { preferences.setCompactHeader(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            // 5. Instant Catch-Up Engine Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (isNature) Color(0xFF1B3322) else Color(0xFF1B1527),
                                    surfaceColor
                                )
                            )
                        )
                        .border(1.dp, if (isNature) NatureSproutLime.copy(alpha = 0.5f) else CyberMagenta.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = if (isNature) NatureSproutLime else CyberMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INSTANT CATCH-UP ENGINE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = if (isNature) NatureSproutLime else CyberMagenta
                        )
                    }

                    Text(
                        text = "Fell behind on multiple study chains? Don't let overdue backlog cause stress. Instant Catch-Up re-baselines all delayed chains starting from today so you can study with a fresh slate.",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { viewModel.instantCatchUpAllLaggingChains() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNature) NatureLeafGreen else CyberMagenta,
                            contentColor = if (isNature) Color(0xFF00210E) else Color.White
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
                            text = "Instant Catch-Up All Lagging Chains",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    mode: AppThemeMode,
    isSelected: Boolean,
    previewContent: @Composable () -> Unit,
    onSelect: () -> Unit
) {
    val isNatureCard = mode == AppThemeMode.NATURE_LOVER
    val cardBg = if (isNatureCard) TerrariumSurface else ObsidianSurface
    val cardBorderColor = when {
        isSelected && isNatureCard -> NatureLeafGreen
        isSelected && !isNatureCard -> CyberCyan
        isNatureCard -> TerrariumBorder
        else -> ObsidianBorder
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isNatureCard) "🌿🐛" else "🖤⚡",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = mode.title,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = mode.subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isNatureCard) NatureLeafGreen.copy(alpha = 0.25f)
                                else CyberCyan.copy(alpha = 0.25f)
                            )
                            .border(
                                1.dp,
                                if (isNatureCard) NatureLeafGreen else CyberCyan,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = if (isNatureCard) NatureLeafGreen else CyberCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini visual preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isNatureCard) Color(0xFF0D1F15) else Color(0xFF090C14))
                    .border(1.dp, if (isNatureCard) Color(0xFF1E3A28) else Color(0xFF172033), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                previewContent()
            }
        }
    }
}

@Composable
private fun CyberpunkThemePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("PHY Ch 4-1", color = CyberCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberMagenta.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("⚡ 2x XP", color = CyberMagenta, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            Text("🔥 12d", color = CyberAmber, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        // Simulated Cyber bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF172033))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(5.dp)
                    .background(CyberCyan)
            )
        }
    }
}

@Composable
private fun NatureThemePreview(critterType: NatureCritterType) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 3.dp, bottomStart = 3.dp))
                        .background(NatureLeafGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("MAT Ch 3-2", color = NatureLeafGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NatureSproutLime.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${critterType.emoji} Active", color = NatureSproutLime, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            Text("✨ Fireflies", color = NatureFireflyGold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        // Botanical stem with leaves
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1B3322)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(listOf(NatureSproutLime, NatureLeafGreen))
                    )
            )
        }
    }
}
