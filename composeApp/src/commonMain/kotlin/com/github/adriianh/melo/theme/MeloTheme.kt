package com.github.adriianh.melo.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

val LocalMeloColors = staticCompositionLocalOf { MeloColors }

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
    background = MeloColors.surface0,
    onBackground = MeloColors.textPrimary,
    surface = MeloColors.surface0,
    onSurface = MeloColors.textPrimary,
    surfaceVariant = MeloColors.surface1,
    onSurfaceVariant = MeloColors.textSecondary,
    surfaceContainer = MeloColors.surface1,
    surfaceContainerHigh = MeloColors.surface2,
    outline = MeloColors.border,
    outlineVariant = MeloColors.borderStrong,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeloTheme(
    accentColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val targetPrimary = accentColor ?: Color(0xFFFF2D55)
    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ThemePrimaryAnimation"
    )

    val colorScheme = MeloDarkColorScheme.copy(
        primary = animatedPrimary,
        primaryContainer = animatedPrimary.copy(alpha = 0.25f),
    )

    CompositionLocalProvider(
        LocalMeloColors provides MeloColors,
        LocalRippleConfiguration provides MeloRippleConfiguration
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MeloType.meloTypography,
            content = content
        )
    }
}
