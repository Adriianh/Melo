@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun PlayerPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float?) -> Unit,
    onBack: () -> Unit,
)

@Composable
internal expect fun rememberPrefersReducedMotion(): Boolean

internal expect fun Modifier.idleCursorHidden(hidden: Boolean): Modifier

@Composable
internal expect fun KeepScreenOnEffect(enabled: Boolean)
