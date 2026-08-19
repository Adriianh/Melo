package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

interface MeloColorScheme {
    val isDark: Boolean
    val surface0: Color
    val surface1: Color
    val surface2: Color
    val border: Color
    val borderStrong: Color
    val textPrimary: Color
    val textSecondary: Color
    val textMuted: Color
    val glassSurface: Color
    val glassFill: Color
    val glassBorder: Color
    val playerBarFill: Color
    val playerBarBorder: Color
    val chromePillFill: Color
    val chromePillBorder: Color
    val brandAccent: Color get() = Color(0xFFFF2D55)
}

object DarkMeloColors : MeloColorScheme {
    override val isDark = true
    override val surface0 = Color(0xFF0D0D0E)
    override val surface1 = Color(0xFF1C1C1E)
    override val surface2 = Color(0xFF242426)
    override val border = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x33FFFFFF)
    override val textPrimary = Color.White
    override val textSecondary = Color(0xBFFFFFFF)
    override val textMuted = Color(0x80FFFFFF)
    override val glassSurface = Color(0xCC1C1C1E)
    override val glassFill = Color(0x331C1C1E)
    override val glassBorder = Color(0x26FFFFFF)
    override val playerBarFill = Color(0xF21C1C1E)
    override val playerBarBorder = Color(0x33FFFFFF)
    override val chromePillFill = Color(0x0FFFFFFF)
    override val chromePillBorder = Color(0x1FFFFFFF)
}

object LightMeloColors : MeloColorScheme {
    override val isDark = false
    override val surface0 = Color(0xFFF2F4F7)
    override val surface1 = Color(0xFFFFFFFF)
    override val surface2 = Color(0xFFE9ECEF)
    override val border = Color(0x14000000)
    override val borderStrong = Color(0x24000000)
    override val textPrimary = Color(0xFF111315)
    override val textSecondary = Color(0xFF555B62)
    override val textMuted = Color(0xFF8A929B)
    override val glassSurface = Color(0xD9FFFFFF)
    override val glassFill = Color(0xB3FFFFFF)
    override val glassBorder = Color(0x1F000000)
    override val playerBarFill = Color(0xE6FFFFFF)
    override val playerBarBorder = Color(0x1F000000)
    override val chromePillFill = Color(0x0A000000)
    override val chromePillBorder = Color(0x14000000)
}

data class AnimatedMeloColors(
    override val isDark: Boolean,
    override val surface0: Color,
    override val surface1: Color,
    override val surface2: Color,
    override val border: Color,
    override val borderStrong: Color,
    override val textPrimary: Color,
    override val textSecondary: Color,
    override val textMuted: Color,
    override val glassSurface: Color,
    override val glassFill: Color,
    override val glassBorder: Color,
    override val playerBarFill: Color,
    override val playerBarBorder: Color,
    override val chromePillFill: Color,
    override val chromePillBorder: Color,
) : MeloColorScheme

val LocalMeloColors = staticCompositionLocalOf<MeloColorScheme> { DarkMeloColors }

val MeloColors: MeloColorScheme
    @Composable
    get() = LocalMeloColors.current
