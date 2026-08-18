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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.util.ColorUtils
import com.github.adriianh.melo.util.DarkMeloColors
import com.github.adriianh.melo.util.LightMeloColors
import com.github.adriianh.melo.util.MeloColorScheme
import com.github.adriianh.melo.util.MeloType

val LocalMeloColors = staticCompositionLocalOf<MeloColorScheme> { DarkMeloColors }

@OptIn(ExperimentalMaterial3Api::class)
val MeloRippleConfiguration = RippleConfiguration(
    color = Color.White.copy(alpha = 0.15f),
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.06f,
        focusedAlpha = 0.08f,
        hoveredAlpha = 0.04f,
        pressedAlpha = 0.12f
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
    primaryContainer = Color(0xFFFFDADE),
    onPrimaryContainer = Color(0xFF3A101C),
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
        primaryContainer = if (isDarkTheme) animatedPrimary.copy(alpha = 0.25f) else animatedPrimary.copy(
            alpha = 0.15f
        ),
    )

    val meloColors = if (isDarkTheme) DarkMeloColors else LightMeloColors

    CompositionLocalProvider(
        LocalMeloColors provides meloColors,
        LocalRippleConfiguration provides MeloRippleConfiguration
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MeloType.meloTypography,
            content = content
        )
    }
}