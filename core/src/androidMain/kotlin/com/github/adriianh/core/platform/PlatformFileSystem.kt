package com.github.adriianh.core.platform

import android.os.Environment
import java.io.File

actual object PlatformFileSystem {
    actual fun fileExists(path: String): Boolean = try {
        if (path.startsWith("content://")) true
        else File(path.removePrefix("file://")).exists()
    } catch (_: Exception) {
        false
    }

    actual fun fileSize(path: String): Long = try {
        File(path.removePrefix("file://")).takeIf { it.exists() }?.length() ?: 0L
    } catch (_: Exception) {
        0L
    }

    actual fun toFileUri(path: String): String {
        if (path.startsWith("content://") || path.startsWith("file://") || path.startsWith("http://") || path.startsWith(
                "https://"
            )
        ) {
            return path
        }
        return "file://${File(path).absolutePath}"
    }

    actual fun readText(path: String): String? = try {
        File(path).takeIf { it.exists() }?.readText()
    } catch (_: Exception) {
        null
    }

    actual fun writeText(path: String, text: String) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(text)
        } catch (_: Exception) {
        }
    }

    actual fun writeBytes(path: String, bytes: ByteArray) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        } catch (_: Exception) {
        }
    }

    actual fun readBytes(path: String): ByteArray? = try {
        File(path.removePrefix("file://")).takeIf { it.exists() }?.readBytes()
    } catch (_: Exception) {
        null
    }

    actual fun copyFile(sourcePath: String, destPath: String): Boolean = try {
        val src = File(sourcePath.removePrefix("file://"))
        val dst = File(destPath.removePrefix("file://"))
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
    } catch (_: Exception) {
        false
    }

    actual fun makeDirs(path: String): Boolean = try {
        File(path).mkdirs()
    } catch (_: Exception) {
        false
    }

    actual fun getDefaultMusicPaths(): List<String> {
        val paths = mutableListOf<String>()
        try {
            val musicDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (musicDir != null) paths.add(musicDir.absolutePath)
        } catch (_: Exception) {
        }

        try {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) paths.add(downloadsDir.absolutePath)
        } catch (_: Exception) {
        }

        listOf("/storage/emulated/0/Music", "/storage/emulated/0/Download").forEach { path ->
            if (path !in paths) paths.add(path)
        }

        return paths.distinct()
    }

    actual fun getTempDirectory(): String = try {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
            ?: System.getProperty("java.io.tmpdir")
            ?: Environment.getExternalStorageDirectory().path
    } catch (_: Exception) {
        Environment.getExternalStorageDirectory().path
    }
}