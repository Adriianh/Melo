package com.github.adriianh.melo.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicReference

private const val DEFAULT_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

private const val ANDROID_CHROME_UA =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

private object AndroidSessionCookieStore {
    val capturedCookies = AtomicReference<String?>(null)

    fun mergeYouTubeCookies(cookieManager: CookieManager): String? {
        cookieManager.flush()
        val cookieParts = linkedMapOf<String, String>()

        val urlsToCheck = listOf(
            "https://music.youtube.com",
            "https://www.youtube.com",
            "https://youtube.com",
            "https://accounts.google.com",
            "https://myaccount.google.com",
            "https://google.com",
            "https://www.google.com"
        )

        urlsToCheck.forEach { url ->
            val cookieStr = cookieManager.getCookie(url)
            if (!cookieStr.isNullOrBlank()) {
                cookieStr.split(";")
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .forEach { part ->
                        val separatorIndex = part.indexOf('=')
                        if (separatorIndex > 0) {
                            val key = part.substring(0, separatorIndex).trim()
                            val value = part.substring(separatorIndex + 1).trim()
                            if (key.isNotEmpty() && !cookieParts.containsKey(key)) {
                                cookieParts[key] = value
                            }
                        }
                    }
            }
        }

        capturedCookies.get()?.split(";")?.forEach { part ->
            val separatorIndex = part.indexOf('=')
            if (separatorIndex > 0) {
                val key = part.substring(0, separatorIndex).trim()
                val value = part.substring(separatorIndex + 1).trim()
                if (key.isNotEmpty() && !cookieParts.containsKey(key)) {
                    cookieParts[key] = value
                }
            }
        }

        val merged = cookieParts.takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString(separator = "; ") { (key, value) -> "$key=$value" }

        if (!merged.isNullOrBlank() && (merged.contains("SAPISID=") || merged.contains("__Secure-3PSID="))) {
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
actual fun InAppSignInBrowser(
    modifier: Modifier,
    onCookiesCaptured: (String) -> Unit
) {
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
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = ANDROID_CHROME_UA
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        checkAndDeliverCookies(cookieManager, view, onCookiesCaptured)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        checkAndDeliverCookies(cookieManager, view, onCookiesCaptured)
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                    ) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        checkAndDeliverCookies(cookieManager, view, onCookiesCaptured)
                    }
                }

                loadUrl(DEFAULT_LOGIN_URL)
            }
        }
    )
}

private fun checkAndDeliverCookies(
    cookieManager: CookieManager,
    webView: WebView?,
    onCookiesCaptured: (String) -> Unit
) {
    cookieManager.flush()
    val cookies = AndroidSessionCookieStore.mergeYouTubeCookies(cookieManager)
    if (!cookies.isNullOrBlank() && cookies.contains("SAPISID=")) {
        onCookiesCaptured(cookies)
        return
    }

    webView?.evaluateJavascript("document.cookie") { jsCookies ->
        val unquoted = jsCookies?.trim('"', ' ')
        if (!unquoted.isNullOrBlank() && unquoted != "null") {
            unquoted.split(";").forEach { part ->
                val separatorIndex = part.indexOf('=')
                if (separatorIndex > 0) {
                    val key = part.substring(0, separatorIndex).trim()
                    val value = part.substring(separatorIndex + 1).trim()
                    if (key.isNotEmpty()) {
                        val current = AndroidSessionCookieStore.capturedCookies.get() ?: ""
                        if (!current.contains("$key=")) {
                            AndroidSessionCookieStore.capturedCookies.set(
                                if (current.isEmpty()) "$key=$value" else "$current; $key=$value"
                            )
                        }
                    }
                }
            }
            val updated = AndroidSessionCookieStore.mergeYouTubeCookies(cookieManager)
            if (!updated.isNullOrBlank() && updated.contains("SAPISID=")) {
                onCookiesCaptured(updated)
            }
        }
    }
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