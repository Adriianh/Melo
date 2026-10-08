package com.github.adriianh.melo.service

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "DiscordGateway"

class DiscordGatewayClient(
    private val token: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    private val wsClient =
        HttpClient {
            install(WebSockets) {
                maxFrameSize = Long.MAX_VALUE
            }
        }

    private val httpClient =
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 6_000L
                connectTimeoutMillis = 6_000L
            }
        }

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val assetCache = ConcurrentHashMap<String, String>()

    suspend fun resolveExternalAsset(
        url: String,
        applicationId: String,
    ): String? {
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return null
        }
        assetCache[url]?.let { return it }

        return try {
            val response =
                httpClient.post("https://discord.com/api/v10/applications/$applicationId/external-assets") {
                    header(HttpHeaders.Authorization, token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"urls":["$url"]}""")
                }
            if (response.status == HttpStatusCode.OK) {
                val list = json.decodeFromString<List<ExternalAssetResponse>>(response.bodyAsText())
                val path = list.firstOrNull()?.externalAssetPath
                if (!path.isNullOrBlank()) {
                    val mpAsset = "mp:$path"
                    assetCache[url] = mpAsset
                    Log.d(TAG, "Resolved external asset for $url -> $mpAsset")
                    mpAsset
                } else {
                    Log.w(TAG, "external-assets returned empty path for $url")
                    null
                }
            } else {
                Log.w(TAG, "external-assets failed with status: ${response.status}")
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve external asset: ${e.message}")
            null
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var presenceJob: Job? = null
    private var sequence: Int? = null
    private var isReady = false
    private var pendingPresence: PresenceData? = null

    fun connect() {
        if (connectionJob?.isActive == true) {
            Log.d(TAG, "connect() called but already active")
            return
        }
        Log.d(TAG, "connect() starting connection loop with token ${token.take(6)}...")
        connectionJob =
            scope.launch {
                while (isActive) {
                    runConnectionLoop()
                    delay(RECONNECT_DELAY_MS.milliseconds)
                }
            }
    }

    private suspend fun runConnectionLoop() {
        try {
            Log.d(TAG, "Connecting to WebSocket: $GATEWAY_URL")
            wsClient.webSocketSession(GATEWAY_URL).also { ws ->
                session = ws
                Log.d(TAG, "WebSocket connected successfully")
                ws.incoming.receiveAsFlow().collect { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        Log.d(TAG, "Received frame (len=${text.length}): ${text.take(150)}")
                        handleMessage(text)
                    }
                }
                val closeReason = ws.closeReason.await()
                Log.w(TAG, "WebSocket closed by server: code=${closeReason?.code}, reason=${closeReason?.message}")
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Connection loop cancelled")
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Connection loop error: ${e.message}", e)
        } finally {
            cleanupSession()
        }
    }

    private fun cleanupSession() {
        Log.d(TAG, "cleanupSession called")
        isReady = false
        presenceJob?.cancel()
        presenceJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        session = null
    }

    private suspend fun handleMessage(text: String) {
        val payload = json.decodeFromString<GatewayPayload>(text)
        payload.s?.let { sequence = it }

        Log.d(TAG, "handleMessage: op=${payload.op}, t=${payload.t}, s=${payload.s}")
        when (payload.op) {
            OP_HELLO -> handleHello(payload)
            OP_DISPATCH -> handleDispatch(payload)
            OP_HEARTBEAT -> sendHeartbeat()
            OP_RECONNECT -> {
                Log.w(TAG, "Gateway sent OP_RECONNECT (7)")
                session?.close(CloseReason(4000, "Reconnect"))
            }
            OP_INVALID_SESSION -> {
                Log.w(TAG, "Gateway sent OP_INVALID_SESSION (9)")
                delay(150.milliseconds)
                sendIdentify()
            }
        }
    }

    private suspend fun handleHello(payload: GatewayPayload) {
        val hello = payload.d?.let { json.decodeFromJsonElement<HelloData>(it) }
        Log.d(TAG, "handleHello: heartbeatInterval=${hello?.heartbeatInterval}")
        hello?.heartbeatInterval?.let { startHeartbeat(it) }
        sendIdentify()
    }

    private suspend fun handleDispatch(payload: GatewayPayload) {
        if (payload.t == "READY") {
            isReady = true
            Log.i(TAG, "Gateway is READY! Sending presence now")
            val presence = pendingPresence ?: PresenceData(activities = emptyList())
            sendPresence(presence)
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        Log.d(TAG, "Starting heartbeat every ${intervalMs}ms")
        heartbeatJob =
            scope.launch {
                while (isActive) {
                    delay(intervalMs.milliseconds)
                    sendHeartbeat()
                }
            }
    }

    private suspend fun sendHeartbeat() {
        Log.d(TAG, "Sending heartbeat with seq=$sequence")
        sendPayload(OP_HEARTBEAT, sequence?.let { json.encodeToJsonElement(it) })
    }

    private suspend fun sendIdentify() {
        Log.d(TAG, "Sending OP_IDENTIFY")
        val identify =
            IdentifyData(
                token = token,
                properties =
                    IdentifyProperties(
                        os = "Windows",
                        browser = "Discord Client",
                        device = "ktor",
                    ),
                capabilities = 65,
                compress = false,
                largeThreshold = 100,
            )
        sendPayload(OP_IDENTIFY, json.encodeToJsonElement(identify))
    }

    fun updatePresence(presence: PresenceData?) {
        pendingPresence = presence
        Log.d(TAG, "updatePresence called: isReady=$isReady, presence=$presence")
        if (!isReady) return
        presenceJob?.cancel()
        presenceJob =
            scope.launch {
                val toSend = presence ?: PresenceData(activities = emptyList())
                sendPresence(toSend)
            }
    }

    private suspend fun sendPresence(presence: PresenceData) {
        Log.d(TAG, "Sending OP_PRESENCE_UPDATE: $presence")
        sendPayload(OP_PRESENCE_UPDATE, json.encodeToJsonElement(presence))
    }

    private suspend fun sendPayload(
        op: Int,
        data: JsonElement?,
    ) {
        val payload = GatewayPayload(op = op, d = data)
        val text = json.encodeToString(payload)
        session?.send(Frame.Text(text))
    }

    fun disconnect() {
        Log.d(TAG, "disconnect called")
        connectionJob?.cancel()
        connectionJob = null
        cleanupSession()
        scope.launch {
            try {
                session?.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnect"))
            } catch (_: Throwable) {
            }
            session = null
            try {
                wsClient.close()
                httpClient.close()
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
        private const val OP_DISPATCH = 0
        private const val OP_HEARTBEAT = 1
        private const val OP_IDENTIFY = 2
        private const val OP_PRESENCE_UPDATE = 3
        private const val OP_RECONNECT = 7
        private const val OP_INVALID_SESSION = 9
        private const val OP_HELLO = 10
        private const val RECONNECT_DELAY_MS = 5000L
    }
}

@Serializable
data class GatewayPayload(
    val op: Int,
    val d: JsonElement? = null,
    val s: Int? = null,
    val t: String? = null,
)

@Serializable
data class HelloData(
    @SerialName("heartbeat_interval")
    val heartbeatInterval: Long,
)

@Serializable
data class IdentifyData(
    val token: String,
    val properties: IdentifyProperties,
    val capabilities: Int = 65,
    val compress: Boolean = false,
    @SerialName("large_threshold")
    val largeThreshold: Int = 100,
)

@Serializable
data class IdentifyProperties(
    val os: String,
    val browser: String,
    val device: String,
)

@Serializable
data class PresenceData(
    val activities: List<ActivityData>,
    val status: String = "online",
    val afk: Boolean = false,
    val since: Long? = null,
)

@Serializable
data class ActivityData(
    val name: String,
    val type: Int = 2,
    val details: String? = null,
    val state: String? = null,
    val timestamps: TimestampsData? = null,
    val assets: AssetsData? = null,
    val buttons: List<String>? = null,
    val metadata: ActivityMetadata? = null,
    @SerialName("application_id")
    val applicationId: String? = null,
    @SerialName("status_display_type")
    val statusDisplayType: Int? = 1,
)

@Serializable
data class TimestampsData(
    val start: Long? = null,
    val end: Long? = null,
)

@Serializable
data class AssetsData(
    @SerialName("large_image")
    val largeImage: String? = null,
    @SerialName("large_text")
    val largeText: String? = null,
)

@Serializable
data class ActivityMetadata(
    @SerialName("button_urls")
    val buttonUrls: List<String>? = null,
)

@Serializable
data class ExternalAssetResponse(
    @SerialName("external_asset_path")
    val externalAssetPath: String,
    val url: String? = null,
)
