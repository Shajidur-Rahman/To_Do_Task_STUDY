package com.chronos.hsc.ui.theme

import androidx.compose.ui.graphics.Color
import com.chronos.hsc.data.local.HscSubject

// Obsidian & Cyberpunk Surface Palette
val ObsidianBackground = Color(0xFF07090E)
val ObsidianSurface = Color(0xFF10141F)
val ObsidianSurfaceElevated = Color(0xFF171D2D)
val ObsidianSurfaceHighlight = Color(0xFF20283E)
val ObsidianBorder = Color(0xFF28324E)

// Primary Cyber Accents
val CyberCyan = Color(0xFF00E5FF)
val CyberEmerald = Color(0xFF00E676)
val CyberMagenta = Color(0xFFD500F9)
val CyberAmber = Color(0xFFFFEA00)
val CyberOrange = Color(0xFFFF6D00)
val CyberCrimson = Color(0xFFFF1744)
val BossShieldPurple = Color(0xFFAA00FF)

// Text Hierarchy
val TextPrimary = Color(0xFFF3F6FF)
val TextSecondary = Color(0xFFA0AEC0)
val TextMuted = Color(0xFF64748B)

// Heatmap Levels (Neon Emerald / Cyan Scale)
val HeatmapLevel0 = Color(0xFF141A29)
val HeatmapLevel1 = Color(0xFF0A3D38)
val HeatmapLevel2 = Color(0xFF00796B)
val HeatmapLevel3 = Color(0xFF00BFA5)
val HeatmapLevel4 = Color(0xFF00E676)

fun HscSubject.brandColor(): Color = Color(this.colorHex)
