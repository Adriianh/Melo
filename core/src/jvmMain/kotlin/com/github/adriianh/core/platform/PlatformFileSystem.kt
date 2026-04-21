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
}
