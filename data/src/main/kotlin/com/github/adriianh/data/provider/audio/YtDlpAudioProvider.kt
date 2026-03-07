package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.data.remote.piped.PipedApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    override suspend fun getStreamUrl(sourceId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://www.youtube.com/watch?v=$sourceId"
                runYtDlp(
                    "--quiet",
                    "--no-warnings",
                    "--no-playlist",
                    "--skip-download",
                    "-f", "bestaudio",
                    "--get-url",
                    url
                ).lines().firstOrNull { it.startsWith("http") }
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
