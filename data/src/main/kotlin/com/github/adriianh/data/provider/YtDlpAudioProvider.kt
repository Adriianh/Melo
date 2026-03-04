package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.provider.AudioProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AudioProvider backed by yt-dlp running as a local subprocess.
 * Requires yt-dlp to be installed on the system (https://github.com/yt-dlp/yt-dlp).
 *
 * getSourceId  → returns the YouTube video ID found via yt-dlp's ytsearch
 * getStreamUrl → returns the best direct audio stream URL for a given video ID
 */
class YtDlpAudioProvider : AudioProvider {

    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? =
        withContext(Dispatchers.IO) {
            try {
                val cleanTitle = title
                    .replace(Regex("\\s*\\(.*?\\)"), "")
                    .replace(Regex("\\s*\\[.*?]"), "")
                    .trim()
                val query = "ytsearch1:$cleanTitle $artist"

                val result = runYtDlp(
                    "--no-playlist",
                    "--get-id",
                    query
                )
                result.trim().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun getStreamUrl(sourceId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://www.youtube.com/watch?v=$sourceId"
                val result = runYtDlp(
                    "--no-playlist",
                    "-f", "bestaudio",
                    "--get-url",
                    url
                )
                result.trim().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }

    private fun runYtDlp(vararg args: String): String {
        val command = mutableListOf(ytDlpBinary()) + args.toList()
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }

    private fun ytDlpBinary(): String {
        val candidates = listOf("yt-dlp", "/usr/local/bin/yt-dlp", "/usr/bin/yt-dlp")
        for (bin in candidates) {
            try {
                val probe = ProcessBuilder(bin, "--version")
                    .redirectErrorStream(true)
                    .start()
                probe.waitFor()
                if (probe.exitValue() == 0) return bin
            } catch (_: Exception) {
                continue
            }
        }
        return "yt-dlp"
    }
}

