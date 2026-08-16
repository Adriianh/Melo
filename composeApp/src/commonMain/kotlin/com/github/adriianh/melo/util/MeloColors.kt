package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color

object MeloColors {
    // V5 Base Neutrals (§2.1)
    val surface0 = Color(0xFF0D0D0E) // Deepest Black for Ambient Canvas contrast
    val surface1 = Color(0xFF1C1C1E) // Sidebar, M3 cards
    val surface2 = Color(0xFF242426) // Player bar, sheets
    val border = Color(0x1AFFFFFF) // 10% white
    val borderStrong = Color(0x33FFFFFF) // 20% white

    val textPrimary = Color.White
    val textSecondary = Color(0xBFFFFFFF) // 75% white
    val textMuted = Color(0x80FFFFFF) // 50% white

    // Brand vs Dynamic Accents (§2.2)
    val brandAccent = Color(0xFFFF2D55) // Fixed brand pink/red for navigation

    // Chrome Tokens (§2.3)
    val chromePillFill = Color(0x0FFFFFFF) // 6% white + subtle depth
    val chromePillBorder = Color(0x1FFFFFFF) // 12% white

    // Content Glass Tokens (§2.3 - Now Playing)
    val contentGlassFill = Color(0x1FFFFFFF) // 12% white
    val contentGlassBorder = Color(0x40FFFFFF) // 25% white
    val scrimOverArt = Color(0x59000000) // 35% black

    // Aliases for compatibility
    val glassFill = contentGlassFill
    val glassBorder = contentGlassBorder
    val chromeGlassFill = chromePillFill
    val chromeGlassBorder = chromePillBorder
}