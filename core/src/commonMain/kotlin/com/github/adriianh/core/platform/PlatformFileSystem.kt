package com.github.adriianh.core.platform

/**
 * Platform-agnostic file system operations needed by the core module.
 */
expect object PlatformFileSystem {
    /**
     * Returns `true` if a file exists at the given [path].
     */
    fun fileExists(path: String): Boolean

    /**
     * Converts a local file [path] to a URI string suitable for playback
     * (e.g. `file:///absolute/path`).
     */
    fun toFileUri(path: String): String
}
