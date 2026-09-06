package com.github.adriianh.melo.ui.login

import com.github.adriianh.melo.ui.login.BrowserAuthManager.LAUNCH_TIMEOUT
import com.github.adriianh.melo.ui.login.CookieHeader.hasSapisid
import com.github.adriianh.melo.ui.login.CookieHeader.toHeaderStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** How a browser renders pages; determines which cookie-store format/decryption path applies. */
enum class BrowserEngine { CHROMIUM, GECKO }

data class BrowserCandidate(
    val name: String,
    val path: String,
    val engine: BrowserEngine,
    /** macOS Keychain service name for this browser's cookie-encryption key. Chromium-only. */
    val macKeychainService: String = "$name Safe Storage",
)

/** Outcome of an attempt to get a YouTube/Google session via the system browser. */
sealed interface BrowserAuthResult {
    data class Success(val cookieHeader: String) : BrowserAuthResult
    data object NoBrowserFound : BrowserAuthResult
    data object TimedOut : BrowserAuthResult
    data class Failed(val cause: Throwable) : BrowserAuthResult
}

/**
 * Locates an installed browser and uses it -- either by launching a disposable, logged-in
 * instance or by reading cookies out of the user's existing browser profile -- to get a
 * YouTube Music / Google session cookie header.
 */
object BrowserAuthManager {

    private const val YOUTUBE_LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

    private val LAUNCH_TIMEOUT = 5.minutes
    private val POLL_INTERVAL = 1.seconds

    private const val FIREFOX_COOKIE_QUERY =
        "SELECT name, value FROM moz_cookies WHERE host LIKE '%youtube.com' OR host LIKE '%google.com'"
    private const val CHROMIUM_COOKIE_QUERY =
        "SELECT name, value, encrypted_value FROM cookies " +
                "WHERE host_key LIKE '%youtube.com' OR host_key LIKE '%google.com'"

    private enum class WinRoot(private val envVar: String?, private val fallback: String) {
        PROGRAM_FILES("ProgramFiles", "C:\\Program Files"),
        PROGRAM_FILES_X86("ProgramFiles(x86)", "C:\\Program Files (x86)"),
        LOCAL_APP_DATA("LOCALAPPDATA", ""),
        ;

        fun resolve(): String = envVar?.let { System.getenv(it) } ?: fallback
    }

    private data class WindowsBrowserSpec(
        val name: String,
        val engine: BrowserEngine,
        /** Path relative to each of [roots], Windows-style separators, e.g. `Google\Chrome\...`. */
        val relativeExePath: String,
        val roots: List<WinRoot> = listOf(
            WinRoot.PROGRAM_FILES,
            WinRoot.PROGRAM_FILES_X86,
            WinRoot.LOCAL_APP_DATA
        ),
        val macKeychainService: String = "$name Safe Storage",
    )

    private val windowsBrowserSpecs = listOf(
        WindowsBrowserSpec(
            "Google Chrome",
            BrowserEngine.CHROMIUM,
            "Google\\Chrome\\Application\\chrome.exe",
            macKeychainService = "Chrome Safe Storage"
        ),
        WindowsBrowserSpec(
            "Microsoft Edge",
            BrowserEngine.CHROMIUM,
            "Microsoft\\Edge\\Application\\msedge.exe"
        ),
        WindowsBrowserSpec(
            "Brave",
            BrowserEngine.CHROMIUM,
            "BraveSoftware\\Brave-Browser\\Application\\brave.exe"
        ),
        WindowsBrowserSpec(
            "Vivaldi",
            BrowserEngine.CHROMIUM,
            "Vivaldi\\Application\\vivaldi.exe",
            roots = listOf(WinRoot.LOCAL_APP_DATA)
        ),
        WindowsBrowserSpec(
            "Mozilla Firefox",
            BrowserEngine.GECKO,
            "Mozilla Firefox\\firefox.exe",
            roots = listOf(WinRoot.PROGRAM_FILES, WinRoot.PROGRAM_FILES_X86)
        ),
        WindowsBrowserSpec(
            "LibreWolf",
            BrowserEngine.GECKO,
            "LibreWolf\\librewolf.exe",
            roots = listOf(WinRoot.PROGRAM_FILES)
        ),
        WindowsBrowserSpec(
            "Floorp",
            BrowserEngine.GECKO,
            "Floorp\\floorp.exe",
            roots = listOf(WinRoot.LOCAL_APP_DATA)
        ),
        WindowsBrowserSpec(
            "Zen Browser",
            BrowserEngine.GECKO,
            "Zen Browser\\zen.exe",
            roots = listOf(WinRoot.LOCAL_APP_DATA)
        ),
    )

