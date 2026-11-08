package com.chronos.hsc.ui.theme

import androidx.compose.ui.graphics.Color
import com.chronos.hsc.data.local.HscSubject

// Pure Obsidian & Minimalist Black Surface Palette (Serious study mood, pitch black OLED)
val ObsidianBackground = Color(0xFF000000)
val ObsidianSurface = Color(0xFF0C0C0C)
val ObsidianSurfaceElevated = Color(0xFF161616)
val ObsidianSurfaceHighlight = Color(0xFF222222)
val ObsidianBorder = Color(0xFF27272A)

// Monochrome & Minimalist Accents (Serious Black & White Study Engine)
val MonoWhite = Color(0xFFFFFFFF)
val MonoSilver = Color(0xFFE4E4E7)
val MonoTitanium = Color(0xFFA1A1AA)
val MonoMuted = Color(0xFF71717A)
val MonoDarkBorder = Color(0xFF27272A)

// Secondary alert/accent colors
val CyberCyan = Color(0xFF00E5FF)
val CyberEmerald = Color(0xFF00E676)
val CyberMagenta = Color(0xFFD500F9)
val CyberAmber = Color(0xFFFFEA00)
val CyberOrange = Color(0xFFFF6D00)
val CyberCrimson = Color(0xFFFF1744)
val BossShieldPurple = Color(0xFFAA00FF)

// Text Hierarchy
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF71717A)

// Heatmap Levels
val HeatmapLevel0 = Color(0xFF141416)
val HeatmapLevel1 = Color(0xFF27272A)
val HeatmapLevel2 = Color(0xFF52525B)
val HeatmapLevel3 = Color(0xFFA1A1AA)
val HeatmapLevel4 = Color(0xFFFFFFFF)

fun HscSubject.brandColor(): Color = Color(this.colorHex)
