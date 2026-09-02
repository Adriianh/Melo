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

    /**
     * Reads the contents of a file at the given [path] as a string.
     * Returns `null` if the file does not exist or an error occurs.
     */
    fun readText(path: String): String?

    /**
     * Writes the given [text] to a file at the given [path].
     * Creates the file and parent directories if they do not exist.
     */
    fun writeText(path: String, text: String)

    /**
     * Writes the given [bytes] to a file at the given [path].
     * Creates the file and parent directories if they do not exist.
     */
    fun writeBytes(path: String, bytes: ByteArray)

    /**
     * Deletes the file or empty directory at the given [path].
     */
    fun deleteFile(path: String): Boolean

    /**
     * Creates the directory and any necessary parent directories at [path].
     */
    fun makeDirs(path: String): Boolean

    /**
     * Returns standard system directories where audio/music files typically reside on the platform.
     */
    fun getDefaultMusicPaths(): List<String>
}
