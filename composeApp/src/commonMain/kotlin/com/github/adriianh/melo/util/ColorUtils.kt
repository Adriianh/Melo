package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Utilities for color manipulation used by the UI.
 *
 * Provides small helpers to adapt colors for light and dark themes and to
 * produce dynamic surface colors derived from an accent. These helpers aim to
 * improve perceived contrast and visual consistency across theme modes.
 */
object ColorUtils {

    /**
     * Harmonize a color for use in the current theme (dark or light).
     *
     * The function measures the color's relative luminance and blends it slightly
     * toward white (when in dark mode) or toward black (when in light mode)
     * depending on thresholds. This prevents very dark colors from disappearing
     * on dark backgrounds and very light colors from washing out on light
     * backgrounds.
     *
     * @param color the input [Color] to adjust
     * @param isDarkMode true when the UI is using a dark theme, false for light
     * @return a [Color] adjusted to better suit the specified theme mode
     */
    fun harmonize(color: Color, isDarkMode: Boolean): Color {
        val lum = color.luminance()

        return if (isDarkMode) {
            when {
                lum < 0.15f -> lerp(color, Color.White, 0.45f)
                lum < 0.30f -> lerp(color, Color.White, 0.25f)
                else -> color
            }
        } else {
            when {
                lum > 0.85f -> lerp(color, Color.Black, 0.40f)
                lum > 0.70f -> lerp(color, Color.Black, 0.20f)
                else -> color
            }
        }
    }

    /**
     * Creates a semi-transparent surface color derived from an accent color.
     *
     * The result blends the accent strongly toward black (in dark mode) or
     * toward white (in light mode) and applies a reduced alpha. Useful for
     * overlays, scrims, or surface tints that should carry a hint of the accent
     * while remaining readable.
     *
     * @param accentColor the base accent [Color]
     * @param isDarkMode true when the UI is using a dark theme, false for light
     * @return a semi-transparent surface [Color] appropriate for the theme mode
     */
    fun createDynamicSurface(accentColor: Color, isDarkMode: Boolean): Color {
        return if (isDarkMode) {
            lerp(accentColor, Color.Black, 0.8f).copy(alpha = 0.6f)
        } else {
            lerp(accentColor, Color.White, 0.8f).copy(alpha = 0.4f)
        }
    }
}