    private fun resolveWindowsCandidates(): List<BrowserCandidate> =
        windowsBrowserSpecs.flatMap { spec ->
            spec.roots.map { root ->
                BrowserCandidate(
                    spec.name,
                    "${root.resolve()}\\${spec.relativeExePath}",
                    spec.engine,
                    spec.macKeychainService
                )
            }
        }

    /** macOS app locations all follow `/Applications/<bundle>.app/Contents/MacOS/<executable>`. */
    private data class MacBrowserSpec(
        val name: String,
        val engine: BrowserEngine,
        val bundleName: String,
        val executableName: String = bundleName,
        val macKeychainService: String = "$name Safe Storage",
    )

    private val macBrowserSpecs = listOf(
        MacBrowserSpec(
            "Google Chrome",
            BrowserEngine.CHROMIUM,
            bundleName = "Google Chrome",
            macKeychainService = "Chrome Safe Storage"
        ),
        MacBrowserSpec("Brave", BrowserEngine.CHROMIUM, bundleName = "Brave Browser"),
        MacBrowserSpec("Microsoft Edge", BrowserEngine.CHROMIUM, bundleName = "Microsoft Edge"),
        MacBrowserSpec("Arc", BrowserEngine.CHROMIUM, bundleName = "Arc"),
        MacBrowserSpec("Vivaldi", BrowserEngine.CHROMIUM, bundleName = "Vivaldi"),
        MacBrowserSpec("Opera", BrowserEngine.CHROMIUM, bundleName = "Opera"),
        MacBrowserSpec(
            "Mozilla Firefox",
            BrowserEngine.GECKO,
            bundleName = "Firefox",
            executableName = "firefox"
        ),
        MacBrowserSpec(
            "LibreWolf",
            BrowserEngine.GECKO,
            bundleName = "LibreWolf",
            executableName = "librewolf"
        ),
        MacBrowserSpec(
            "Zen Browser",
            BrowserEngine.GECKO,
            bundleName = "Zen Browser",
            executableName = "zen"
        ),
    )

    private fun resolveMacCandidates(): List<BrowserCandidate> =
        macBrowserSpecs.map { spec ->
            BrowserCandidate(
                spec.name,
                "/Applications/${spec.bundleName}.app/Contents/MacOS/${spec.executableName}",
                spec.engine,
                spec.macKeychainService,
            )
        }

    /** Linux installs are mostly a bare binary name under a handful of bin roots, plus optional Flatpak/Snap. */
    private data class LinuxBrowserSpec(
        val name: String,
        val engine: BrowserEngine,
        val binNames: List<String> = emptyList(),
        val flatpakId: String? = null,
        val snapName: String? = null,
        val macKeychainService: String = "$name Safe Storage",
    )

    private val linuxBrowserSpecs = listOf(
        LinuxBrowserSpec(
            "Google Chrome",
            BrowserEngine.CHROMIUM,
            binNames = listOf("google-chrome", "google-chrome-stable"),
            flatpakId = "com.google.Chrome",
            macKeychainService = "Chrome Safe Storage"
        ),
        LinuxBrowserSpec(
            "Chromium",
            BrowserEngine.CHROMIUM,
            binNames = listOf("chromium", "chromium-browser"),
            snapName = "chromium"
        ),
        LinuxBrowserSpec(
            "Brave",
            BrowserEngine.CHROMIUM,
            binNames = listOf("brave-browser", "brave"),
            flatpakId = "com.brave.Browser"
        ),
        LinuxBrowserSpec(
            "Microsoft Edge",
            BrowserEngine.CHROMIUM,
            binNames = listOf("microsoft-edge", "microsoft-edge-stable"),
            flatpakId = "com.microsoft.Edge"
        ),
        LinuxBrowserSpec(
            "Vivaldi",
            BrowserEngine.CHROMIUM,
            binNames = listOf("vivaldi", "vivaldi-stable")
        ),
        LinuxBrowserSpec("Opera", BrowserEngine.CHROMIUM, binNames = listOf("opera")),
        LinuxBrowserSpec(
            "Firefox",
            BrowserEngine.GECKO,
            binNames = listOf("firefox", "firefox-esr", "firefox-bin"),
            flatpakId = "org.mozilla.firefox",
            snapName = "firefox"
        ),
        LinuxBrowserSpec("LibreWolf", BrowserEngine.GECKO, binNames = listOf("librewolf")),
        LinuxBrowserSpec("Floorp", BrowserEngine.GECKO, binNames = listOf("floorp")),
        LinuxBrowserSpec("Zen Browser", BrowserEngine.GECKO, binNames = listOf("zen-browser")),
    )

