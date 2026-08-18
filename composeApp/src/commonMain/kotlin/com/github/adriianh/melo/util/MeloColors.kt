package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color

interface MeloColorScheme {
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
    val brandAccent: Color get() = Color(0xFFFF2D55)
}

object DarkMeloColors : MeloColorScheme {
    override val surface0 = Color(0xFF0D0D0E)
    override val surface1 = Color(0xFF1C1C1E)
    override val surface2 = Color(0xFF242426)
    override val border = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x33FFFFFF)
    override val textPrimary = Color.White
    override val textSecondary = Color(0xBFFFFFFF)
    override val textMuted = Color(0x80FFFFFF)
    override val glassSurface = Color(0xCC1C1C1E)
    override val glassFill = glassSurface
    override val glassBorder = Color(0x26FFFFFF)
    override val playerBarFill = Color(0xE61C1C1E)
    override val playerBarBorder = glassBorder
}

object LightMeloColors : MeloColorScheme {
    override val surface0 = Color(0xFFF2F3F5)
    override val surface1 = Color(0xFFFFFFFF)
    override val surface2 = Color(0xFFE8EAED)
    override val border = Color(0x1A000000)
    override val borderStrong = Color(0x33000000)
    override val textPrimary = Color(0xFF1A1C1E)
    override val textSecondary = Color(0xFF4F5660)
    override val textMuted = Color(0xFF747F8D)
    override val glassSurface = Color(0xCCFFFFFF)
    override val glassFill = glassSurface
    override val glassBorder = Color(0x1A000000)
    override val playerBarFill = Color(0xE6FFFFFF)
    override val playerBarBorder = glassBorder
}

object MeloColors : MeloColorScheme by DarkMeloColors {
    val Light = LightMeloColors
    val Dark = DarkMeloColors

    val chromePillFill = Color(0x0FFFFFFF)
    val chromePillBorder = Color(0x1FFFFFFF)
    val contentGlassFill = Color(0x1FFFFFFF)
    val contentGlassBorder = Color(0x40FFFFFF)
    val scrimOverArt = Color(0x59000000)
    val chromeGlassFill = chromePillFill
    val chromeGlassBorder = chromePillBorder
}
