package com.github.adriianh.core.platform

import java.io.File
import java.io.IOException

object MeloDataDirectory {
    /**
     * Resolves the root Melo user data directory for the current JVM platform.
     *
     * - Windows: `%APPDATA%\Melo` (with fallback to `~/.melo` if APPDATA is unset).
     *   Automatically triggers transparent one-time migration from `~/.melo` if needed.
     * - Linux / macOS: `~/.melo`.
     */
    fun resolve(): File {
        val os = System.getProperty("os.name", "").lowercase()
        return if (os.contains("win")) {
            resolveWindowsDirectory(
                appData = System.getenv("APPDATA"),
                userHome = System.getProperty("user.home"),
            )
        } else {
            val dir = File(System.getProperty("user.home"), ".melo")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        }
    }

    internal fun resolveWindowsDirectory(
        appData: String?,
        userHome: String?,
    ): File {
        val targetDir =
            if (!appData.isNullOrBlank()) {
                File(appData, "Melo")
            } else {
                File(userHome ?: ".", ".melo")
            }
        if (!userHome.isNullOrBlank()) {
            migrateWindowsLegacyDirIfNeeded(
                legacyDir = File(userHome, ".melo"),
                targetDir = targetDir,
            )
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }

    /**
     * Migrates user data from [legacyDir] (`%USERPROFILE%\.melo`) to [targetDir] (`%APPDATA%\Melo`).
     */
    internal fun migrateWindowsLegacyDirIfNeeded(
        legacyDir: File,
        targetDir: File,
    ) {
        if (!legacyDir.exists() || !legacyDir.isDirectory || isSameDirectory(legacyDir, targetDir)) {
            return
        }

        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetContents = targetDir.list()
            if (targetContents.isNullOrEmpty()) {
                legacyDir.copyRecursively(
                    target = targetDir,
                    overwrite = false,
                    onError = { _, _ -> OnErrorAction.SKIP },
                )
            } else {
                copyMissingKeyFiles(legacyDir, targetDir)
                copyMissingSubdirectories(legacyDir, targetDir)
            }
        } catch (_: Exception) {
            // Guard against unexpected filesystem errors to keep app startup resilient
        }
    }

    private fun isSameDirectory(
        a: File,
        b: File,
    ): Boolean =
        try {
            a.canonicalFile == b.canonicalFile
        } catch (_: IOException) {
            a.absoluteFile == b.absoluteFile
        }

    private fun copyMissingKeyFiles(
        sourceDir: File,
        targetDir: File,
    ) {
        val keyFiles =
            listOf(
                "melo.db",
                "melo.db-wal",
                "melo.db-shm",
                "settings.json",
                "library_cache.json",
                "home_feed_cache.json",
                "entity_cache.json",
                "sts.txt",
            )
        for (fileName in keyFiles) {
            val src = File(sourceDir, fileName)
            val dst = File(targetDir, fileName)
            if (src.exists() && !dst.exists()) {
                src.copyTo(dst, overwrite = false)
            }
        }
    }

    private fun copyMissingSubdirectories(
        sourceDir: File,
        targetDir: File,
    ) {
        val subdirs = listOf("cache", "downloads")
        for (subdirName in subdirs) {
            val src = File(sourceDir, subdirName)
            val dst = File(targetDir, subdirName)
            if (src.exists() && src.isDirectory && !dst.exists()) {
                src.copyRecursively(
                    target = dst,
                    overwrite = false,
                    onError = { _, _ -> OnErrorAction.SKIP },
                )
            }
        }
    }
}
