package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.utils.parseCookieString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
 *
 * The session cookies are passed to every resolution so login-gated and
 * age-restricted tracks behave like they do on Android (where the embedded
 * yt-dlp always receives the cookie file). yt-dlp also needs a JavaScript
 * runtime to solve YouTube's JS challenges; [QuickJsBootstrap] provides one on
 * Desktop, matching the QuickJS runtime bundled on Android. Stream resolutions
 * are serialized and bounded by a timeout so concurrent or hung processes
 * cannot stall playback or leave a stray yt-dlp behind.
 */
class YtDlpAudioProvider(
    private val pipedApiClient: PipedApiClient,
    private val fallback: AudioProvider? = null,
) : AudioProvider {
    private companion object {
        const val STREAM_TIMEOUT_MS = 45_000L
        const val AGE_GATE_TIMEOUT_MS = 150_000L
        const val DOWNLOAD_TIMEOUT_MS = 30 * 60_000L
    }

    private val ytDlpBin: String by lazy { YtDlpBootstrap.resolve() }
    private val quickJsPath: String? by lazy { QuickJsBootstrap.resolve() }
    private val streamUrlCache = ConcurrentHashMap<String, String>()
    private val sourceIdCache = ConcurrentHashMap<String, String>()
    private val processMutex = Mutex()

    /**
     * Extracts the exact expiration time from a YouTube stream URL's "expire=" query parameter.
     * Returns null if not found.
     */
    private fun getUrlExpiryTimeMs(url: String): Long? {
        // e.g. ...&expire=1711099234&...
        val regex = Regex("""[?&]expire=(\d+)""")
        val match = regex.find(url) ?: return null
        return match.groupValues[1].toLongOrNull()?.times(1000L)
    }

    /**
     * Uses Piped's `music_songs` filter to find the correct YouTube Music video ID
     * for the given track. Returns the ID (e.g. `"4NRXx6U8ABQ"`) or null on failure.
     */
    override suspend fun getSourceId(
        artist: String,
        title: String,
        durationMs: Long,
    ): String? {
        val cacheKey = "$artist|$title|$durationMs"
        sourceIdCache[cacheKey]?.let { return it }

        val id = pipedApiClient.search(title, title, artist, durationMs)
        if (id != null) {
            sourceIdCache[cacheKey] = id
        }

        return id
    }

    /**
     * Resolves a direct audio-stream URL for the given YouTube video ID using a
     * single yt-dlp subprocess (~2-4 s). Returns null on failure.
     */
    override suspend fun getStreamUrl(sourceId: String): String? {
        streamUrlCache[sourceId]?.let { url ->
            val expiryTimeMs = getUrlExpiryTimeMs(url)
            if (expiryTimeMs == null || System.currentTimeMillis() < (expiryTimeMs - 5 * 60 * 1000L)) {
                return url
            }
            streamUrlCache.remove(sourceId)
        }

        return withContext(Dispatchers.IO) {
            val streamUrl = resolveStreamUrl(sourceId, ageRestricted = false)
            streamUrl?.also {
                streamUrlCache[sourceId] = it
            } ?: fallback?.getStreamUrl(sourceId)
        }
    }

    /**
     * Resolves an age-restricted stream using the logged-in session: copies the
     * current cookies to a Netscape file and enables yt-dlp's EJS challenge
     * solver through the QuickJS runtime. Both pieces are required: the JS
     * runtime clears the BotGuard challenge and the cookies prove the account
     * is old enough to watch the video.
     */
    override suspend fun getAgeRestrictedStreamUrl(sourceId: String): String? =
        withContext(Dispatchers.IO) {
            if (YouTube.cookie == null) return@withContext null
            resolveStreamUrl(sourceId, ageRestricted = true)
        }

    private suspend fun resolveStreamUrl(
        sourceId: String,
        ageRestricted: Boolean,
    ): String? {
        val cookieFile = YouTube.cookie?.let { writeNetscapeCookies(it) }
        return try {
            val args =
                mutableListOf(
                    "--quiet",
                    "--no-warnings",
                    "--no-playlist",
                    "--skip-download",
                    "--no-check-certificates",
                    "--ignore-config",
                )
            if (cookieFile != null) {
                args += listOf("--cookies", cookieFile.absolutePath)
            }
            if (ageRestricted) {
                // Age-gated videos need yt-dlp's EJS challenge solver, which only
                // works with a JavaScript runtime. Enabling it for regular tracks
                // is not needed and makes resolutions significantly slower.
                quickJsPath?.let { args += listOf("--js-runtimes", "quickjs:$it") }
            } else {
                args += listOf("--extractor-args", "youtube:player-client=ios,web,android")
            }
            args +=
                listOf(
                    "-f",
                    "bestaudio/best",
                    "--get-url",
                    "https://www.youtube.com/watch?v=$sourceId",
                )

            val timeoutMs = if (ageRestricted) AGE_GATE_TIMEOUT_MS else STREAM_TIMEOUT_MS
            val output = processMutex.withLock { runYtDlp(timeoutMs, args) }
            output?.lines()?.firstOrNull { it.startsWith("http") }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } finally {
            cookieFile?.delete()
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
        embedMetadata: Boolean,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://www.youtube.com/watch?v=$source"

                val dir = File(destination)
                val beforeFiles =
                    dir.listFiles()?.associate { it.name to it.lastModified() } ?: emptyMap()
                val resolvedQuality =
                    when (format) {
                        "flac" -> "0"
                        "opus" ->
                            quality
                                .replace("k", "")
                                .toIntOrNull()
                                ?.coerceAtMost(192)
                                .let { "${it}k" }

                        else -> quality
                    }
                val formatArg =
                    when (format) {
                        "opus" -> "bestaudio[ext=webm]/bestaudio/best"
                        else -> "bestaudio/best"
                    }

                val args =
                    mutableListOf(
                        "-q",
                        "--no-warnings",
                        "--no-check-certificates",
                        "--ignore-config",
                        "--extractor-args",
                        "youtube:player-client=ios,web,android",
                        "-x",
                        "-f",
                        formatArg,
                        "--audio-format",
                        format,
                        "--audio-quality",
                        resolvedQuality,
                    )
                if (embedMetadata) {
                    args.add("--embed-thumbnail")
                    args.add("--add-metadata")
                }
                args.addAll(
                    listOf(
                        "-o",
                        "$destination/%(id)s - %(uploader)s - %(title)s.%(ext)s",
                        url,
                    ),
                )

                val cookieFile = YouTube.cookie?.let { writeNetscapeCookies(it) }
                try {
                    if (cookieFile != null) {
                        args.addAll(0, listOf("--cookies", cookieFile.absolutePath))
                    }
                    quickJsPath?.let { args.addAll(0, listOf("--js-runtimes", "quickjs:$it")) }
                    runYtDlp(DOWNLOAD_TIMEOUT_MS, args)
                } finally {
                    cookieFile?.delete()
                }

                val afterFiles =
                    dir.listFiles()?.associate { it.name to it.lastModified() } ?: emptyMap()
                val modifiedOrNewFiles =
                    afterFiles.filter { (name, lastMod) -> beforeFiles[name] != lastMod }.keys

                // Filter for audio files only (ignore thumbnails like .webp, .jpg, etc.)
                val audioExtensions = setOf("mp3", "flac", "m4a", "opus", "ogg", "wav", "aac")
                val downloaded =
                    modifiedOrNewFiles
                        .firstOrNull { name ->
                            name.startsWith("$source ") &&
                                audioExtensions.any { ext ->
                                    name.endsWith(
                                        ".$ext",
                                        ignoreCase = true,
                                    )
                                }
                        }?.let { File(dir, it) }

                modifiedOrNewFiles.forEach { fileName ->
                    val file = File(dir, fileName)
                    if (file.absolutePath != downloaded?.absolutePath) {
                        val isMetadata =
                            setOf("webp", "png", "jpg", "jpeg", "json", "temp", "part").any { ext ->
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

    /**
     * Builds a Netscape-format cookie file from the current session so a yt-dlp
     * subprocess can authenticate as the logged-in user.
     */
    private fun writeNetscapeCookies(cookie: String): File? =
        try {
            val file = File.createTempFile("melo_ytdlp_cookies", ".txt")
            file.deleteOnExit()
            file.writeText(
                buildString {
                    appendLine("# Netscape HTTP Cookie File")
                    for ((name, value) in parseCookieString(cookie)) {
                        appendLine(".youtube.com\tTRUE\t/\tFALSE\t0\t$name\t$value")
                    }
                },
                StandardCharsets.UTF_8,
            )
            file
        } catch (_: Exception) {
            null
        }

    /**
     * Runs yt-dlp and returns its combined output, or null when the process does
     * not finish within [timeoutMs]. The process is killed on timeout so a stuck
     * network request cannot block playback indefinitely.
     *
     * Older yt-dlp releases do not know `--js-runtimes`; in that case the command
     * is retried without it so playback and downloads keep working.
     */
    private fun runYtDlp(
        timeoutMs: Long,
        args: List<String>,
    ): String? {
        val output = runYtDlpOnce(timeoutMs, args) ?: return null
        return if ("--js-runtimes" in args && output.contains("no such option: --js-runtimes")) {
            runYtDlpOnce(timeoutMs, args.withoutJsRuntime())
        } else {
            output
        }
    }

    private fun List<String>.withoutJsRuntime(): List<String> {
        val index = indexOf("--js-runtimes")
        if (index < 0 || index + 1 >= size) return this
        return filterIndexed { i, _ -> i != index && i != index + 1 }
    }

    private fun runYtDlpOnce(
        timeoutMs: Long,
        args: List<String>,
    ): String? {
        val process =
            ProcessBuilder(listOf(ytDlpBin) + args)
                .redirectErrorStream(true)
                .start()
        val output = StringBuffer()
        val drain =
            Thread {
                try {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line -> output.append(line).append('\n') }
                    }
                } catch (_: Exception) {
                }
            }
        drain.isDaemon = true
        drain.start()

        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroy()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
            drain.join(1_000)
            return null
        }
        drain.join(2_000)
        return output.toString()
    }
}
