package com.github.adriianh.melo.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.github.adriianh.melo.ui.login.CookieHeader.hasSapisid
import com.github.adriianh.melo.ui.login.CookieHeader.toHeaderStringOrNull
import com.github.adriianh.melo.ui.login.SessionCookieStore.install
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import java.util.logging.Logger

private const val DEFAULT_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
private const val CHROME_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

private val log = Logger.getLogger("Melo.InAppSignIn")

/**
 * Holds the JVM-wide cookie jar used to capture the YouTube/Google session from the login
 * [WebView].
 *
 * Note: [install] replaces the JVM's *default* [CookieHandler]. JavaFX's WebEngine consults this
 * default handler for its own networking, which is why this is global rather than scoped to a
 * single WebView -- but it also means it can affect any other code in the process that relies on
 * `CookieHandler.getDefault()`. If Melo's HTTP client (Ktor/OkHttp/etc.) manages its own cookie
 * jar independently, this has no effect on it; if it *also* falls back to the JVM default, be
 * aware the two will share a state.
 */
object SessionCookieStore {
    private val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    private val lastCapturedJsCookies = AtomicReference<String?>(null)
    private val lock = Any()

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        synchronized(lock) {
            if (installed) return
            CookieHandler.setDefault(cookieManager)
            installed = true
        }
    }

    /** Merges cookies captured via `document.cookie` (JS) with those in the Java cookie jar. */
    fun recordJsCookies(rawDocumentCookie: String) {
        lastCapturedJsCookies.set(rawDocumentCookie)
    }

    fun sessionCookies(): Map<String, String> = synchronized(lock) {
        val cookies = LinkedHashMap<String, String>()
        cookieManager.cookieStore.cookies
            .filter { cookie ->
                val domain = cookie.domain?.lowercase().orEmpty()
                domain.contains("youtube.com") || domain.contains("google.com") || domain.isEmpty()
            }
            .forEach { cookies[it.name] = it.value }

        lastCapturedJsCookies.get()?.let { raw ->
            CookieHeader.parse(raw).forEach { (name, value) -> cookies.putIfAbsent(name, value) }
        }
        cookies
    }

    fun sessionCookieHeader(): String? = sessionCookies().toHeaderStringOrNull()

    fun hasSapisid(): Boolean = sessionCookies().hasSapisid()

    fun clear() {
        synchronized(lock) { cookieManager.cookieStore.removeAll() }
        lastCapturedJsCookies.set(null)
    }
}

/**
 * Owns the lifecycle of the login [WebView] and reports a captured session back to the caller.
 * Kept separate from the `@Composable` so the capture logic can be reasoned about (and tested)
 * independently of Compose's lifecycle.
 */
private class YouTubeLoginWebViewController(private val onCookiesCaptured: (String) -> Unit) {

    val jfxPanel = JFXPanel()

    fun start() {
        install()
        Platform.setImplicitExit(false)
        Platform.runLater {
            val webView = WebView().apply { engine.userAgent = CHROME_UA }
            webView.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                if (newState == Worker.State.SUCCEEDED) onPageLoaded(webView)
            }
            webView.engine.load(DEFAULT_LOGIN_URL)
            jfxPanel.scene = Scene(webView)
        }
    }

    private fun onPageLoaded(webView: WebView) {
        val documentCookie =
            runCatching { webView.engine.executeScript("document.cookie") as? String }
                .onFailure {
                    log.log(
                        Level.FINE,
                        "Could not read document.cookie from login WebView",
                        it
                    )
                }
                .getOrNull()

        if (documentCookie.isNullOrBlank()) return
        SessionCookieStore.recordJsCookies(documentCookie)

        val sessionHeader = SessionCookieStore.sessionCookieHeader()
        if (sessionHeader != null && SessionCookieStore.hasSapisid()) {
            onCookiesCaptured(sessionHeader)
        }
    }

    fun stop() {
        Platform.runLater { jfxPanel.scene = null }
    }
}

@Composable
actual fun InAppSignInBrowser(
    modifier: Modifier,
    onCookiesCaptured: (String) -> Unit,
) {
    val controller = remember { YouTubeLoginWebViewController(onCookiesCaptured) }
    DisposableEffect(Unit) {
        controller.start()
        onDispose { controller.stop() }
    }
    SwingPanel(modifier = modifier, factory = { controller.jfxPanel })
}

actual fun isInAppSignInAvailable(): Boolean = true

actual fun sessionCookiesReady(): Boolean = SessionCookieStore.hasSapisid()

actual fun captureSessionCookies(): String? = SessionCookieStore.sessionCookieHeader()

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
        log.log(Level.WARNING, "$context failed", cause)
        null
    }
}