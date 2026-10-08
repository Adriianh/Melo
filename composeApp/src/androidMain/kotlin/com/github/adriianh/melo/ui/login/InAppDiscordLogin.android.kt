package com.github.adriianh.melo.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val FALLBACK_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

private const val DISCORD_DARK_BG_COLOR = 0xFF313338.toInt()

private const val EXTRACT_TOKEN_JS =
    """
    (function() {
        try {
            let iframe = document.createElement('iframe');
            document.head.appendChild(iframe);
            let token = iframe.contentWindow.localStorage.getItem('token') ||
                iframe.contentWindow.localStorage.token;
            iframe.remove();
            if (token) return token.replace(/"/g, '');
            let direct = window.localStorage.getItem('token');
            return direct ? direct.replace(/"/g, '') : null;
        } catch (e) {
            return null;
        }
    })()
    """

actual fun isDiscordInAppLoginSupported(): Boolean = true

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun InAppDiscordLogin(
    modifier: Modifier,
    onTokenCaptured: (String) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    setBackgroundColor(DISCORD_DARK_BG_COLOR)
                    setupCookieManager()
                    setupSettings()
                    webChromeClient =
                        object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true

                            override fun onProgressChanged(
                                view: WebView?,
                                newProgress: Int,
                            ) {
                                if (newProgress >= 80) {
                                    isLoading = false
                                }
                            }
                        }
                    webViewClient =
                        createDiscordWebViewClient(
                            onLoadingFinished = { isLoading = false },
                            onTokenCaptured = onTokenCaptured,
                        )
                    loadUrl("https://discord.com/login")
                }
            },
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun WebView.setupCookieManager() {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(this, true)
}

@Suppress("DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupSettings() {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        allowContentAccess = true
        allowFileAccess = false
        cacheMode = WebSettings.LOAD_DEFAULT

        val defaultUa = userAgentString
        userAgentString =
            if (defaultUa.contains("Chrome/")) {
                defaultUa.replace("; wv", "").replace("Version/4.0 ", "")
            } else {
                FALLBACK_USER_AGENT
            }
    }
}

private fun createDiscordWebViewClient(
    onLoadingFinished: () -> Unit,
    onTokenCaptured: (String) -> Unit,
): WebViewClient =
    object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            val url = request?.url?.toString() ?: return false
            if (url.startsWith("intent:") || url.startsWith("discord:")) {
                return true
            }
            return false
        }

        override fun onPageStarted(
            view: WebView?,
            url: String?,
            favicon: Bitmap?,
        ) {
            super.onPageStarted(view, url, favicon)
            checkAndExtractToken(view, onTokenCaptured)
        }

        override fun onPageFinished(
            view: WebView?,
            url: String?,
        ) {
            super.onPageFinished(view, url)
            onLoadingFinished()
            checkAndExtractToken(view, onTokenCaptured)
        }

        override fun doUpdateVisitedHistory(
            view: WebView?,
            url: String?,
            isReload: Boolean,
        ) {
            super.doUpdateVisitedHistory(view, url, isReload)
            checkAndExtractToken(view, onTokenCaptured)
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
        ): WebResourceResponse? {
            try {
                request?.requestHeaders?.let { headers ->
                    val auth = headers["Authorization"] ?: headers["authorization"]
                    if (!auth.isNullOrBlank() && !auth.startsWith("Bot ", ignoreCase = true)) {
                        view?.post { onTokenCaptured(auth.trim('"', ' ')) }
                    }
                }
            } catch (_: Throwable) {
            }
            return super.shouldInterceptRequest(view, request)
        }
    }

private fun checkAndExtractToken(
    view: WebView?,
    onTokenCaptured: (String) -> Unit,
) {
    view?.evaluateJavascript(EXTRACT_TOKEN_JS.trimIndent()) { result ->
        val token = result?.trim('"', ' ')
        if (!token.isNullOrBlank() && token != "null" && token != "undefined") {
            onTokenCaptured(token)
        }
    }
}
