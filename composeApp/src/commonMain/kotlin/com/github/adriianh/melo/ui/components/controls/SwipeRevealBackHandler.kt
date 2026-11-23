@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.controls

import androidx.compose.runtime.Composable

@Composable
internal expect fun SwipeRevealBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
