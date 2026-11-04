package com.github.adriianh.data.provider.audio

import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Ensures a QuickJS runtime is available for yt-dlp's EJS challenge solver.
 *
 * yt-dlp needs a JavaScript runtime to solve YouTube's JS challenges (nsig).
 * Without one it warns that some formats may be missing, and age-restricted
 * tracks cannot be resolved even with valid cookies. Android bundles QuickJS;
 * Desktop resolves it here using the same order as [YtDlpBootstrap].
 *
 * Resolution order:
 *   1. `qjs` / `quickjs` on the system PATH
 *   2. ~/.config/melo/qjs  (previously self-downloaded)
 *   3. Download the pinned quickjs-ng release for the current platform
 */
object QuickJsBootstrap {
    private val log = Logger.getLogger("Melo.QuickJsBootstrap")

    /** Pinned quickjs-ng release so the downloaded runtime is reproducible. */
    private const val RELEASE = "v0.18.0"

    private val meloConfigDir: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Melo")
        } else {
            File(System.getProperty("user.home"), ".config/melo")
        }
    }

    private val bundledBin: File
        get() = File(meloConfigDir, if (isWindows()) "qjs.exe" else "qjs")

    @Volatile
    private var resolved: String? = null

    @Volatile
    private var resolutionAttempted = false

    /**
     * Returns the path to a working QuickJS binary, downloading it if needed.
     * Returns null when no runtime could be obtained; callers must keep working
     * without JS challenge solving.
     */
    @Synchronized
    fun resolve(): String? {
        if (resolutionAttempted) return resolved
        resolutionAttempted = true
        resolved = systemBinary() ?: bundledBinary() ?: download()
        return resolved
    }

    private fun systemBinary(): String? = listOf("qjs", "quickjs").firstOrNull { probe(it) }

    private fun bundledBinary(): String? =
        bundledBin
            .takeIf { it.exists() && it.canExecute() && probe(it.absolutePath) }
            ?.absolutePath

    private fun probe(bin: String): Boolean =
        try {
            val process =
                ProcessBuilder(bin, "-e", "1+1")
                    .redirectErrorStream(true)
                    .start()
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                false
            } else {
                process.exitValue() == 0
            }
        } catch (_: Exception) {
            false
        }

    private fun download(): String? {
        val assetName = assetName() ?: return null
        val downloadUrl =
            "https://github.com/quickjs-ng/quickjs/releases/download/$RELEASE/$assetName"

        log.fine("QuickJS not found — downloading from $downloadUrl …")
        return try {
            meloConfigDir.mkdirs()
            val connection = URI(downloadUrl).toURL().openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.getInputStream().use { input ->
                Files.copy(input, bundledBin.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            bundledBin.setExecutable(true)
            if (probe(bundledBin.absolutePath)) {
                bundledBin.absolutePath
            } else {
                bundledBin.delete()
                null
            }
        } catch (e: IOException) {
            log.fine("Could not download QuickJS: ${e.message}")
            null
        }
    }

    private fun assetName(): String? {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            os.contains("win") && is64Bit(arch) -> "qjs-windows-x86_64.exe"
            os.contains("win") -> "qjs-windows-x86.exe"
            os.contains("mac") && arch.contains("arm") -> "qjs-darwin-arm64"
            os.contains("mac") -> "qjs-darwin-x86_64"
            arch.contains("aarch64") || arch.contains("arm64") -> "qjs-linux-aarch64"
            arch.contains("riscv64") -> "qjs-linux-riscv64"
            arch == "arm" || arch.contains("armv7") -> "qjs-linux-armv7"
            is64Bit(arch) -> "qjs-linux-x86_64"
            else -> "qjs-linux-x86"
        }
    }

    private fun is64Bit(arch: String): Boolean = arch.contains("64")

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}
