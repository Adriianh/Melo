package com.github.adriianh.core.platform

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual object PlatformFileSystem {
    actual fun fileExists(path: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun toFileUri(path: String): String =
        "file://$path"

    actual fun readText(path: String): String? =
        NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)

    @OptIn(BetaInteropApi::class)
    actual fun writeText(path: String, text: String) {
        NSString.create(string = text).writeToFile(path, true, NSUTF8StringEncoding, null)
    }

    actual fun writeBytes(path: String, bytes: ByteArray) {
    }

    actual fun deleteFile(path: String): Boolean = try {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    } catch (_: Exception) { false }

    actual fun makeDirs(path: String): Boolean = try {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    } catch (_: Exception) { false }

    actual fun getDefaultMusicPaths(): List<String> = emptyList()
}
