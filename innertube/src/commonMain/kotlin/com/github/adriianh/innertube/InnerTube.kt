package com.github.adriianh.innertube

import com.github.adriianh.core.platform.currentTimeSeconds
import com.github.adriianh.core.platform.defaultCountryCode
import com.github.adriianh.core.platform.defaultLanguageTag
import com.github.adriianh.core.platform.sha1
import com.github.adriianh.innertube.models.Context
import com.github.adriianh.innertube.models.YouTubeClient
import com.github.adriianh.innertube.models.YouTubeLocale
import com.github.adriianh.innertube.models.body.AccountMenuBody
import com.github.adriianh.innertube.models.body.Action
import com.github.adriianh.innertube.models.body.BrowseBody
import com.github.adriianh.innertube.models.body.CreatePlaylistBody
import com.github.adriianh.innertube.models.body.EditPlaylistBody
import com.github.adriianh.innertube.models.body.FeedbackBody
import com.github.adriianh.innertube.models.body.GetQueueBody
import com.github.adriianh.innertube.models.body.GetSearchSuggestionsBody
import com.github.adriianh.innertube.models.body.GetTranscriptBody
import com.github.adriianh.innertube.models.body.LikeBody
import com.github.adriianh.innertube.models.body.NextBody
import com.github.adriianh.innertube.models.body.PlayerBody
import com.github.adriianh.innertube.models.body.PlaylistDeleteBody
import com.github.adriianh.innertube.models.body.SearchBody
import com.github.adriianh.innertube.models.body.SubscribeBody
import com.github.adriianh.innertube.utils.parseCookieString
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/**
 * Provide access to InnerTube endpoints.
 * For making HTTP requests, not parsing response.
 */
