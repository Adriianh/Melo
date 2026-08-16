package com.github.adriianh.melo.ui.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.time.Duration.Companion.milliseconds

object BrowserAuthManager {

    private const val YOUTUBE_LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

    data class BrowserCandidate(
        val name: String,
        val path: String,
        val isFirefox: Boolean,
    )

    fun findAvailableBrowser(): BrowserCandidate? {
        val os = System.getProperty("os.name").lowercase()

        val candidates = when {
            os.contains("win") -> listOf(
                BrowserCandidate(
                    "Chrome",
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    false
                ),
                BrowserCandidate(
                    "Chrome (x86)",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                    false
                ),
                BrowserCandidate(
                    "Chrome (User)",
                    System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
                    false
                ),
                BrowserCandidate(
                    "Edge",
                    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                    false
                ),
                BrowserCandidate(
                    "Brave",
                    "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
                    false
                ),
                BrowserCandidate(
                    "Firefox",
                    "C:\\Program Files\\Mozilla Firefox\\firefox.exe",
                    true
                ),
            )

            os.contains("mac") -> listOf(
                BrowserCandidate(
                    "Chrome",
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    false
                ),
                BrowserCandidate(
                    "Brave",
                    "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser",
                    false
                ),
                BrowserCandidate(
                    "Edge",
                    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
                    false
                ),
                BrowserCandidate(
                    "Firefox",
                    "/Applications/Firefox.app/Contents/MacOS/firefox",
                    true
                ),
            )

            else -> listOf(
                BrowserCandidate("Chrome", "/usr/bin/google-chrome", false),
                BrowserCandidate("Chrome Stable", "/usr/bin/google-chrome-stable", false),
                BrowserCandidate("Chromium", "/usr/bin/chromium", false),
                BrowserCandidate("Chromium Browser", "/usr/bin/chromium-browser", false),
                BrowserCandidate("Brave", "/usr/bin/brave-browser", false),
                BrowserCandidate("Microsoft Edge", "/usr/bin/microsoft-edge", false),
                BrowserCandidate("Microsoft Edge Stable", "/usr/bin/microsoft-edge-stable", false),
                BrowserCandidate("Firefox", "/usr/bin/firefox", true),
                BrowserCandidate("Firefox Bin", "/usr/bin/firefox-bin", true),
            )
        }

        return candidates.firstOrNull { File(it.path).exists() && File(it.path).canExecute() }
    }

    suspend fun launchBrowserAuth(): String? = withContext(Dispatchers.IO) {
        val browser = findAvailableBrowser() ?: return@withContext null
        val tempDir = Files.createTempDirectory("melo_auth_").toFile()

        val process = try {
            if (browser.isFirefox) {
                ProcessBuilder(
                    browser.path,
                    "-no-remote",
                    "-profile",
                    tempDir.absolutePath,
                    YOUTUBE_LOGIN_URL
                ).start()
            } else {
                ProcessBuilder(
                    browser.path,
                    "--app=$YOUTUBE_LOGIN_URL",
                    "--user-data-dir=${tempDir.absolutePath}",
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--password-store=basic"
                ).start()
            }
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            return@withContext null
        }

        try {
            val startTime = System.currentTimeMillis()
            val timeout = 5 * 60 * 1000L

            while (System.currentTimeMillis() - startTime < timeout) {
                if (!process.isAlive && !hasAnyCookiesFile(tempDir, browser.isFirefox)) {
                    break
                }

                val cookies = extractCookiesFromDir(tempDir, browser.isFirefox)
                if (!cookies.isNullOrBlank() && cookies.contains("SAPISID=")) {
                    try {
                        process.destroy()
                    } catch (_: Exception) {
                    }
                    return@withContext cookies
                }

                delay(1000.milliseconds)
            }
            null
        } finally {
            try {
                if (process.isAlive) process.destroy()
            } catch (_: Exception) {
            }
            try {
                tempDir.deleteRecursively()
            } catch (_: Exception) {
            }
        }
    }

