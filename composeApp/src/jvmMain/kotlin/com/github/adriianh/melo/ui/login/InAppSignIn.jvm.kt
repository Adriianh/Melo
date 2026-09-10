package com.github.adriianh.melo.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.logging.Logger

private val log = Logger.getLogger("Melo.InAppSignIn")

/**
 * In-app browser sign-in is **not available** on JVM/Desktop.
 *
 * Instead, Melo uses the system browser via [BrowserAuthManager] which:
 * - Launches a disposable browser profile for login ([launchAutomatedBrowserLogin])
 * - Imports cookies from the user's existing browser ([importExistingBrowserCookies])
 *
 * This avoids the heavy JavaFX WebView runtime (~100 MB) that was previously required.
 */
@Composable
actual fun InAppSignInBrowser(
    modifier: Modifier,
    onCookiesCaptured: (String) -> Unit,
) {
    // No-op: in-app WebView is not available on desktop.
    // The LoginDialog UI will not render this composable because
    // isInAppSignInAvailable() returns false.
}

actual fun isInAppSignInAvailable(): Boolean = false

actual fun sessionCookiesReady(): Boolean = false

actual fun captureSessionCookies(): String? = null

actual fun isAutomatedBrowserLoginAvailable(): Boolean =
    BrowserAuthManager.findAvailableBrowser() != null

actual suspend fun launchAutomatedBrowserLogin(): String? =
    BrowserAuthManager.launchBrowserAuth().toCookieHeaderOrNull("automated browser login")

actual suspend fun importExistingBrowserCookies(): String? =
    BrowserAuthManager.importExistingBrowserCookies()
        .toCookieHeaderOrNull("existing-profile cookie import")

/**
 * Adapts [BrowserAuthResult] to the `String?` contract expected by the `expect` declarations,
 * logging *why* a null is being returned instead of losing that information silently.
 */
private fun BrowserAuthResult.toCookieHeaderOrNull(context: String): String? = when (this) {
    is BrowserAuthResult.Success -> cookieHeader
    is BrowserAuthResult.NoBrowserFound -> {
        log.info("$context: no supported browser found on this system")
        null
    }

    is BrowserAuthResult.TimedOut -> {
        log.info("$context: timed out waiting for a session cookie")
        null
    }

    is BrowserAuthResult.Failed -> {
        log.log(java.util.logging.Level.WARNING, "$context failed", cause)
        null
    }
}