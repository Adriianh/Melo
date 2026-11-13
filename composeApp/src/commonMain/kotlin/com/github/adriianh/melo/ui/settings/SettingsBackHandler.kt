@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.runtime.Composable

@Composable
internal expect fun SettingsBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
