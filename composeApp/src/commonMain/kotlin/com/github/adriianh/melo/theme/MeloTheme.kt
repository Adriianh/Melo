package com.github.adriianh.melo.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.util.ColorUtils
import com.github.adriianh.melo.util.DarkMeloColors
import com.github.adriianh.melo.util.LightMeloColors
import com.github.adriianh.melo.util.LocalMeloColors
import com.github.adriianh.melo.util.MeloType

@OptIn(ExperimentalMaterial3Api::class)
val MeloDarkRipple = RippleConfiguration(
    color = Color.White.copy(alpha = 0.15f),
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.06f,
        focusedAlpha = 0.08f,
        hoveredAlpha = 0.04f,
        pressedAlpha = 0.12f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
val MeloLightRipple = RippleConfiguration(
    color = Color.Black.copy(alpha = 0.12f),
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.04f,
        focusedAlpha = 0.06f,
        hoveredAlpha = 0.03f,
        pressedAlpha = 0.10f
    )
)

private val MeloDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF2D55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A101C),
    onPrimaryContainer = Color(0xFFFFDADE),
    background = DarkMeloColors.surface0,
    onBackground = DarkMeloColors.textPrimary,
    surface = DarkMeloColors.surface0,
    onSurface = DarkMeloColors.textPrimary,
    surfaceVariant = DarkMeloColors.surface1,
    onSurfaceVariant = DarkMeloColors.textSecondary,
    surfaceContainer = DarkMeloColors.surface1,
    surfaceContainerHigh = DarkMeloColors.surface2,
    outline = DarkMeloColors.border,
    outlineVariant = DarkMeloColors.borderStrong,
)

private val MeloLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF2D55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5E9),
    onPrimaryContainer = Color(0xFF9E002B),
    background = LightMeloColors.surface0,
    onBackground = LightMeloColors.textPrimary,
    surface = LightMeloColors.surface0,
    onSurface = LightMeloColors.textPrimary,
    surfaceVariant = LightMeloColors.surface1,
    onSurfaceVariant = LightMeloColors.textSecondary,
    surfaceContainer = LightMeloColors.surface1,
    surfaceContainerHigh = LightMeloColors.surface2,
    outline = LightMeloColors.border,
    outlineVariant = LightMeloColors.borderStrong,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeloTheme(
    accentColor: Color? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val themeAnimSpec = tween<Color>(durationMillis = 600, easing = FastOutSlowInEasing)
    val targetColors = if (isDarkTheme) DarkMeloColors else LightMeloColors

    val surface0 by animateColorAsState(targetColors.surface0, themeAnimSpec, label = "surface0")
    val surface1 by animateColorAsState(targetColors.surface1, themeAnimSpec, label = "surface1")
    val surface2 by animateColorAsState(targetColors.surface2, themeAnimSpec, label = "surface2")
    val border by animateColorAsState(targetColors.border, themeAnimSpec, label = "border")
    val borderStrong by animateColorAsState(targetColors.borderStrong, themeAnimSpec, label = "borderStrong")
    val textPrimary by animateColorAsState(targetColors.textPrimary, themeAnimSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(targetColors.textSecondary, themeAnimSpec, label = "textSecondary")
    val textMuted by animateColorAsState(targetColors.textMuted, themeAnimSpec, label = "textMuted")
    val glassSurface by animateColorAsState(targetColors.glassSurface, themeAnimSpec, label = "glassSurface")
    val glassFill by animateColorAsState(targetColors.glassFill, themeAnimSpec, label = "glassFill")
    val glassBorder by animateColorAsState(targetColors.glassBorder, themeAnimSpec, label = "glassBorder")
    val playerBarFill by animateColorAsState(targetColors.playerBarFill, themeAnimSpec, label = "playerBarFill")
    val playerBarBorder by animateColorAsState(targetColors.playerBarBorder, themeAnimSpec, label = "playerBarBorder")
    val chromePillFill by animateColorAsState(targetColors.chromePillFill, themeAnimSpec, label = "chromePillFill")
    val chromePillBorder by animateColorAsState(targetColors.chromePillBorder, themeAnimSpec, label = "chromePillBorder")

    val meloColors = com.github.adriianh.melo.util.AnimatedMeloColors(
        isDark = isDarkTheme,
        surface0 = surface0,
        surface1 = surface1,
        surface2 = surface2,
        border = border,
        borderStrong = borderStrong,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted,
        glassSurface = glassSurface,
        glassFill = glassFill,
        glassBorder = glassBorder,
        playerBarFill = playerBarFill,
        playerBarBorder = playerBarBorder,
        chromePillFill = chromePillFill,
        chromePillBorder = chromePillBorder
    )

    val rawPrimary = accentColor ?: Color(0xFFFF2D55)
    val harmonizedPrimary = ColorUtils.harmonize(rawPrimary, isDarkTheme)

    val animatedPrimary by animateColorAsState(
        targetValue = harmonizedPrimary,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ThemePrimaryAnimation"
    )

    val baseColorScheme = if (isDarkTheme) MeloDarkColorScheme else MeloLightColorScheme
    val colorScheme = baseColorScheme.copy(
        primary = animatedPrimary,
        primaryContainer = if (isDarkTheme) animatedPrimary.copy(alpha = 0.25f) else animatedPrimary.copy(alpha = 0.15f),
        background = surface0,
        surface = surface0,
        onBackground = textPrimary,
        onSurface = textPrimary,
        surfaceVariant = surface1,
        onSurfaceVariant = textSecondary,
        surfaceContainer = surface1,
        surfaceContainerHigh = surface2,
        outline = border,
        outlineVariant = borderStrong
    )

    val rippleConfiguration = if (isDarkTheme) MeloDarkRipple else MeloLightRipple

    CompositionLocalProvider(
        LocalMeloColors provides meloColors,
        LocalRippleConfiguration provides rippleConfiguration
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MeloType.meloTypography,
            content = content
        )
    }
}