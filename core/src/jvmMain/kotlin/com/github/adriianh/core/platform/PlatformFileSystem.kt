package com.github.adriianh.core.platform

import java.io.File

actual object PlatformFileSystem {
    actual fun fileExists(path: String): Boolean = File(path).exists()

    actual fun toFileUri(path: String): String = "file://${File(path).absolutePath}"
}
