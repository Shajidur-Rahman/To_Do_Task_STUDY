package com.chronos.hsc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.BLACK_LOVER }
val LocalNatureCritterType = compositionLocalOf { NatureCritterType.POKA_INCHWORM }

// Nature & Terrarium Color Palette
val TerrariumBackground = Color(0xFF0B1911)
val TerrariumSurface = Color(0xFF122419)
val TerrariumSurfaceElevated = Color(0xFF193323)
val TerrariumSurfaceHighlight = Color(0xFF22442F)
val TerrariumBorder = Color(0xFF2D5A3C)
val TerrariumBorderHighlight = Color(0xFF4ADE80)

val NatureLeafGreen = Color(0xFF22C55E)
val NatureSproutLime = Color(0xFF84CC16)
val NatureDewdropCyan = Color(0xFF06B6D4)
val NatureFireflyGold = Color(0xFFFACC15)
val NatureVineBrown = Color(0xFF4A3525)
val NatureAutumnOrange = Color(0xFFF97316)
val NatureLagRust = Color(0xFFEA580C)
val NatureLadybugRed = Color(0xFFEF4444)
val NatureFlowerPink = Color(0xFFF472B6)
val NaturePokaBody = Color(0xFF86EFAC)
val NaturePokaFace = Color(0xFF15803D)

// Shapes
val NatureCardShape = RoundedCornerShape(topStart = 22.dp, bottomEnd = 22.dp, topEnd = 10.dp, bottomStart = 10.dp)
val CyberCardShape = RoundedCornerShape(16.dp)

object ChronosThemeTokens {

    @Composable
    fun isNature(): Boolean = LocalAppThemeMode.current == AppThemeMode.NATURE_LOVER

    @Composable
    fun cardShape(): Shape {
        return if (isNature()) NatureCardShape else CyberCardShape
    }

    @Composable
    fun backgroundColor(): Color {
        return if (isNature()) TerrariumBackground else ObsidianBackground
    }

    @Composable
    fun surfaceColor(): Color {
        return if (isNature()) TerrariumSurface else ObsidianSurface
    }

    @Composable
    fun surfaceElevatedColor(): Color {
        return if (isNature()) TerrariumSurfaceElevated else ObsidianSurfaceElevated
    }

    @Composable
    fun borderColor(): Color {
        return if (isNature()) TerrariumBorder else ObsidianBorder
    }

    @Composable
    fun primaryAccent(): Color {
        return if (isNature()) NatureLeafGreen else Color.White
    }

    @Composable
    fun secondaryAccent(): Color {
        return if (isNature()) NatureSproutLime else MonoTitanium
    }

    @Composable
    fun lagBadgeColor(): Color {
        return if (isNature()) NatureLagRust else Color(0xFFE11D48)
    }
}
