package com.github.adriianh.innertube

import com.github.adriianh.innertube.models.Context
import com.github.adriianh.innertube.models.YouTubeClient
import com.github.adriianh.innertube.models.YouTubeLocale
import com.github.adriianh.innertube.models.body.LikeBody
import com.github.adriianh.innertube.models.body.PlayerBody
import com.github.adriianh.innertube.models.body.SearchBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InnerTubeHttpTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun `SearchBody serializes correctly`() {
        val context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.0",
                osVersion = null,
                gl = "US",
                hl = "en",
                visitorData = null
            )
        )

        val body = SearchBody(context = context, query = "Bad Bunny", params = null)
        val encoded = json.encodeToString(SearchBody.serializer(), body)

        assertTrue(encoded.contains("Bad Bunny"), "Should contain query")
        assertTrue(encoded.contains("WEB_REMIX"), "Should contain client name")
        assertTrue(encoded.contains("context"), "Should contain context key")
    }

    @Test
    fun `PlayerBody serializes with playbackContext`() {
        val context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.0",
                osVersion = null,
                gl = "US",
                hl = "en",
                visitorData = null
            )
        )

        val body = PlayerBody(
            context = context,
            videoId = "dQw4w9WgXcQ",
            playlistId = "PLtest123",
            playbackContext = PlayerBody.PlaybackContext(
                PlayerBody.PlaybackContext.ContentPlaybackContext(signatureTimestamp = 20078)
            )
        )
        val encoded = json.encodeToString(PlayerBody.serializer(), body)

        assertTrue(encoded.contains("dQw4w9WgXcQ"), "Should contain videoId")
        assertTrue(encoded.contains("20078"), "Should contain signatureTimestamp")
        assertTrue(encoded.contains("contentCheckOk"), "Should contain contentCheckOk")
    }

    @Test
    fun `YouTubeClient toContext maps fields correctly`() {
        val client = YouTubeClient.WEB_REMIX
        val locale = YouTubeLocale(gl = "MX", hl = "es")
        val visitorData = "visitor123"
        val dataSyncId = "sync456"

        val context = client.toContext(locale, visitorData, dataSyncId)

        assertEquals("WEB_REMIX", context.client.clientName)
        assertEquals("1.20251227.01.00", context.client.clientVersion)
        assertEquals("MX", context.client.gl)
        assertEquals("es", context.client.hl)
        assertEquals("visitor123", context.client.visitorData)
    }

    @Test
    fun `YouTubeClient toContext serializes onBehalfOfUser for login clients`() {
        val client = YouTubeClient.WEB_REMIX // loginSupported = true
        val locale = YouTubeLocale(gl = "US", hl = "en")
        val dataSyncId = "sync789"

        val context = client.toContext(locale, null, dataSyncId)

        val encoded = json.encodeToString(Context.serializer(), context)
        assertTrue(encoded.contains("sync789"), "Should serialize onBehalfOfUser for login clients")
    }

    @Test
    fun `YouTubeClient toContext omits onBehalfOfUser for non-login clients`() {
        val client = YouTubeClient.WEB // loginSupported = false
        val locale = YouTubeLocale(gl = "US", hl = "en")

        val context = client.toContext(locale, null, "sync789")

        val encoded = json.encodeToString(Context.serializer(), context)
        assertTrue(
            !encoded.contains("onBehalfOfUser") || !encoded.contains("sync789"),
            "Should not contain onBehalfOfUser for non-login clients"
        )
    }

    @Test
    fun `MockEngine handles search POST request`() = runTest {
        val mockResponse = """
            {
                "contents": {
                    "tabbedSearchResultsRenderer": {
                        "tabs": []
                    }
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = io.ktor.http.headersOf("Content-Type", "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@InnerTubeHttpTest.json)
            }
        }

        val response = client.post {
            url("https://music.youtube.com/youtubei/v1/search")
            contentType(ContentType.Application.Json)
            setBody(
                SearchBody(
                    context = Context(
                        client = Context.Client(
                            clientName = "WEB_REMIX",
                            clientVersion = "1.0",
                            osVersion = null,
                            gl = "US",
                            hl = "en",
                            visitorData = null
                        )
                    ),
                    query = "Test Query",
                    params = null
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("tabbedSearchResultsRenderer"))
    }

    @Test
    fun `MockEngine handles error response`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Unauthorized",
                status = HttpStatusCode.Unauthorized
            )
        }

        val client = HttpClient(mockEngine)

        val response = client.post {
            url("https://music.youtube.com/youtubei/v1/player")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `MockEngine handles player response with streamingData`() = runTest {
        val mockResponse = """
            {
                "streamingData": {
                    "adaptiveFormats": [
                        {
                            "itag": 251,
                            "url": "https://example.com/stream",
                            "mimeType": "audio/webm; codecs=opus",
                            "bitrate": 128000
                        }
                    ]
                },
                "videoDetails": {
                    "videoId": "dQw4w9WgXcQ",
                    "title": "Never Gonna Give You Up",
                    "lengthSeconds": "212"
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = io.ktor.http.headersOf("Content-Type", "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@InnerTubeHttpTest.json)
            }
        }

        val response = client.post {
            url("https://music.youtube.com/youtubei/v1/player")
            contentType(ContentType.Application.Json)
            setBody(
                PlayerBody(
                    context = Context(
                        client = Context.Client(
                            clientName = "WEB_REMIX",
                            clientVersion = "1.0",
                            osVersion = null,
                            gl = "US",
                            hl = "en",
                            visitorData = null
                        )
                    ),
                    videoId = "dQw4w9WgXcQ",
                    playlistId = null
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("streamingData"))
        assertTrue(body.contains("adaptiveFormats"))
    }

    @Test
    fun `Context serializes and deserializes correctly`() {
        val original = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.0",
                osVersion = "Windows 10",
                gl = "US",
                hl = "en",
                visitorData = "abc123"
            )
        )

        val encoded = json.encodeToString(Context.serializer(), original)
        val decoded = json.decodeFromString(Context.serializer(), encoded)

        assertEquals(original.client.clientName, decoded.client.clientName)
        assertEquals(original.client.gl, decoded.client.gl)
        assertEquals(original.client.visitorData, decoded.client.visitorData)
    }

    @Test
    fun `LikeBody serializes playlist target without polymorphic type discriminator`() {
        val context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.0",
                osVersion = null,
                gl = "US",
                hl = "en",
                visitorData = null
            )
        )
        val body = LikeBody(
            context = context,
            target = LikeBody.Target(playlistId = "OLAK5uy_test")
        )
        val encoded = json.encodeToString(LikeBody.serializer(), body)

        assertTrue(encoded.contains("\"playlistId\":\"OLAK5uy_test\""))
        assertTrue(
            !encoded.contains("\"type\""),
            "Should not contain polymorphic class discriminator"
        )
    }

    @Test
    fun `LikeBody serializes video target without polymorphic type discriminator`() {
        val context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.0",
                osVersion = null,
                gl = "US",
                hl = "en",
                visitorData = null
            )
        )
        val body = LikeBody(
            context = context,
            target = LikeBody.Target(videoId = "vid123")
        )
        val encoded = json.encodeToString(LikeBody.serializer(), body)

        assertTrue(encoded.contains("\"videoId\":\"vid123\""))
        assertTrue(
            !encoded.contains("\"type\""),
            "Should not contain polymorphic class discriminator"
        )
    }

    @Test
    fun `registerPlayback and registerWatchtime send correct URLs and headers`() = runTest {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val mockEngine = MockEngine { request ->
            requests.add(request)
            respond(
                content = "",
                status = HttpStatusCode.NoContent,
            )
        }

        val innerTube = InnerTube(mockEngine)
        innerTube.cookie = "SAPISID=test-sapisid; SID=test-sid"

        innerTube.registerPlayback(
            url = "https://s.youtube.com/api/stats/playback?ns=yt&docid=dQw4w9WgXcQ",
            cpn = "test_cpn_123456",
            playlistId = "PLtest",
            client = YouTubeClient.WEB_REMIX
        )

        innerTube.registerWatchtime(
            url = "https://s.youtube.com/api/stats/watchtime?ns=yt&docid=dQw4w9WgXcQ",
            cpn = "test_cpn_123456",
            playlistId = "PLtest",
            client = YouTubeClient.WEB_REMIX
        )

        assertEquals(2, requests.size)
        val playbackReq = requests[0]
        println("Playback URL: " + playbackReq.url.toString())
        assertTrue(
            playbackReq.url.encodedPath.contains("api/stats/playback"),
            "Path should contain api/stats/playback, got: ${playbackReq.url.encodedPath}"
        )
        assertEquals("2", playbackReq.url.parameters["ver"])
        assertEquals("WEB_REMIX", playbackReq.url.parameters["c"])
        assertEquals("test_cpn_123456", playbackReq.url.parameters["cpn"])
        assertEquals("PLtest", playbackReq.url.parameters["list"])
        assertTrue(
            playbackReq.headers.contains("Authorization"),
            "Should contain SAPISIDHASH Authorization header"
        )
        assertTrue(playbackReq.headers.contains("Origin"), "Should contain Origin header")

        val watchtimeReq = requests[1]
        println("Watchtime URL: " + watchtimeReq.url.toString())
        assertTrue(
            watchtimeReq.url.encodedPath.contains("api/stats/watchtime"),
            "Path should contain api/stats/watchtime, got: ${watchtimeReq.url.encodedPath}"
        )
        assertEquals("playing", watchtimeReq.url.parameters["state"])
        assertEquals("10", watchtimeReq.url.parameters["cmt"])
    }

    @Test
    fun `player request URL is properly formed`() = runTest {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val mockEngine = MockEngine { request ->
            requests.add(request)
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
            )
        }

        val innerTube = InnerTube(mockEngine)
        innerTube.player(
            client = YouTubeClient.WEB_REMIX,
            videoId = "test_vid_123",
            playlistId = null,
            signatureTimestamp = null
        )

        assertEquals(1, requests.size)
        val playerReq = requests[0]
        println("Player request full URL: " + playerReq.url.toString())
        println("Player request path: " + playerReq.url.encodedPath)
        assertEquals("/youtubei/v1/player", playerReq.url.encodedPath)
    }
}