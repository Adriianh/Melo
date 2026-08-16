package com.github.adriianh.melo.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.atomic.AtomicReference

private const val DEFAULT_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
private const val CHROME_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

object SessionCookieStore {
    private val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    private var installed = false
    val lastCapturedJsCookies = AtomicReference<String?>(null)

    fun install() {
        if (!installed) {
            CookieHandler.setDefault(cookieManager)
            installed = true
        }
    }

    fun sessionCookieHeader(): String? {
        val parts = LinkedHashMap<String, String>()
        val store = cookieManager.cookieStore
        for (cookie in store.cookies) {
            val domain = cookie.domain?.lowercase() ?: ""
            if (domain.contains("youtube.com") || domain.contains("google.com") || domain.isEmpty()) {
                parts[cookie.name] = cookie.value
            }
        }

        // Also merge any cookies extracted from JS document.cookie
        lastCapturedJsCookies.get()?.split(";")?.forEach { part ->
            val separatorIndex = part.indexOf('=')
            if (separatorIndex > 0) {
                val k = part.substring(0, separatorIndex).trim()
                val v = part.substring(separatorIndex + 1).trim()
                if (k.isNotEmpty() && !parts.containsKey(k)) {
                    parts[k] = v
                }
            }
        }

        return if (parts.isEmpty()) null else parts.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    fun hasSapisid(): Boolean {
        val header = sessionCookieHeader()
        return header?.contains("SAPISID=") == true
    }

    fun clear() {
        cookieManager.cookieStore.removeAll()
        lastCapturedJsCookies.set(null)
    }
}

@Composable
actual fun InAppSignInBrowser(modifier: Modifier) {
    SessionCookieStore.install()
    val jfxPanel = remember { JFXPanel() }
    DisposableEffect(Unit) {
        Platform.setImplicitExit(false)
        Platform.runLater {
            val webView = WebView()
            webView.engine.userAgent = CHROME_UA

            webView.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        val docCookies = webView.engine.executeScript("document.cookie") as? String
                        if (!docCookies.isNullOrBlank()) {
                            SessionCookieStore.lastCapturedJsCookies.set(docCookies)
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            webView.engine.load(DEFAULT_LOGIN_URL)
            jfxPanel.scene = Scene(webView)
        }
        onDispose {
            Platform.runLater { jfxPanel.scene = null }
        }
    }
    SwingPanel(modifier = modifier, factory = { jfxPanel })
}

actual fun isInAppSignInAvailable(): Boolean = true

actual fun sessionCookiesReady(): Boolean = SessionCookieStore.hasSapisid()

actual fun captureSessionCookies(): String? = SessionCookieStore.sessionCookieHeader()

actual fun isAutomatedBrowserLoginAvailable(): Boolean =
    BrowserAuthManager.findAvailableBrowser() != null

actual suspend fun launchAutomatedBrowserLogin(): String? =
    BrowserAuthManager.launchBrowserAuth()

actual suspend fun importExistingBrowserCookies(): String? =
    BrowserAuthManager.importExistingFirefoxCookies()