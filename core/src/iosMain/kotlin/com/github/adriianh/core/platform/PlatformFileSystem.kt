package com.github.adriianh.core.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual object PlatformFileSystem {
    actual fun fileExists(path: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun fileSize(path: String): Long = try {
        (NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
            ?.get("NSFileSize") as? Number)?.toLong() ?: 0L
    } catch (_: Exception) {
        0L
    }

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

    actual fun readBytes(path: String): ByteArray? = null

    actual fun copyFile(sourcePath: String, destPath: String): Boolean = try {
        NSFileManager.defaultManager.copyItemAtPath(sourcePath, destPath, null)
    } catch (_: Exception) {
        false
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

    actual fun getTempDirectory(): String = NSTemporaryDirectory()
}
