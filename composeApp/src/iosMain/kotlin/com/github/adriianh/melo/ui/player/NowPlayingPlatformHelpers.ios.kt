@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlayerPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float?) -> Unit,
    onBack: () -> Unit,
) = Unit

@Composable
internal actual fun rememberPrefersReducedMotion(): Boolean = false

internal actual fun Modifier.idleCursorHidden(hidden: Boolean): Modifier = this

@Composable
internal actual fun KeepScreenOnEffect(enabled: Boolean) = Unit
