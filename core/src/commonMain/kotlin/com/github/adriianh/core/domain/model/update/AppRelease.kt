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

@Serializable
enum class UpdateChannel(
    val displayName: String,
) {
    STABLE("Estable"),
    NIGHTLY("Nightly"),
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
    val channel: UpdateChannel = UpdateChannel.STABLE,
) {
    fun getAssetForPlatform(
        platform: UpdatePlatform,
        method: UpdateInstallMethod = UpdateInstallMethod.UNKNOWN,
    ): ReleaseAsset? {
        val candidates =
            assets.filter { asset ->
                asset.platform == platform &&
                    assetPlatformPriority(asset.name.lowercase(), platform, method) < NO_MATCH_PRIORITY
            }
        return candidates.minByOrNull { asset ->
            assetPlatformPriority(asset.name.lowercase(), platform, method)
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
        method: UpdateInstallMethod,
    ): Int =
        when (platform) {
            UpdatePlatform.WINDOWS -> windowsPriority(name)
            UpdatePlatform.LINUX -> linuxPriority(name, method)
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

    private fun linuxPriority(
        name: String,
        method: UpdateInstallMethod,
    ): Int =
        when (method) {
            UpdateInstallMethod.APPIMAGE -> if (name.endsWith(".appimage")) 0 else NO_MATCH_PRIORITY

            UpdateInstallMethod.TARBALL ->
                // Only the desktop archive carries an architecture suffix. The TUI archive
                // (`melo-<version>-linux.tar.gz`) lives in the same release and must never
                // be selected here, so anything without a suffix is rejected.
                if (name.endsWith(".tar.gz") && (name.contains("x64") || name.contains("x86_64"))) {
                    0
                } else {
                    NO_MATCH_PRIORITY
                }

            else ->
                when {
                    name.endsWith(".appimage") -> 0
                    name.endsWith(".deb") -> 1
                    name.endsWith(".rpm") -> 2
                    name.endsWith(".tar.gz") -> 3
                    else -> 4
                }
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

/** Priority that excludes an asset from the selection for the current install method. */
private const val NO_MATCH_PRIORITY = 10
