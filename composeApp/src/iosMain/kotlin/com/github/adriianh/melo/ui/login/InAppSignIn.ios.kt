package com.github.adriianh.melo.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun InAppSignInBrowser(modifier: Modifier) = Unit

actual fun isInAppSignInAvailable(): Boolean = false

actual fun sessionCookiesReady(): Boolean = false

actual fun captureSessionCookies(): String? = null

actual fun isAutomatedBrowserLoginAvailable(): Boolean = false

actual suspend fun launchAutomatedBrowserLogin(): String? = null

actual suspend fun importExistingBrowserCookies(): String? = null