class InnerTube(
    engineOverride: HttpClientEngine? = null,
) {
    private var httpClient = createClient(engineOverride)

    var locale = YouTubeLocale(
        gl = defaultCountryCode(),
        hl = defaultLanguageTag()
    )
    var visitorData: String? = null
    var dataSyncId: String? = null
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    var proxyConfig: ProxyConfig? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var proxyAuth: String? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var useLoginForBrowse: Boolean = false

    private fun createClient(engine: HttpClientEngine? = null): HttpClient {
        val resolvedEngine = engine ?: createPlatformHttpClient(proxyConfig).engine
        return HttpClient(resolvedEngine) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                })
            }

            install(ContentEncoding) {
                gzip(0.9F)
                deflate(0.8F)
            }

            defaultRequest {
                url(YouTubeClient.API_URL_YOUTUBE_MUSIC)
            }
        }
    }

    private fun HttpRequestBuilder.ytClient(
        client: YouTubeClient,
        setLogin: Boolean = false
    ) {
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
            append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
            if (setLogin && client.loginSupported) {
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    append("X-Goog-AuthUser", "0")
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = currentTimeSeconds()
                    val sapisidHash =
                        sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = httpClient.post("like/like") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target(videoId = videoId)
            )
        )
    }

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = httpClient.post("like/removelike") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target(videoId = videoId)
            )
        )
    }

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = httpClient.post("like/like") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target(playlistId = playlistId)
            )
        )
    }

    suspend fun unlikePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = httpClient.post("like/removelike") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target(playlistId = playlistId)
            )
        )
    }

    suspend fun likeAlbum(
        client: YouTubeClient,
        browseId: String,
    ) = httpClient.post("like/like") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target(playlistId = browseId)
            )
        )
    }

    suspend fun unlikeAlbum(
        client: YouTubeClient,
        browseId: String,
    ) = httpClient.post("like/removelike") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target(playlistId = browseId)
            )
        )
    }

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
    ) = httpClient.post("playlist/create") {
        ytClient(client, true)
        setBody(
            CreatePlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                title = title
            )
        )
    }

    suspend fun addPlaylistToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        addPlaylistId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId.removePrefix("VL"),
                actions = listOf(
                    Action.AddPlaylistAction(addedFullListId = addPlaylistId)
                )
            )
        )
    }

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
    ) = httpClient.post("subscription/subscribe") {
        ytClient(client, setLogin = true)
        setBody(
            SubscribeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                channelIds = listOf(channelId)
            )
        )
    }

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
    ) = httpClient.post("subscription/unsubscribe") {
        ytClient(client, setLogin = true)
        setBody(
            SubscribeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                channelIds = listOf(channelId)
            )
        )
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ) = httpClient.post("search") {
        ytClient(client, setLogin = useLoginForBrowse)
        setBody(
            SearchBody(
                context = client.toContext(
                    locale,
                    visitorData,
                    if (useLoginForBrowse) dataSyncId else null
                ),
                query = query,
                params = params
            )
        )
        parameter("continuation", continuation)
        parameter("ctoken", continuation)
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
    ) = httpClient.post("${client.apiUrl}player") {
        ytClient(client, setLogin = client.loginSupported && cookie != null)
        setBody(
            PlayerBody(
                context = client.toContext(locale, visitorData, dataSyncId).let {
                    if (client.isEmbedded) {
                        it.copy(
                            thirdParty = Context.ThirdParty(
                                embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                            )
                        )
                    } else it
                },
                videoId = videoId,
                playlistId = playlistId,
                playbackContext = if (client.useSignatureTimestamp && signatureTimestamp != null) {
                    PlayerBody.PlaybackContext(
                        PlayerBody.PlaybackContext.ContentPlaybackContext(signatureTimestamp)
                    )
                } else null,
            )
        )
    }

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String?,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
    ) = httpClient.get(url) {
        ytClient(client, true)
        parameter("ver", "2")
        parameter("c", client.clientName)
        parameter("cpn", cpn)

        if (playlistId != null) {
            parameter("list", playlistId)
            parameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
        }
    }

    suspend fun registerWatchtime(
        url: String,
        cpn: String,
        playlistId: String?,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
    ) = httpClient.get(url) {
        ytClient(client, true)
        parameter("ver", "2")
        parameter("c", client.clientName)
        parameter("cpn", cpn)
        parameter("state", "playing")
        parameter("st", "0")
        parameter("et", "10")
        parameter("cmt", "10")

        if (playlistId != null) {
            parameter("list", playlistId)
            parameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
        }
    }

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ) = httpClient.post("browse") {
        ytClient(client, setLogin = setLogin || useLoginForBrowse)
        setBody(
            BrowseBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                browseId = browseId,
                params = params,
                continuation = continuation
            )
        )
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = httpClient.post("next") {
        ytClient(client, setLogin = true)
        setBody(
            NextBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                videoId = videoId,
                playlistId = playlistId,
                playlistSetVideoId = playlistSetVideoId,
                index = index,
                params = params,
                continuation = continuation
            )
        )
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = httpClient.post("music/get_search_suggestions") {
        ytClient(client)
        setBody(
            GetSearchSuggestionsBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                input = input
            )
        )
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = httpClient.post("music/get_queue") {
        ytClient(client)
        setBody(
            GetQueueBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                videoIds = videoIds,
                playlistId = playlistId
            )
        )
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ) = httpClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
        parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
        headers {
            append("Content-Type", "application/json")
        }
        setBody(
            GetTranscriptBody(
                context = client.toContext(locale, null, dataSyncId),
                params = Base64.encode("\n${11.toChar()}$videoId".encodeToByteArray())
            )
        )
    }

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId,
                actions = listOf(
                    Action.RenamePlaylistAction(
                        playlistName = name
                    )
                )
            )
        )
    }

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = httpClient.post("playlist/delete") {
        ytClient(client, setLogin = true)
        setBody(
            PlaylistDeleteBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId
            )
        )
    }

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId.removePrefix("VL"),
                actions = listOf(
                    Action.AddVideoAction(addedVideoId = videoId)
                )
            )
        )
    }

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId.removePrefix("VL"),
                actions = listOf(
                    Action.RemoveVideoAction(
                        removedVideoId = videoId,
                        setVideoId = setVideoId,
                        params = "Pw%3D%3D"
                    )
                )
            )
        )
    }

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId,
                actions = listOf(
                    Action.MoveVideoAction(
                        movedSetVideoIdSuccessor = successorSetVideoId,
                        setVideoId = setVideoId,
                    )
                )

            )
        )
    }

    suspend fun feedback(
        client: YouTubeClient,
        tokens: List<String>
    ) = httpClient.post("feedback") {
        ytClient(client, setLogin = true)
        setBody(
            FeedbackBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                feedbackTokens = tokens
            )
        )
    }

    suspend fun getSwJsData() = httpClient.get("https://music.youtube.com/sw.js_data")

    suspend fun accountMenu(client: YouTubeClient) = httpClient.post("account/account_menu") {
        ytClient(client, setLogin = true)
        setBody(AccountMenuBody(client.toContext(locale, visitorData, dataSyncId)))
    }
}
