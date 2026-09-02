package com.github.adriianh.core.platform

import java.io.File

actual object PlatformFileSystem {
    actual fun fileExists(path: String): Boolean = File(path).exists()

    actual fun toFileUri(path: String): String = "file://${File(path).absolutePath}"

    actual fun readText(path: String): String? = try {
        File(path).takeIf { it.exists() }?.readText()
    } catch (_: Exception) { null }

    actual fun writeText(path: String, text: String) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(text)
        } catch (_: Exception) {}
    }

    actual fun writeBytes(path: String, bytes: ByteArray) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        } catch (_: Exception) {}
    }

    actual fun readBytes(path: String): ByteArray? = try {
        File(path).takeIf { it.exists() }?.readBytes()
    } catch (_: Exception) {
        null
    }

    actual fun copyFile(sourcePath: String, destPath: String): Boolean = try {
        val src = File(sourcePath)
        val dst = File(destPath)
        if (!src.exists()) false
        else {
            dst.parentFile?.mkdirs()
            src.copyTo(dst, overwrite = true)
            true
        }
    } catch (_: Exception) {
        false
    }

    actual fun deleteFile(path: String): Boolean = try {
        File(path).delete()
    } catch (_: Exception) { false }

    actual fun makeDirs(path: String): Boolean = try {
        File(path).mkdirs()
    } catch (_: Exception) { false }

    actual fun getDefaultMusicPaths(): List<String> {
        val userHome = System.getProperty("user.home") ?: return emptyList()
        val musicDir = File(userHome, "Music")
        val downloadsDir = File(userHome, "Downloads")
        return listOf(musicDir, downloadsDir)
            .filter { it.exists() && it.isDirectory }
            .map { it.absolutePath }
            .ifEmpty { listOf(musicDir.absolutePath) }
    }
}