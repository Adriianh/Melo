package com.github.adriianh.melo.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun isDiscordInAppLoginSupported(): Boolean = false

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
actual fun InAppDiscordLogin(
    modifier: Modifier,
    onTokenCaptured: (String) -> Unit,
) = Unit
