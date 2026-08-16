package com.github.adriianh.melo.ui.login

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicReference

private const val DEFAULT_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

private object AndroidSessionCookieStore {
    val capturedCookies = AtomicReference<String?>(null)

    fun mergeYouTubeCookies(cookieManager: CookieManager): String? {
        val cookieParts = linkedMapOf<String, String>()

        listOf(
            "https://music.youtube.com",
            "https://www.youtube.com",
            "https://youtube.com",
        ).forEach { url ->
            cookieManager.getCookie(url)
                ?.split(";")
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                ?.forEach { part ->
                    val separatorIndex = part.indexOf('=')
                    if (separatorIndex <= 0) return@forEach

                    val key = part.substring(0, separatorIndex).trim()
                    val value = part.substring(separatorIndex + 1).trim()
                    if (key.isNotEmpty()) {
                        cookieParts[key] = value
                    }
                }
        }

        val merged = cookieParts.takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString(separator = "; ") { (key, value) -> "$key=$value" }

        if (!merged.isNullOrBlank() && merged.contains("SAPISID=")) {
            capturedCookies.set(merged)
        }

        return merged
    }

    fun hasSapisid(): Boolean {
        val cookieManager = CookieManager.getInstance()
        val current = mergeYouTubeCookies(cookieManager)
        return current?.contains("SAPISID=") == true || capturedCookies.get()?.contains("SAPISID=") == true
    }

    fun clear() {
        capturedCookies.set(null)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun InAppSignInBrowser(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false

                    // Remove '; wv' and 'Version/4.0' to prevent Google's disallowed_useragent block
                    val defaultUa = userAgentString
                    userAgentString = defaultUa
                        .replace("; wv", "")
                        .replace("Version/4.0 ", "")
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        AndroidSessionCookieStore.mergeYouTubeCookies(cookieManager)
                    }
                }

                loadUrl(DEFAULT_LOGIN_URL)
            }
        }
    )
}

actual fun isInAppSignInAvailable(): Boolean = true

actual fun sessionCookiesReady(): Boolean = AndroidSessionCookieStore.hasSapisid()

actual fun captureSessionCookies(): String? {
    val cookieManager = CookieManager.getInstance()
    val merged = AndroidSessionCookieStore.mergeYouTubeCookies(cookieManager)
    return merged ?: AndroidSessionCookieStore.capturedCookies.get()
}

actual fun isAutomatedBrowserLoginAvailable(): Boolean = false

actual suspend fun launchAutomatedBrowserLogin(): String? = null

actual suspend fun importExistingBrowserCookies(): String? = null