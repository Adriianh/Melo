package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.data.remote.piped.PipedApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * AudioProvider that combines two backends for best results:
 *
 *  - **Piped** (`music_songs` filter) handles *search*: it queries YouTube Music
 *    specifically, so results are always official audio tracks — no music videos,
 *    covers or parodies slipping through.
 *
 *  - **yt-dlp** handles *stream resolution*: given the video ID returned by Piped
 *    it resolves the direct audio URL locally in ~2-4 s, without relying on any
 *    Piped instance staying alive.
 *
 * On first use, [YtDlpBootstrap] ensures yt-dlp is available, downloading it to
 * `~/.config/melo/yt-dlp` automatically if it is not on the system PATH.
 */
class YtDlpAudioProvider(
    private val pipedApiClient: PipedApiClient
) : AudioProvider {

    private val ytDlpBin: String by lazy { YtDlpBootstrap.resolve() }
    private val streamUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val cacheExpiryMs = 6 * 60 * 60 * 1000L

    /**
     * Uses Piped's `music_songs` filter to find the correct YouTube Music video ID
     * for the given track. Returns the ID (e.g. `"4NRXx6U8ABQ"`) or null on failure.
     */
    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? =
        pipedApiClient.search(title, title, artist, durationMs)

    /**
     * Resolves a direct audio-stream URL for the given YouTube video ID using a
     * single yt-dlp subprocess (~2-4 s). Returns null on failure.
     */
    override suspend fun getStreamUrl(sourceId: String): String? {
        streamUrlCache[sourceId]?.let { (url, timestamp) ->
            if (System.currentTimeMillis() - timestamp < cacheExpiryMs) {
                return url
            }
            streamUrlCache.remove(sourceId)
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.youtube.com/watch?v=$sourceId"
                val streamUrl = runYtDlp(
                    "--quiet",
                    "--no-warnings",
                    "--no-playlist",
                    "--skip-download",
                    "--no-check-certificates",
                    "--ignore-config",
                    "--js-runtime", "node",
                    "--extractor-args", "youtube:player-client=ios,web,android",
                    "-f", "bestaudio/best",
                    "--get-url",
                    url
                ).lines().firstOrNull { it.startsWith("http") }

                streamUrl?.also {
                    streamUrlCache[sourceId] = Pair(it, System.currentTimeMillis())
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Downloads the audio for the given YouTube video ID using yt-dlp with the specified
     * format (e.g. "mp3", "flac") to the given destination directory.
     * Returns the absolute path of the downloaded file if successful, or null otherwise.
     */
    override suspend fun downloadAudio(
        source: String,
        destination: String,
        format: String,
        quality: String,
        embedMetadata: Boolean
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://www.youtube.com/watch?v=$source"

                val dir = File(destination)
                val beforeFiles = dir.listFiles()?.associate { it.name to it.lastModified() } ?: emptyMap()
                val resolvedQuality = when (format) {
                    "flac" -> "0"
                    "opus" -> quality.replace("k", "").toIntOrNull()
                        ?.coerceAtMost(192).let { "${it}k" }

                    else -> quality
                }
                val formatArg = when (format) {
                    "opus" -> "bestaudio[ext=webm]/bestaudio/best"
                    else -> "bestaudio/best"
                }

                val args = mutableListOf(
                    "-q",
                    "--no-warnings",
                    "--no-check-certificates",
                    "--ignore-config",
                    "--js-runtime", "node",
                    "--extractor-args", "youtube:player-client=ios,web,android",
                    "-x",
                    "-f", formatArg,
                    "--audio-format", format,
                    "--audio-quality", resolvedQuality
                )
                if (embedMetadata) {
                    args.add("--embed-thumbnail")
                    args.add("--add-metadata")
                }
                args.addAll(
                    listOf(
                        "-o", "$destination/%(id)s - %(uploader)s - %(title)s.%(ext)s",
                        url
                    )
                )

                runYtDlp(*args.toTypedArray())

                val afterFiles = dir.listFiles()?.associate { it.name to it.lastModified() } ?: emptyMap()
                val modifiedOrNewFiles = afterFiles.filter { (name, lastMod) -> beforeFiles[name] != lastMod }.keys

                // Filter for audio files only (ignore thumbnails like .webp, .jpg, etc.)
                val audioExtensions = setOf("mp3", "flac", "m4a", "opus", "ogg", "wav", "aac")
                val downloaded = modifiedOrNewFiles.firstOrNull { name ->
                    name.startsWith("$source ") && audioExtensions.any { ext ->
                        name.endsWith(
                            ".$ext",
                            ignoreCase = true
                        )
                    }
                }?.let { File(dir, it) }

                modifiedOrNewFiles.forEach { fileName ->
                    val file = File(dir, fileName)
                    if (file.absolutePath != downloaded?.absolutePath) {
                        val isMetadata = setOf("webp", "png", "jpg", "jpeg", "json", "temp", "part").any { ext ->
                            fileName.endsWith(".$ext", ignoreCase = true)
                        }
                        if (isMetadata) file.delete()
                    }
                }

                if (downloaded != null && downloaded.exists()) downloaded.absolutePath else null
            } catch (_: Exception) {
                null
            }
        }

    private fun runYtDlp(vararg args: String): String {
        val process = ProcessBuilder(listOf(ytDlpBin) + args.toList())
            .redirectErrorStream(false)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }
}