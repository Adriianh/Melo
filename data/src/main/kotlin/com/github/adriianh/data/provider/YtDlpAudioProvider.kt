package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.provider.AudioProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AudioProvider backed by yt-dlp running as a local subprocess.
 * On first use, resolves the yt-dlp binary via [YtDlpBootstrap] — downloading it
 * automatically to ~/.config/melo/yt-dlp if not already present on the system.
 */
class YtDlpAudioProvider : AudioProvider {

    private val ytDlpBin: String by lazy { YtDlpBootstrap.resolve() }

    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? =
        withContext(Dispatchers.IO) {
            try {
                val cleanTitle = title
                    .replace(Regex("\\s*\\(.*?\\)"), "")
                    .replace(Regex("\\s*\\[.*?]"), "")
                    .trim()
                val query = "ytsearch1:$cleanTitle $artist"
                runYtDlp("--no-playlist", "--get-id", query).trim().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun getStreamUrl(sourceId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://www.youtube.com/watch?v=$sourceId"
                runYtDlp("--no-playlist", "-f", "bestaudio", "--get-url", url)
                    .trim().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }

    private fun runYtDlp(vararg args: String): String {
        val command = listOf(ytDlpBin) + args.toList()
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }
}
