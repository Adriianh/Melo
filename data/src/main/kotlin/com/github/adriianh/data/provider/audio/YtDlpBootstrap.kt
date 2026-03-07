package com.github.adriianh.data.provider

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Ensures yt-dlp is available on the system before the AudioProvider is used.
 *
 * Resolution order:
 *   1. System PATH (`yt-dlp` or common locations)
 *   2. ~/.config/melo/yt-dlp  (previously self-downloaded)
 *   3. Download from GitHub releases and store in ~/.config/melo/yt-dlp
 */
object YtDlpBootstrap {

    private val meloConfigDir: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "melo")
        } else {
            File(System.getProperty("user.home"), ".config/melo")
        }
    }

    private val bundledBin: File get() = File(meloConfigDir, "yt-dlp")

    /**
     * Returns the path to a working yt-dlp binary.
     * Downloads it if needed. Throws if it cannot be obtained.
     */
    fun resolve(): String {
        systemBinary()?.let { return it }

        if (bundledBin.exists() && bundledBin.canExecute()) {
            if (probe(bundledBin.absolutePath)) return bundledBin.absolutePath
        }

        return download()
    }

    /**
     * Returns true if yt-dlp is available on the system PATH (no download needed).
     */
    fun isAvailableOnSystem(): Boolean = systemBinary() != null

    private fun systemBinary(): String? {
        val candidates = listOf(
            "yt-dlp",
            "/usr/local/bin/yt-dlp",
            "/usr/bin/yt-dlp",
            "/opt/homebrew/bin/yt-dlp",
        )
        return candidates.firstOrNull { probe(it) }
    }

    private fun probe(bin: String): Boolean {
        return try {
            val p = ProcessBuilder(bin, "--version")
                .redirectErrorStream(true)
                .start()
            p.waitFor()
            p.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun download(): String {
        meloConfigDir.mkdirs()

        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val assetName = when {
            os.contains("win") -> "yt-dlp.exe"
            os.contains("mac") && arch.contains("arm") -> "yt-dlp_macos_arm"
            os.contains("mac") -> "yt-dlp_macos"
            else -> "yt-dlp"
        }

        val downloadUrl =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/$assetName"

        println("yt-dlp not found — downloading from $downloadUrl …")

        try {
            URI(downloadUrl).toURL().openStream().use { input ->
                Files.copy(input, bundledBin.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            bundledBin.setExecutable(true)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Could not download yt-dlp from $downloadUrl.\n" +
                "Please install it manually: https://github.com/yt-dlp/yt-dlp#installation",
                e
            )
        }

        if (!probe(bundledBin.absolutePath)) {
            throw IllegalStateException(
                "Downloaded yt-dlp but it does not appear to work on this system.\n" +
                "Please install it manually: https://github.com/yt-dlp/yt-dlp#installation"
            )
        }

        println("yt-dlp downloaded to ${bundledBin.absolutePath}")
        return bundledBin.absolutePath
    }
}

