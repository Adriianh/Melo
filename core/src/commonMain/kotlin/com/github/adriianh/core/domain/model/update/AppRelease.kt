package com.github.adriianh.core.domain.model.update

import com.github.adriianh.core.platform.currentUpdatePlatform
import kotlinx.serialization.Serializable

@Serializable
enum class UpdatePlatform {
    WINDOWS,
    LINUX,
    MACOS,
    ANDROID,
    UNKNOWN,
}

fun currentPlatform(): UpdatePlatform = currentUpdatePlatform()

@Serializable
data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val platform: UpdatePlatform,
)

@Serializable
data class AppRelease(
    val version: String,
    val tagName: String,
    val name: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val publishedAt: String,
    val assets: List<ReleaseAsset>,
) {
    fun getAssetForPlatform(platform: UpdatePlatform): ReleaseAsset? {
        val matching = assets.filter { it.platform == platform }
        if (matching.isEmpty()) return null

        return matching.minByOrNull { asset ->
            assetPlatformPriority(asset.name.lowercase(), platform)
        }
    }

    fun getTuiAssetForPlatform(platform: UpdatePlatform): ReleaseAsset? {
        val candidates =
            assets.filter { asset ->
                tuiAssetPlatformPriority(asset.name.lowercase(), platform) < 10
            }
        return candidates.minByOrNull { asset ->
            tuiAssetPlatformPriority(asset.name.lowercase(), platform)
        }
    }

    private fun tuiAssetPlatformPriority(
        name: String,
        platform: UpdatePlatform,
    ): Int =
        when (platform) {
            UpdatePlatform.WINDOWS -> windowsTuiPriority(name)
            UpdatePlatform.LINUX -> linuxTuiPriority(name)
            UpdatePlatform.MACOS -> macosTuiPriority(name)
            UpdatePlatform.ANDROID, UpdatePlatform.UNKNOWN -> 10
        }

    private fun windowsTuiPriority(name: String): Int =
        when {
            name.endsWith("-windows.zip") -> 0
            name.endsWith(".zip") && name.contains("windows") -> 1
            name.endsWith(".zip") -> 2
            else -> 10
        }

    private fun linuxTuiPriority(name: String): Int =
        when {
            name.endsWith("-linux.tar.gz") -> 0
            name.endsWith(".tar.gz") &&
                name.contains("linux") &&
                !name.contains("x86_64") &&
                !name.contains("x64") -> 1
            name.endsWith(".tar.gz") && name.contains("linux") -> 2
            name.endsWith(".tar.gz") -> 3
            else -> 10
        }

    private fun macosTuiPriority(name: String): Int =
        when {
            name.endsWith("-macos.tar.gz") -> 0
            name.endsWith(".tar.gz") &&
                (name.contains("macos") || name.contains("darwin") || name.contains("mac")) -> 1
            name.endsWith(".tar.gz") -> 2
            name.endsWith(".zip") && (name.contains("macos") || name.contains("mac")) -> 3
            else -> 10
        }

    private fun assetPlatformPriority(
        name: String,
        platform: UpdatePlatform,
    ): Int =
        when (platform) {
            UpdatePlatform.WINDOWS -> windowsPriority(name)
            UpdatePlatform.LINUX -> linuxPriority(name)
            UpdatePlatform.MACOS -> macosPriority(name)
            UpdatePlatform.ANDROID -> if (name.endsWith(".apk")) 0 else 1
            UpdatePlatform.UNKNOWN -> 0
        }

    private fun windowsPriority(name: String): Int =
        when {
            name.contains("setup") && name.endsWith(".exe") -> 0
            name.endsWith(".exe") -> 1
            name.endsWith(".msi") -> 2
            name.endsWith(".zip") -> 3
            else -> 4
        }

    private fun linuxPriority(name: String): Int =
        when {
            name.endsWith(".appimage") -> 0
            name.endsWith(".deb") -> 1
            name.endsWith(".rpm") -> 2
            name.endsWith(".tar.gz") -> 3
            else -> 4
        }

    private fun macosPriority(name: String): Int =
        when {
            name.endsWith(".dmg") -> 0
            name.endsWith(".pkg") -> 1
            name.endsWith(".zip") -> 2
            name.endsWith(".tar.gz") -> 3
            else -> 4
        }
}