    private val linuxBinRoots: List<String>
        get() {
            val staticRoots = listOf("/usr/bin", "/usr/local/bin")
            val pathRoots =
                System.getenv("PATH").orEmpty().split(File.pathSeparator).filter { it.isNotBlank() }
            return (staticRoots + pathRoots).distinct()
        }
    private const val FLATPAK_BIN_ROOT = "/var/lib/flatpak/exports/bin"
    private const val SNAP_BIN_ROOT = "/snap/bin"

    private fun resolveLinuxCandidates(): List<BrowserCandidate> =
        linuxBrowserSpecs.flatMap { spec ->
            val binPaths =
                spec.binNames.flatMap { bin -> linuxBinRoots.map { root -> "$root/$bin" } }
            val packagedPaths = listOfNotNull(
                spec.flatpakId?.let { "$FLATPAK_BIN_ROOT/$it" },
                spec.snapName?.let { "$SNAP_BIN_ROOT/${spec.snapName}" },
            )
            (binPaths + packagedPaths).map { path ->
                BrowserCandidate(spec.name, path, spec.engine, spec.macKeychainService)
            }
        }

    /** Finds the first installed, executable browser candidate for the current OS, if any. */
    fun findAvailableBrowser(): BrowserCandidate? {
        val candidates = when (HostOs.current) {
            HostOs.WINDOWS -> resolveWindowsCandidates()
            HostOs.MACOS -> resolveMacCandidates()
            HostOs.LINUX -> resolveLinuxCandidates()
        }
        return candidates.firstOrNull { candidate ->
            val file = File(candidate.path)
            file.exists() && file.canExecute()
        }
    }

    /**
     * Launches an available browser against a throwaway profile so the user can log in, then
     * polls that profile's cookie store until a session appears (or [LAUNCH_TIMEOUT] elapses).
     */
    suspend fun launchBrowserAuth(): BrowserAuthResult = withContext(Dispatchers.IO) {
        val browser = findAvailableBrowser() ?: return@withContext BrowserAuthResult.NoBrowserFound
        val tempDir = Files.createTempDirectory("melo_auth_").toFile()

        val process = runCatching { startBrowserProcess(browser, tempDir) }
            .getOrElse {
                tempDir.deleteRecursively()
                return@withContext BrowserAuthResult.Failed(it)
            }

        try {
            pollForSessionCookies(tempDir, browser) ?: run {
                if (process.isAlive) return@withContext BrowserAuthResult.TimedOut
                BrowserAuthResult.NoBrowserFound
            }
        } finally {
            runCatching { if (process.isAlive) process.destroy() }
            runCatching { tempDir.deleteRecursively() }
        }
    }

    private fun startBrowserProcess(browser: BrowserCandidate, tempDir: File): Process =
        when (browser.engine) {
            BrowserEngine.GECKO -> ProcessBuilder(
                browser.path, "-no-remote", "-profile", tempDir.absolutePath, YOUTUBE_LOGIN_URL,
            ).start()

            BrowserEngine.CHROMIUM -> ProcessBuilder(
                browser.path,
                "--app=$YOUTUBE_LOGIN_URL",
                "--user-data-dir=${tempDir.absolutePath}",
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-features=AppBoundEncryptionProvider",
                "--password-store=basic",
            ).start()
        }

