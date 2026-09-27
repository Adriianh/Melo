package com.github.adriianh.data.manager

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadManagerStreamTest {
    private lateinit var tempDir: File
    private val getStreamUseCase = mockk<GetStreamUseCase>()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val offlineRepository = mockk<OfflineRepository>(relaxed = true)

    @BeforeTest
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "dm_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        coEvery { getSettingsUseCase.getSnapshot() } returns
            Settings(
                cachePath = File(tempDir, "cache").absolutePath,
                downloadPath = File(tempDir, "downloads").absolutePath,
            )
        coEvery { offlineRepository.getOfflineTrack(any()) } returns null
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun fakeTrack(
        id: String,
        durationMs: Long,
    ) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        durationMs = durationMs,
        genres = emptyList(),
        artworkUrl = null,
        sourceId = null,
    )

    @Test
    fun `cacheTrack skips caching tracks with duration exceeding MAX_AUTO_CACHE_DURATION_MS`() =
        runTest {
            val client = HttpClient(MockEngine { respond("dummy") })
            val manager =
                DownloadManagerImpl(
                    httpClient = client,
                    getStreamUseCase = getStreamUseCase,
                    getSettingsUseCase = getSettingsUseCase,
                    offlineRepository = offlineRepository,
                    configDirPath = tempDir.absolutePath,
                    dispatcher = Dispatchers.Unconfined,
                )

            val longTrack = fakeTrack("long-1", DownloadManager.MAX_AUTO_CACHE_DURATION_MS + 1)
            val result = manager.cacheTrack(longTrack)

            assertFalse(result)
            coVerify(exactly = 0) { getStreamUseCase(any()) }
        }

    @Test
    fun `cacheTrack skips caching tracks with non-positive duration`() =
        runTest {
            val client = HttpClient(MockEngine { respond("dummy") })
            val manager =
                DownloadManagerImpl(
                    httpClient = client,
                    getStreamUseCase = getStreamUseCase,
                    getSettingsUseCase = getSettingsUseCase,
                    offlineRepository = offlineRepository,
                    configDirPath = tempDir.absolutePath,
                    dispatcher = Dispatchers.Unconfined,
                )

            val liveTrack = fakeTrack("live-1", 0L)
            val result = manager.cacheTrack(liveTrack)

            assertFalse(result)
            coVerify(exactly = 0) { getStreamUseCase(any()) }
        }

    @Test
    fun `cacheTrack streams track in chunks and saves to cache`() =
        runTest {
            val audioBytes = ByteArray(128 * 1024) { 7 }
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = audioBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentLength, audioBytes.size.toString()),
                    )
                }
            val client = HttpClient(mockEngine)
            coEvery { getStreamUseCase(any()) } returns "https://stream.audio/track.opus"

            val savedSlot = slot<OfflineTrack>()
            coEvery { offlineRepository.saveOfflineTrack(capture(savedSlot)) } returns Unit

            val manager =
                DownloadManagerImpl(
                    httpClient = client,
                    getStreamUseCase = getStreamUseCase,
                    getSettingsUseCase = getSettingsUseCase,
                    offlineRepository = offlineRepository,
                    configDirPath = tempDir.absolutePath,
                    dispatcher = Dispatchers.Unconfined,
                )

            val track = fakeTrack("normal-1", 180_000L)
            val result = manager.cacheTrack(track)

            assertTrue(result)
            assertTrue(savedSlot.isCaptured)
            assertEquals("normal-1", savedSlot.captured.track.id)
            assertEquals(DownloadStatus.COMPLETED, savedSlot.captured.downloadStatus)
            assertEquals(DownloadType.CACHE, savedSlot.captured.downloadType)
            assertEquals(128 * 1024L, savedSlot.captured.fileSize)
        }

    @Test
    fun `downloadTrack streams to disk and records completed download`() =
        runTest {
            val audioBytes = ByteArray(128 * 1024) { 42 }
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = audioBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentLength, audioBytes.size.toString()),
                    )
                }
            val client = HttpClient(mockEngine)
            coEvery { getStreamUseCase(any()) } returns "https://stream.audio/track.opus"

            val savedSlot = slot<OfflineTrack>()
            coEvery { offlineRepository.saveOfflineTrack(capture(savedSlot)) } returns Unit

            val manager =
                DownloadManagerImpl(
                    httpClient = client,
                    getStreamUseCase = getStreamUseCase,
                    getSettingsUseCase = getSettingsUseCase,
                    offlineRepository = offlineRepository,
                    configDirPath = tempDir.absolutePath,
                    dispatcher = Dispatchers.Unconfined,
                )

            val track = fakeTrack("download-1", 200_000L)
            val result = manager.downloadTrack(track)

            assertTrue(result)
            assertTrue(savedSlot.isCaptured)
            assertEquals("download-1", savedSlot.captured.track.id)
            assertEquals(DownloadStatus.COMPLETED, savedSlot.captured.downloadStatus)
            assertEquals(DownloadType.MANUAL, savedSlot.captured.downloadType)
            assertEquals(128 * 1024L, savedSlot.captured.fileSize)
        }
}
