package com.github.adriianh.cli.service

import com.github.adriianh.core.domain.usecase.login.SetSessionCookiesUseCase
import com.github.adriianh.core.domain.usecase.login.VerifySessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.data.auth.BrowserAuthManager
import com.github.adriianh.data.auth.BrowserAuthResult
import com.github.adriianh.data.auth.CookieHeader
import com.github.adriianh.data.auth.CookieHeader.toHeaderStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YouTubeAuthStatus(
    val isLoggedIn: Boolean,
    val accountName: String? = null,
    val cookiesConfigured: Boolean = false,
    val error: String? = null,
)

class YouTubeAuthService(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val setSessionCookies: SetSessionCookiesUseCase,
    private val verifySession: VerifySessionUseCase,
) {

    suspend fun getStatus(): YouTubeAuthStatus = withContext(Dispatchers.IO) {
        val cookies = getSettings.getSnapshot().sessionCookies
        if (cookies.isNullOrBlank()) {
            return@withContext YouTubeAuthStatus(
                isLoggedIn = false,
                cookiesConfigured = false,
            )
        }

        setSessionCookies(cookies)
        val name = runCatching { verifySession() }.getOrNull()
        if (name != null) {
            YouTubeAuthStatus(
                isLoggedIn = true,
                accountName = name,
                cookiesConfigured = true,
            )
        } else {
            YouTubeAuthStatus(
                isLoggedIn = false,
                cookiesConfigured = true,
                error = "Cookies are saved, but failed to authenticate with YouTube Music.",
            )
        }
    }

    suspend fun importFromBrowser(): Result<String> = withContext(Dispatchers.IO) {
        val result = BrowserAuthManager.importExistingBrowserCookies()
        handleAuthResult(result, "browser cookie import")
    }

    suspend fun launchBrowserLogin(): Result<String> = withContext(Dispatchers.IO) {
        val result = BrowserAuthManager.launchBrowserAuth()
        handleAuthResult(result, "browser sign-in")
    }

    suspend fun loginWithCookies(rawCookies: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = rawCookies.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cookies cannot be empty"))
        }

        val parsed = CookieHeader.parse(trimmed)
        val formattedHeader = parsed.toHeaderStringOrNull() ?: trimmed

        saveSessionCookies(formattedHeader)
    }

    suspend fun loginWithCookiesFile(file: java.io.File): Result<String> =
        withContext(Dispatchers.IO) {
            if (!file.exists() || !file.isFile) {
                return@withContext Result.failure(IllegalArgumentException("File not found: ${file.absolutePath}"))
            }

            val lines = file.readLines()
            val cookieMap = LinkedHashMap<String, String>()
            var isNetscape = false

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val parts = trimmed.split("\t")
                if (parts.size >= 7) {
                    isNetscape = true
                    val name = parts[5].trim()
                    val value = parts[6].trim()
                    if (name.isNotEmpty()) {
                        cookieMap[name] = value
                    }
                }
            }

            val cookieString = if (isNetscape && cookieMap.isNotEmpty()) {
                CookieHeader.run { cookieMap.toHeaderStringOrNull() } ?: ""
            } else {
                file.readText().trim()
            }

            loginWithCookies(cookieString)
        }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            setSessionCookies(null)
            updateSettings { it.copy(sessionCookies = null) }
        }
    }

    private suspend fun handleAuthResult(
        result: BrowserAuthResult,
        contextName: String,
    ): Result<String> = when (result) {
        is BrowserAuthResult.Success -> saveSessionCookies(result.cookieHeader)
        is BrowserAuthResult.NoBrowserFound -> Result.failure(
            IllegalStateException("No supported browser with an active YouTube Music session was found ($contextName).")
        )

        is BrowserAuthResult.TimedOut -> Result.failure(
            IllegalStateException("Authentication timed out waiting for YouTube Music session ($contextName).")
        )

        is BrowserAuthResult.Failed -> Result.failure(result.cause)
    }

    private suspend fun saveSessionCookies(cookies: String): Result<String> {
        setSessionCookies(cookies)
        val accountName = runCatching { verifySession() }.getOrNull()
        return if (accountName != null) {
            updateSettings { it.copy(sessionCookies = cookies) }
            Result.success(accountName)
        } else {
            val previous = getSettings.getSnapshot().sessionCookies
            setSessionCookies(previous)
            Result.failure(
                IllegalStateException("The cookies did not produce a valid YouTube Music session. Make sure you are logged in to music.youtube.com.")
            )
        }
    }
}