    /** Polls the profile directory's cookie store, re-reading only when the file changes. */
    private suspend fun pollForSessionCookies(
        tempDir: File,
        browser: BrowserCandidate
    ): BrowserAuthResult? {
        var lastReadMTime = -1L
        return withTimeoutOrNull(LAUNCH_TIMEOUT) {
            while (isActive) {
                val cookieDb = cookieDbFile(tempDir, browser.engine)
                if (cookieDb != null && cookieDb.length() > 0L) {
                    val walFile = File(cookieDb.path + "-wal")
                    val currentMTime = maxOf(
                        cookieDb.lastModified(),
                        walFile.takeIf { it.exists() }?.lastModified() ?: 0L
                    )
                    if (currentMTime != lastReadMTime) {
                        lastReadMTime = currentMTime
                        val cookies = readCookies(cookieDb, browser)
                        if (cookies != null && cookies.hasSapisid()) {
                            return@withTimeoutOrNull BrowserAuthResult.Success(
                                cookies.toHeaderStringOrNull() ?: continue
                            )
                        }
                    }
                }
                delay(POLL_INTERVAL)
            }
            null
        }
    }

    private fun cookieDbFile(profileDir: File, engine: BrowserEngine): File? = when (engine) {
        BrowserEngine.GECKO -> File(profileDir, "cookies.sqlite").takeIf { it.exists() }
        BrowserEngine.CHROMIUM -> listOf(
            File(profileDir, "Default/Network/Cookies"),
            File(profileDir, "Default/Cookies"),
        ).firstOrNull { it.exists() }
    }

    /**
     * Searches the user's existing Firefox family, then Chromium-family, profiles for a
     * YouTube/Google session, returning the first one found with a `SAPISID` cookie.
     */
    suspend fun importExistingBrowserCookies(): BrowserAuthResult = withContext(Dispatchers.IO) {
        val home = System.getProperty("user.home")
            ?: return@withContext BrowserAuthResult.Failed(IllegalStateException("user.home not set"))

        findGeckoSession(home)?.let { return@withContext BrowserAuthResult.Success(it) }
        findChromiumSession(home)?.let { return@withContext BrowserAuthResult.Success(it) }
        BrowserAuthResult.NoBrowserFound
    }

    private fun geckoProfileRoots(home: String): List<File> = when (HostOs.current) {
        HostOs.WINDOWS -> {
            val appData = System.getenv("APPDATA") ?: ""
            listOf(
                File(appData, "Mozilla\\Firefox\\Profiles"),
                File(appData, "LibreWolf\\Profiles"),
                File(appData, "Floorp\\Profiles"),
                File(appData, "zen\\Profiles"),
            )
        }

        HostOs.MACOS -> listOf(
            File(home, "Library/Application Support/Firefox/Profiles"),
            File(home, "Library/Application Support/LibreWolf/Profiles"),
            File(home, "Library/Application Support/Floorp/Profiles"),
            File(home, "Library/Application Support/zen/Profiles"),
        )

        HostOs.LINUX -> listOf(
            File(home, ".mozilla/firefox"),
            File(home, ".var/app/org.mozilla.firefox/.mozilla/firefox"),
            File(home, "snap/firefox/common/.mozilla/firefox"),
            File(home, ".librewolf"),
            File(home, ".floorp"),
            File(home, ".zen"),
        )
    }

    private fun chromiumProfileRoots(home: String): List<File> = when (HostOs.current) {
        HostOs.WINDOWS -> {
            val localAppData = System.getenv("LOCALAPPDATA") ?: ""
            listOf(
                File(localAppData, "Google\\Chrome\\User Data"),
                File(localAppData, "BraveSoftware\\Brave-Browser\\User Data"),
                File(localAppData, "Microsoft\\Edge\\User Data"),
                File(localAppData, "Vivaldi\\User Data"),
            )
        }

        HostOs.MACOS -> listOf(
            File(home, "Library/Application Support/Google/Chrome"),
            File(home, "Library/Application Support/BraveSoftware/Brave-Browser"),
            File(home, "Library/Application Support/Microsoft Edge"),
            File(home, "Library/Application Support/Vivaldi"),
            File(home, "Library/Application Support/Arc/User Data"),
        )

        HostOs.LINUX -> listOf(
            File(home, ".config/google-chrome"),
            File(home, ".config/chromium"),
            File(home, ".config/BraveSoftware/Brave-Browser"),
            File(home, ".config/microsoft-edge"),
            File(home, ".config/vivaldi"),
            File(home, ".config/opera"),
            File(home, ".var/app/com.google.Chrome/config/google-chrome"),
            File(home, ".var/app/com.brave.Browser/config/BraveSoftware/Brave-Browser"),
        )
    }

