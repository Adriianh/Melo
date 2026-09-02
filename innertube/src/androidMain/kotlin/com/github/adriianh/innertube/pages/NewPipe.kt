package com.github.adriianh.innertube.pages

import android.util.Log
import com.github.adriianh.innertube.ProxyConfig
import com.github.adriianh.innertube.ProxyType
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.YouTubeClient
import com.github.adriianh.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap


/**
 * Holds cache and constants for NewPipe extraction.
 */
private object CacheHolder {
    data class StreamCache(val list: List<Pair<Int, String>>, val expiryMs: Long)

    val streamCache = ConcurrentHashMap<String, StreamCache>()
    const val CACHE_TTL_MS = 2 * 60 * 1000L
    const val TAG = "NewPipeUtils"
}

/**
 * OkHttp-based Downloader for NewPipe, supporting optional proxy and authentication.
 */
class NewPipeDownloaderImpl(
    proxyConfig: ProxyConfig?,
    proxyAuth: String? = null,
) : Downloader() {
    private val client =
        OkHttpClient
            .Builder()
            .proxy(proxyConfig?.let { cfg ->
                Proxy(
                    when (cfg.type) {
                        ProxyType.HTTP -> Proxy.Type.HTTP
                        ProxyType.SOCKS -> Proxy.Type.SOCKS
                    },
                    InetSocketAddress(cfg.host, cfg.port)
                )
            })
            .proxyAuthenticator { _, response ->
                proxyAuth?.let { auth ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }
            .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder =
            okhttp3.Request
                .Builder()
                .method(httpMethod, dataToSend?.toRequestBody())
                .url(url)
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_CHROME)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body.string()
        val latestUrl = response.request.url.toString()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBodyToReturn,
            latestUrl
        )
    }
}

/**
 * Utility class for NewPipe extraction and signature handling.
 */
internal class NewPipeUtils(
    downloader: Downloader,
) {
    init {
        NewPipe.init(downloader)
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> =
        runCatching {
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
    ): Result<String> =
        runCatching {
            val url =
                format.url ?: format.signatureCipher?.let { signatureCipher ->
                    val params = parseQueryString(signatureCipher)
                    val obfuscatedSignature =
                        params["s"]
                            ?: throw ParsingException("Could not parse cipher signature")
                    val signatureParam =
                        params["sp"]
                            ?: throw ParsingException("Could not parse cipher signature parameter")
                    val url =
                        params["url"]?.let { URLBuilder(it) }
                            ?: throw ParsingException("Could not parse cipher url")
                    url.parameters[signatureParam] =
                        YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                            videoId,
                            obfuscatedSignature,
                        )
                    url.buildString()
                } ?: throw ParsingException("Could not find format url")

            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url,
            )
        }
}

/**
 * Singleton for NewPipe extraction and caching.
 */
object NewPipeExtractor {
    private val downloader by lazy {
        NewPipeDownloaderImpl(
            proxyConfig = YouTube.proxyConfig,
            proxyAuth = YouTube.proxyAuth
        )
    }

    private val utils by lazy {
        NewPipeUtils(downloader)
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> =
        utils.getSignatureTimestamp(videoId)

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): Result<String> =
        utils.getStreamUrl(format, videoId)

    fun prewarm(videoId: String = "dQw4w9WgXcQ") {
        try {
            utils.getSignatureTimestamp(videoId)
            YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                videoId,
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            )
        } catch (_: Throwable) {
        }
    }

    /**
     * Extracts all available streams for a YouTube video using NewPipe.
     * Returns a list of (itag, url) pairs.
     */
    fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        return try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId"
            )
            val streamsList =
                streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull {
                (it.itagItem?.id ?: return@mapNotNull null) to it.content
            }
        } catch (e: Exception) {
            Log.w(CacheHolder.TAG, "newPipePlayer extraction failed: ${e.message}", e)
            emptyList()
        }
    }
}

/**
 * Returns cached or freshly extracted stream URLs for a YouTube video.
 */
actual fun getNewPipeStreamUrls(videoId: String): List<Pair<Int, String>> {
    val now = System.currentTimeMillis()
    CacheHolder.streamCache[videoId]?.let { cached ->
        if (now < cached.expiryMs) return cached.list
        else CacheHolder.streamCache.remove(videoId)
    }
    val result = NewPipeExtractor.newPipePlayer(videoId)
    CacheHolder.streamCache[videoId] =
        CacheHolder.StreamCache(result, now + CacheHolder.CACHE_TTL_MS)
    return result
}