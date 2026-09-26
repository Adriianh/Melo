package com.github.adriianh.cli.tui.player.ipc

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalIpcServerTest {
    private lateinit var scope: CoroutineScope
    private lateinit var server: LocalIpcServer
    private val json = Json { ignoreUnknownKeys = true }

    private var playedNowTrack: Track? = null
    private var playedListTracks: List<Track>? = null
    private var queuedListTracks: List<Track>? = null
    private var stopCalled = false
    private val activeQueue = mutableListOf<Track>()

    private val sampleTrack =
        Track(
            id = "test-1",
            title = "Test Title",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 120_000L,
            genres = listOf("Rock"),
            artworkUrl = null,
            sourceId = null,
        )

    @BeforeTest
    fun setup() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        playedNowTrack = null
        playedListTracks = null
        queuedListTracks = null
        stopCalled = false
        activeQueue.clear()

        server =
            LocalIpcServer(
                onPlayPause = {},
                onNext = {},
                onPrevious = {},
                onStop = { stopCalled = true },
                onQueueAdd = { activeQueue.add(it) },
                onQueueRemove = { idx ->
                    if (idx in activeQueue.indices) {
                        activeQueue.removeAt(idx)
                        true
                    } else {
                        false
                    }
                },
                onQueueClear = { activeQueue.clear() },
                getQueue = { activeQueue },
                onPlayNow = { playedNowTrack = it },
                onPlayList = { playedListTracks = it },
                onQueueAddList = { queuedListTracks = it },
            )
        server.start(scope)
        Thread.sleep(150)
    }

    @AfterTest
    fun tearDown() {
        server.stop()
        scope.cancel()
    }

    @Test
    fun pingReturnsPong() {
        val res = LocalIpcClient.sendCommand("PING")
        assertEquals("OK PONG", res)
    }

    @Test
    fun playNowDispatchesTrack() {
        val payload = json.encodeToString(Track.serializer(), sampleTrack)
        val res = LocalIpcClient.sendCommand("PLAY_NOW", payload)
        assertEquals("OK", res)
        assertEquals(sampleTrack, playedNowTrack)
    }

    @Test
    fun playListDispatchesTracks() {
        val list = listOf(sampleTrack, sampleTrack.copy(id = "test-2", title = "Second Track"))
        val payload = json.encodeToString(ListSerializer(Track.serializer()), list)
        val res = LocalIpcClient.sendCommand("PLAY_LIST", payload)
        assertEquals("OK", res)
        assertEquals(list, playedListTracks)
    }

    @Test
    fun queueAddListDispatchesTracks() {
        val list = listOf(sampleTrack, sampleTrack.copy(id = "test-2", title = "Second Track"))
        val payload = json.encodeToString(ListSerializer(Track.serializer()), list)
        val res = LocalIpcClient.sendCommand("QUEUE_ADD_LIST", payload)
        assertEquals("OK", res)
        assertEquals(list, queuedListTracks)
    }

    @Test
    fun stopDispatchesCallback() {
        val res = LocalIpcClient.sendCommand("STOP")
        assertEquals("OK", res)
        assertTrue(stopCalled)
    }

    @Test
    fun invalidPayloadReturnsError() {
        val res = LocalIpcClient.sendCommand("PLAY_NOW", "not-a-json")
        assertEquals("ERROR Invalid payload", res)
    }
}
