package com.github.adriianh.melo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

val LocalMeloColors = staticCompositionLocalOf { MeloColors }

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

@Composable
fun MeloTheme(
    accentColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (accentColor != null) {
        MeloDarkColorScheme.copy(
            primary = accentColor,
            primaryContainer = accentColor.copy(alpha = 0.25f),
        )
    } else {
        MeloDarkColorScheme
    }

    CompositionLocalProvider(LocalMeloColors provides MeloColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MeloType.meloTypography,
            content = content
        )
    }
}