    private fun hasAnyCookiesFile(tempDir: File, isFirefox: Boolean): Boolean {
        return if (isFirefox) {
            File(tempDir, "cookies.sqlite").exists()
        } else {
            File(tempDir, "Default/Cookies").exists() || File(
                tempDir,
                "Default/Network/Cookies"
            ).exists()
        }
    }

    private fun extractCookiesFromDir(tempDir: File, isFirefox: Boolean): String? {
        val cookieDb = if (isFirefox) {
            File(tempDir, "cookies.sqlite")
        } else {
            listOf(
                File(tempDir, "Default/Network/Cookies"),
                File(tempDir, "Default/Cookies")
            ).firstOrNull { it.exists() }
        } ?: return null

        if (!cookieDb.exists() || cookieDb.length() == 0L) return null

        // Copy to avoid file lock by the running browser
        val tempCopy = File.createTempFile("cookies_read_", ".sqlite")
        return try {
            cookieDb.copyTo(tempCopy, overwrite = true)
            if (isFirefox) {
                readFirefoxCookies(tempCopy)
            } else {
                readChromiumCookies(tempCopy)
            }
        } catch (_: Exception) {
            null
        } finally {
            tempCopy.delete()
        }
    }

    private fun readFirefoxCookies(dbFile: File): String? {
        val parts = LinkedHashMap<String, String>()
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            val query =
                "SELECT name, value FROM moz_cookies WHERE host LIKE '%youtube.com' OR host LIKE '%google.com'"
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("name")
                        val value = rs.getString("value")
                        if (!name.isNullOrBlank() && !value.isNullOrBlank()) {
                            parts[name] = value
                        }
                    }
                }
            }
        }

        return if (parts.isEmpty()) null else parts.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun readChromiumCookies(dbFile: File): String? {
        val parts = LinkedHashMap<String, String>()
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            val query =
                "SELECT name, value FROM cookies WHERE (host_key LIKE '%youtube.com' OR host_key LIKE '%google.com')"
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("name")
                        val value = rs.getString("value")
                        if (!name.isNullOrBlank() && !value.isNullOrBlank()) {
                            parts[name] = value
                        }
                    }
                }
            }
        }

        return if (parts.isEmpty()) null else parts.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    suspend fun importExistingFirefoxCookies(): String? = withContext(Dispatchers.IO) {
        val home = System.getProperty("user.home") ?: return@withContext null
        val os = System.getProperty("os.name").lowercase()

        val firefoxProfileDirs = when {
            os.contains("win") -> listOf(
                File(System.getenv("APPDATA") ?: "", "Mozilla\\Firefox\\Profiles")
            )

            os.contains("mac") -> listOf(
                File(home, "Library/Application Support/Firefox/Profiles")
            )

            else -> listOf(
                File(home, ".mozilla/firefox"),
                File(home, ".var/app/org.mozilla.firefox/.mozilla/firefox"),
                File(home, "snap/firefox/common/.mozilla/firefox")
            )
        }

        for (baseDir in firefoxProfileDirs) {
            if (!baseDir.exists() || !baseDir.isDirectory) continue
            val profileDirs = baseDir.listFiles { f -> f.isDirectory } ?: continue
            for (profile in profileDirs) {
                val cookieDb = File(profile, "cookies.sqlite")
                if (cookieDb.exists() && cookieDb.length() > 0L) {
                    val tempCopy = File.createTempFile("ff_import_", ".sqlite")
                    try {
                        cookieDb.copyTo(tempCopy, overwrite = true)
                        val cookies = readFirefoxCookies(tempCopy)
                        if (!cookies.isNullOrBlank() && cookies.contains("SAPISID=")) {
                            return@withContext cookies
                        }
                    } catch (_: Exception) {
                    } finally {
                        tempCopy.delete()
                    }
                }
            }
        }

        null
    }
}