    private fun findGeckoSession(home: String): String? {
        for (root in geckoProfileRoots(home)) {
            if (!root.isDirectory) continue
            val profiles = root.listFiles { f -> f.isDirectory } ?: continue
            for (profile in profiles) {
                val cookieDb = File(profile, "cookies.sqlite")
                if (!cookieDb.exists() || cookieDb.length() == 0L) continue
                val cookies = withTempCopy(cookieDb) { readFirefoxCookies(it) }
                if (cookies?.hasSapisid() == true) return cookies.toHeaderStringOrNull()
            }
        }
        return null
    }

    private fun findChromiumSession(home: String): String? {
        for (root in chromiumProfileRoots(home)) {
            if (!root.isDirectory) continue
            val profileDirs = listOf(File(root, "Default")) +
                    (root.listFiles { f -> f.isDirectory && f.name.startsWith("Profile") }
                        ?: emptyArray())

            for (profileDir in profileDirs) {
                val cookieDb =
                    listOf(File(profileDir, "Network/Cookies"), File(profileDir, "Cookies"))
                        .firstOrNull { it.exists() && it.length() > 0L } ?: continue
                val keychainService = "${root.parentFile?.name ?: root.name} Safe Storage"
                val localStateFile = findLocalState(cookieDb)
                val cookies = withTempCopy(cookieDb) { tempDb ->
                    readChromiumCookies(tempDb, keychainService, localStateFile)
                }
                if (cookies?.hasSapisid() == true) return cookies.toHeaderStringOrNull()
            }
        }
        return null
    }

    private fun findLocalState(cookieDb: File): File? {
        var curr: File? = cookieDb.parentFile
        while (curr != null) {
            val candidate = File(curr, "Local State")
            if (candidate.exists() && candidate.isFile) {
                return candidate
            }
            curr = curr.parentFile
        }
        return null
    }

    private fun readCookies(cookieDb: File, browser: BrowserCandidate): Map<String, String>? =
        withTempCopy(cookieDb) { tempDb ->
            val localStateFile = findLocalState(cookieDb)
            when (browser.engine) {
                BrowserEngine.GECKO -> readFirefoxCookies(tempDb)
                BrowserEngine.CHROMIUM -> readChromiumCookies(tempDb, browser.macKeychainService, localStateFile)
            }
        }

    /** Copies a (possibly locked) sqlite DB to a scratch file so it can be safely opened. */
    private fun <T> withTempCopy(dbFile: File, block: (File) -> T): T? {
        val tempCopy = File.createTempFile("melo_cookie_read_", ".sqlite")
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        val tempWal = File(tempCopy.path + "-wal")
        val tempShm = File(tempCopy.path + "-shm")

        return try {
            dbFile.copyTo(tempCopy, overwrite = true)
            if (walFile.exists()) {
                runCatching { walFile.copyTo(tempWal, overwrite = true) }
            }
            if (shmFile.exists()) {
                runCatching { shmFile.copyTo(tempShm, overwrite = true) }
            }
            block(tempCopy)
        } catch (_: Exception) {
            null
        } finally {
            tempCopy.delete()
            tempWal.delete()
            tempShm.delete()
        }
    }

    private fun readFirefoxCookies(dbFile: File): Map<String, String>? = runCatching {
        queryCookies(dbFile, FIREFOX_COOKIE_QUERY) { rs ->
            rs.getString("name") to rs.getString("value")
        }
    }.getOrNull()

    private fun readChromiumCookies(
        dbFile: File,
        macKeychainService: String,
        localStateFile: File? = null,
    ): Map<String, String>? = runCatching {
        queryCookies(dbFile, CHROMIUM_COOKIE_QUERY) { rs ->
            val name = rs.getString("name")
            val plainValue = rs.getString("value")
            val value = if (!plainValue.isNullOrBlank()) {
                plainValue
            } else {
                rs.getBytes("encrypted_value")
                    ?.let { CookieCrypto.decryptChromiumCookie(it, macKeychainService, localStateFile) }
            }
            name to value
        }
    }.getOrNull()

    private inline fun queryCookies(
        dbFile: File,
        query: String,
        extract: (java.sql.ResultSet) -> Pair<String?, String?>,
    ): Map<String, String> {
        val cookies = LinkedHashMap<String, String>()
        openConnection(dbFile).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        val (name, value) = extract(rs)
                        if (!name.isNullOrBlank() && !value.isNullOrBlank()) cookies[name] = value
                    }
                }
            }
        }
        return cookies
    }

    private fun openConnection(dbFile: File): Connection =
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
}