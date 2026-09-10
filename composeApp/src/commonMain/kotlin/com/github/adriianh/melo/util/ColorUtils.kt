package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Utilities for advanced color manipulation, HSL conversion, contrast enforcement,
 * and theme harmonization.
 */
object ColorUtils {

    /**
     * Converts a Compose [Color] into HSL components:
     * [0] -> Hue in degrees [0f, 360f]
     * [1] -> Saturation [0f, 1f]
     * [2] -> Lightness [0f, 1f]
     */
    fun rgbToHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val delta = max - min

        var h = 0f
        var s = 0f
        val l = (max + min) / 2f

        if (delta > 0.0001f) {
            s = if (l > 0.5f) delta / (2f - max - min) else delta / (max + min)

            h = when (max) {
                r -> ((g - b) / delta) + (if (g < b) 6f else 0f)
                g -> ((b - r) / delta) + 2f
                else -> ((r - g) / delta) + 4f
            }
            h *= 60f
        }

        return floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    /**
     * Converts HSL components back into a Compose [Color].
     */
    fun hslToRgb(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): Color {
        val h = (hue % 360f + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)

        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (rPrime, gPrime, bPrime) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    /**
     * Calculates the WCAG 2.1 relative contrast ratio between two colors (range: 1.0 to 21.0).
     */
    fun contrastRatio(foreground: Color, background: Color): Float {
        val lum1 = foreground.luminance()
        val lum2 = background.luminance()
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Harmonizes an accent color for the active theme mode.
     *
     * - Preserves the authentic hue of the album artwork.
     * - Fixes muddy/unsaturated tones by boosting saturation.
     * - Optimizes lightness and perceived contrast, so it is clearly readable against
     *   dark backgrounds (in dark mode) or light surfaces (in light mode).
     * - Special hue compensation for low-perceived-luminance colors like deep blue / purple.
     */
    fun harmonize(color: Color, isDarkMode: Boolean): Color {
        val hsl = rgbToHsl(color)
        var h = hsl[0]
        var s = hsl[1]
        var l = hsl[2]

        // If the color is essentially grayscale/monochrome (< 10% saturation), use brand accent hue
        if (s < 0.10f) {
            h = 348f // Vivid Melo Pink/Rose hue
            s = 0.85f
            l = if (isDarkMode) 0.65f else 0.42f
            return hslToRgb(h, s, l)
        }

        // Boost saturation for vibrant musical aesthetic (minimum 65%)
        s = s.coerceIn(0.65f, 0.95f)

        if (isDarkMode) {
            // Dark Mode: Target background is #0D0D0E / #1C1C1E
            // Blue/Violet hues (200° - 275°) have lower perceived luminance for human eyes,
            // so give them extra lightness to pop vibrantly without washing out.
            val minLightness = when (h) {
                in 195f..275f -> 0.68f // Blues & Purples
                in 45f..160f -> 0.58f  // Yellows & Greens (naturally brighter)
                else -> 0.64f          // Reds, Oranges, Pinks, Magentas
            }
            l = l.coerceIn(minLightness, 0.78f)
        } else {
            // Light Mode: Target background is #F2F4F7 / #FFFFFF
            // Ensure strong contrast against light surfaces
            val maxLightness = when (h) {
                in 45f..160f -> 0.35f  // Greens & Yellows need to be darker to be legible on white
                else -> 0.42f
            }
            l = l.coerceIn(0.28f, maxLightness)
        }

        val result = hslToRgb(h, s, l)

        // Ensure minimum WCAG contrast ratio (at least 4.5:1 for body/accent, 6:1 ideal)
        val bg = if (isDarkMode) Color(0xFF0D0D0E) else Color(0xFFFFFFFF)
        return ensureContrast(result, bg, minContrast = 4.5f)
    }

    /**
     * Adjusts the lightness of [color] until it meets or exceeds [minContrast] against [background].
     */
    fun ensureContrast(color: Color, background: Color, minContrast: Float = 4.5f): Color {
        var current = color
        val bgLum = background.luminance()
        val shouldLighten = bgLum < 0.5f

        val hsl = rgbToHsl(current)
        var l = hsl[2]
        var iterations = 0

        while (contrastRatio(current, background) < minContrast && iterations < 12) {
            l = if (shouldLighten) {
                (l + 0.04f).coerceAtMost(0.92f)
            } else {
                (l - 0.04f).coerceAtLeast(0.15f)
            }
            current = hslToRgb(hsl[0], hsl[1], l)
            iterations++
        }

        return current
    }

    /**
     * Creates a semi-transparent surface tint derived from an accent color.
     */
    fun createDynamicSurface(accentColor: Color, isDarkMode: Boolean): Color {
        return if (isDarkMode) {
            lerp(accentColor, Color.Black, 0.82f).copy(alpha = 0.65f)
        } else {
            lerp(accentColor, Color.White, 0.82f).copy(alpha = 0.45f)
        }
    }
}