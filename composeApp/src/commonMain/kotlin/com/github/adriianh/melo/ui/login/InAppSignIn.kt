package com.github.adriianh.melo.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun InAppSignInBrowser(
    modifier: Modifier = Modifier,
    onCookiesCaptured: (String) -> Unit = {}
)

expect fun isInAppSignInAvailable(): Boolean

expect fun sessionCookiesReady(): Boolean

expect fun captureSessionCookies(): String?

expect fun isAutomatedBrowserLoginAvailable(): Boolean

expect suspend fun launchAutomatedBrowserLogin(): String?

expect suspend fun importExistingBrowserCookies(): String?