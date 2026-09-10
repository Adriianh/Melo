package com.github.adriianh.data.provider.audio

import android.content.Context
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.utils.parseCookieString
import com.github.adriianh.ytdlpipe.QuickJs
import com.github.adriianh.ytdlpipe.YtDlpEngine
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Resolves audio URLs via yt-dlp (embedded with Chaquopy). Capable of handling
 * age-restricted content on-device: the bundled quickjs runtime lets yt-dlp
 * solve the BotGuard challenge itself ({@code js_use_po_token}) and clear the
 * age-gate using the provided session cookies.
 */
class YtDlpAudioProvider(context: Context) : AudioProvider {

    private val appContext = context.applicationContext
    private val engine = YtDlpEngine(appContext)
    private val mutex = Mutex()

    override suspend fun getSourceId(
        artist: String,
        title: String,
        durationMs: Long,
    ): String? = null

    override suspend fun getStreamUrl(sourceId: String): String? = withContext(MeloDispatchers.IO) {
        val cookie = YouTube.cookie ?: return@withContext null
        resolveWithEngine(sourceId, cookie, ageRestricted = false, quickJs = null)
    }

    override suspend fun getAgeRestrictedStreamUrl(sourceId: String): String? {
        val cookie = YouTube.cookie ?: return null
        return withContext(MeloDispatchers.IO) {
            val quickJs = QuickJs.ensure(appContext)
            if (quickJs == null) {
                return@withContext null
            }
            seedChallengeSolverCache()
            val url = resolveWithEngine(sourceId, cookie, ageRestricted = true, quickJs = quickJs)
            url
        }
    }

    private fun seedChallengeSolverCache() {
        val targetDir = File(appContext.filesDir, "ytdlp_cache/challenge-solver")
        val target = File(targetDir, "lib.json")
        if (target.isFile && target.length() > 0) {
            return
        }
        runCatching {
            if (!targetDir.isDirectory && !targetDir.mkdirs()) {
                return
            }
            appContext.assets.open("challenge-solver/lib.json").use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private suspend fun resolveWithEngine(
        sourceId: String,
        cookie: String,
        ageRestricted: Boolean,
        quickJs: File?,
    ): String? {
        val timeoutMs = if (ageRestricted) 150_000L else 45_000L
        val url = mutex.withLock {
            withTimeoutOrNull(timeoutMs.milliseconds) {
                try {
                    engine.getAudioUrl(
                        sourceId,
                        toNetscapeCookieFile(cookie),
                        ageRestricted,
                        quickJs
                    )
                } catch (_: Exception) {
                    ""
                }
            }
        }
        return url?.takeIf { it.isNotBlank() }
    }

    override suspend fun downloadAudio(
        source: String,
        destination: String,
        format: String,
        quality: String,
        embedMetadata: Boolean,
    ): String? = null

    private fun toNetscapeCookieFile(cookie: String): String = buildString {
        appendLine("# Netscape HTTP Cookie File")
        for ((name, value) in parseCookieString(cookie)) {
            appendLine(".youtube.com\tTRUE\t/\tFALSE\t0\t$name\t$value")
        }
    }
}