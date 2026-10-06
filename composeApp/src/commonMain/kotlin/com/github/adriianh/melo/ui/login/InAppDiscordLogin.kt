package com.github.adriianh.melo.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun isDiscordInAppLoginSupported(): Boolean

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
expect fun InAppDiscordLogin(
    modifier: Modifier = Modifier,
    onTokenCaptured: (String) -> Unit,
)
