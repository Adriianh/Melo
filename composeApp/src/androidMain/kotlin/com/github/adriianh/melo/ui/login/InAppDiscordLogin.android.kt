package com.github.adriianh.melo.ui.login

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val DISCORD_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

private const val EXTRACT_TOKEN_JS =
    """
    (function() {
        try {
            let iframe = document.createElement('iframe');
            document.head.appendChild(iframe);
            let token = iframe.contentWindow.localStorage.getItem('token');
            iframe.remove();
            return token ? token.replace(/"/g, '') : null;
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
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setupCookieManager()
                setupSettings()
                webViewClient = createDiscordWebViewClient(onTokenCaptured)
                loadUrl("https://discord.com/login")
            }
        },
    )
}

private fun WebView.setupCookieManager() {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(this, true)
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupSettings() {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        userAgentString = DISCORD_MOBILE_USER_AGENT
    }
}

private fun createDiscordWebViewClient(onTokenCaptured: (String) -> Unit): WebViewClient =
    object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
        ): WebResourceResponse? {
            request?.requestHeaders?.let { headers ->
                val auth = headers["Authorization"] ?: headers["authorization"]
                if (!auth.isNullOrBlank() && !auth.startsWith("Bot ", ignoreCase = true)) {
                    view?.post { onTokenCaptured(auth.trim('"', ' ')) }
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageFinished(
            view: WebView?,
            url: String?,
        ) {
            super.onPageFinished(view, url)
            view?.evaluateJavascript(EXTRACT_TOKEN_JS.trimIndent()) { result ->
                val token = result?.trim('"', ' ')
                if (!token.isNullOrBlank() && token != "null") {
                    onTokenCaptured(token)
                }
            }
        }
    }
