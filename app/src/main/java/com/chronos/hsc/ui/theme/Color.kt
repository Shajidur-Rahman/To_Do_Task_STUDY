package com.chronos.hsc.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.chronos.hsc.data.local.HscSubject

// Pure Obsidian & Minimalist Luxury Dark Surface Palette (Aesthetic Monochrome OLED)
val ObsidianBackground = Color(0xFF060608)
val ObsidianSurface = Color(0xFF0F0F13)
val ObsidianSurfaceElevated = Color(0xFF17171D)
val ObsidianSurfaceHighlight = Color(0xFF22222A)
val ObsidianBorder = Color(0xFF26262F)

// Monochrome & Minimalist Accents (High-Aesthetic Platinum & Titanium Study Engine)
val MonoWhite = Color(0xFFFAFAFA)
val MonoSilver = Color(0xFFE4E4E7)
val MonoTitanium = Color(0xFFA1A1AA)
val MonoMuted = Color(0xFF71717A)
val MonoDarkBorder = Color(0xFF26262F)

// Refined Luxury Accents for Black UI (Replaces harsh rainbow neon with Platinum, Champagne & Soft Sage/Rose)
val CyberCyan = Color(0xFFE4E4E7)       // Crisp Platinum Silver
val CyberEmerald = Color(0xFF6EE7B7)    // Soft Sage Mint
val CyberMagenta = Color(0xFFD4D4D8)    // Brushed Satin Titanium
val CyberAmber = Color(0xFFFDE68A)      // Soft Champagne Gold
val CyberOrange = Color(0xFFFCD34D)     // Warm Muted Amber
val CyberCrimson = Color(0xFFFB7185)    // Soft Rose Alert
val BossShieldPurple = Color(0xFFA1A1AA) // Brushed Steel Shield

// Text Hierarchy
val TextPrimary = Color(0xFFFAFAFA)
val TextSecondary = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF71717A)

// Heatmap Levels
val HeatmapLevel0 = Color(0xFF121216)
val HeatmapLevel1 = Color(0xFF27272A)
val HeatmapLevel2 = Color(0xFF52525B)
val HeatmapLevel3 = Color(0xFFA1A1AA)
val HeatmapLevel4 = Color(0xFFFAFAFA)

@Composable
fun HscSubject.brandColor(): Color {
    if (LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER) {
        return Color(this.colorHex)
    }
    // In Black Lover UI: Cohesive, low-saturation platinum & metallic slate tones (no rainbow clutter)
    return when (this) {
        HscSubject.PHYSICS -> Color(0xFFE2E8F0)     // Frosted Platinum
        HscSubject.CHEMISTRY -> Color(0xFFD6D3D1)   // Warm Titanium Stone
        HscSubject.HIGHER_MATH -> Color(0xFFE4E4E7) // Satin Silver
        HscSubject.BIOLOGY -> Color(0xFFCBD5E1)     // Cool Mist Slate
        HscSubject.ICT -> Color(0xFFD4D4D8)         // Brushed Zinc
        HscSubject.BANGLA -> Color(0xFFE5E5E5)      // Pearl White-Silver
        HscSubject.ENGLISH -> Color(0xFFD4D4D8)     // Brushed Chrome
    }
}

@Composable
fun Color.harmonizeForBlackUi(): Color {
    if (LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER) {
        return this
    }
    return lerp(this, Color(0xFFE4E4E7), 0.72f)
}
