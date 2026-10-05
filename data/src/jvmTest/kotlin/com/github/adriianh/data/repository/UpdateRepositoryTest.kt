package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdatePlatform
import com.github.adriianh.core.domain.repository.DownloadProgress
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateRepositoryTest {
    @Test
    fun `isNewerVersion correctly compares semantic versions`() {
        assertTrue(UpdateRepositoryImpl.isNewerVersion("1.0.2", "1.0.1"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("1.1.0", "1.0.9"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("1.0.2-beta.1", "1.0.1"))

        assertFalse(UpdateRepositoryImpl.isNewerVersion("1.0.1", "1.0.1"))
        assertFalse(UpdateRepositoryImpl.isNewerVersion("1.0.0", "1.0.1"))
        assertFalse(UpdateRepositoryImpl.isNewerVersion("0.9.9", "1.0.0"))
    }

    @Test
    fun `isNewerVersion treats nightly suffix as its numeric base`() {
        // A nightly build (2.1.5-nightly.<date>) is not newer than an official
        // 2.1.5 release of the same base version.
        assertFalse(UpdateRepositoryImpl.isNewerVersion("2.1.5", "2.1.5-nightly.20260923"))
        // But it is newer than an older base, and older than a future one.
        assertTrue(UpdateRepositoryImpl.isNewerVersion("2.1.5-nightly.20260923", "2.1.4"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("2.1.6", "2.1.5-nightly.20260923"))
        // A nightly is never "newer" than itself.
        assertFalse(
            UpdateRepositoryImpl.isNewerVersion(
                "2.1.5-nightly.20260923",
                "2.1.5-nightly.20260923",
            ),
        )
    }

    @Test
    fun `isNewerNightly accurately compares timestamps and handles stable switch`() {
        assertTrue(
            NightlyVersionComparator.isNewerNightly(
                "2.2.1-nightly.20261005.1430",
                "2.2.1-nightly.20261005.0915",
            ),
        )
        assertFalse(
            NightlyVersionComparator.isNewerNightly(
                "2.2.1-nightly.20261005.0915",
                "2.2.1-nightly.20261005.1430",
            ),
        )
        assertFalse(
            NightlyVersionComparator.isNewerNightly(
                "2.2.1-nightly.20261005.1430",
                "2.2.1-nightly.20261005.1430",
            ),
        )
        assertTrue(
            NightlyVersionComparator.isNewerNightly(
                "2.2.1-nightly.20261006.0100",
                "2.2.1-nightly.20261005.2300",
            ),
        )
        assertTrue(
            NightlyVersionComparator.isNewerNightly(
                "2.2.1-nightly.20261005.1200",
                "2.2.1-nightly.20261005",
            ),
        )
        assertTrue(
            NightlyVersionComparator.isNewerNightly(
                "2.2.1-nightly.20261005.1430",
                "2.2.1",
            ),
        )
        assertTrue(
            NightlyVersionComparator.isNewerNightly(
                "2.3.0-nightly.20261005.1000",
                "2.2.1-nightly.20261005.1400",
            ),
        )
        assertFalse(
            NightlyVersionComparator.isNewerNightly(
                "2.2.0-nightly.20261005.1400",
                "2.2.1-nightly.20261005.1000",
            ),
        )
    }

    @Test
    fun `isNewerStable allows upgrading from nightly to current or newer stable base`() {
        assertTrue(NightlyVersionComparator.isNewerStable("2.2.2", "2.2.1"))
        assertFalse(NightlyVersionComparator.isNewerStable("2.2.1", "2.2.1"))
        assertTrue(NightlyVersionComparator.isNewerStable("2.2.1", "2.2.1-nightly.20261005.1430"))
        assertFalse(NightlyVersionComparator.isNewerStable("2.2.0", "2.2.1-nightly.20261005.1430"))
    }

    @Test
    fun `checkForUpdate with NIGHTLY channel targets nightly release and extracts version`() =
        runTest {
            val mockJson =
                """
                {
                    "tag_name": "nightly",
                    "name": "Melo Nightly",
                    "body": "Nightly release\nVersion: `2.2.1-nightly.20261005.1430`",
                    "html_url": "https://github.com/Adriianh/Melo/releases/tag/nightly",
                    "published_at": "2026-10-05T14:30:00Z",
                    "assets": [
                        {
                            "name": "melo-nightly-linux.tar.gz",
                            "browser_download_url": "https://github.com/Adriianh/Melo/releases/download/nightly/melo-nightly-linux.tar.gz",
                            "size": 42000000
                        }
                    ]
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "https://api.github.com/repos/Adriianh/Melo/releases/tags/nightly",
                        request.url.toString(),
                    )
                    respond(
                        content = mockJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val repo = UpdateRepositoryImpl(client, Dispatchers.Unconfined)
            val result = repo.checkForUpdate("2.2.1-nightly.20261005.0915", UpdateChannel.NIGHTLY)

            assertTrue(result.isSuccess)
            val release = result.getOrNull()
            assertNotNull(release)
            assertEquals("2.2.1-nightly.20261005.1430", release.version)
            assertEquals(UpdateChannel.NIGHTLY, release.channel)
            assertEquals(1, release.assets.size)
        }

    @Test
    fun `checkForUpdate returns release when newer version exists`() =
        runTest {
            val mockJson =
                """
                {
                    "tag_name": "v1.0.2",
                    "name": "Melo 1.0.2",
                    "body": "Fixed bugs and added auto-updater",
                    "html_url": "https://github.com/Adriianh/Melo/releases/tag/v1.0.2",
                    "published_at": "2026-09-07T00:00:00Z",
                    "assets": [
                        {
                            "name": "Melo-Setup-x64.exe",
                            "browser_download_url": "https://github.com/Adriianh/Melo/releases/download/v1.0.2/Melo-Setup-x64.exe",
                            "size": 52428800
                        },
                        {
                            "name": "melo_1.0.2_amd64.deb",
                            "browser_download_url": "https://github.com/Adriianh/Melo/releases/download/v1.0.2/melo_1.0.2_amd64.deb",
                            "size": 52428800
                        },
                        {
                            "name": "Melo-1.0.2.dmg",
                            "browser_download_url": "https://github.com/Adriianh/Melo/releases/download/v1.0.2/Melo-1.0.2.dmg",
                            "size": 52428800
                        },
                        {
                            "name": "app-release.apk",
                            "browser_download_url": "https://github.com/Adriianh/Melo/releases/download/v1.0.2/app-release.apk",
                            "size": 35000000
                        }
                    ]
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = mockJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val repo = UpdateRepositoryImpl(client, Dispatchers.Unconfined)
            val result = repo.checkForUpdate("1.0.1")

            assertTrue(result.isSuccess)
            val release = result.getOrNull()
            assertNotNull(release)
            assertEquals("1.0.2", release.version)
            assertEquals("v1.0.2", release.tagName)
            assertEquals(4, release.assets.size)

            val winAsset = release.getAssetForPlatform(UpdatePlatform.WINDOWS)
            assertNotNull(winAsset)
            assertEquals("Melo-Setup-x64.exe", winAsset.name)

            val linuxAsset = release.getAssetForPlatform(UpdatePlatform.LINUX)
            assertNotNull(linuxAsset)
            assertEquals("melo_1.0.2_amd64.deb", linuxAsset.name)

            val macAsset = release.getAssetForPlatform(UpdatePlatform.MACOS)
            assertNotNull(macAsset)
            assertEquals("Melo-1.0.2.dmg", macAsset.name)

            val androidAsset = release.getAssetForPlatform(UpdatePlatform.ANDROID)
            assertNotNull(androidAsset)
            assertEquals("app-release.apk", androidAsset.name)
        }

    @Test
    fun `checkForUpdate returns null when already on latest version`() =
        runTest {
            val mockJson =
                """
                {
                    "tag_name": "v1.0.1",
                    "name": "Melo 1.0.1",
                    "body": "Initial release",
                    "html_url": "https://github.com/Adriianh/Melo/releases/tag/v1.0.1",
                    "published_at": "2026-09-06T00:00:00Z",
                    "assets": []
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = mockJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val repo = UpdateRepositoryImpl(client, Dispatchers.Unconfined)
            val result = repo.checkForUpdate("1.0.1")

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

    @Test
    fun `detectPlatformFromFileName correctly identifies platforms`() {
        assertEquals(UpdatePlatform.WINDOWS, UpdateRepositoryImpl.detectPlatformFromFileName("Melo-Setup.exe"))
        assertEquals(UpdatePlatform.WINDOWS, UpdateRepositoryImpl.detectPlatformFromFileName("Melo-2.2.1.msi"))
        assertEquals(UpdatePlatform.WINDOWS, UpdateRepositoryImpl.detectPlatformFromFileName("melo-2.2.1-windows.zip"))

        assertEquals(
            UpdatePlatform.LINUX,
            UpdateRepositoryImpl.detectPlatformFromFileName("Melo-2.2.1-x86_64.AppImage"),
        )
        assertEquals(UpdatePlatform.LINUX, UpdateRepositoryImpl.detectPlatformFromFileName("melo_2.2.1_amd64.deb"))
        assertEquals(UpdatePlatform.LINUX, UpdateRepositoryImpl.detectPlatformFromFileName("melo-2.2.1-1.x86_64.rpm"))
        assertEquals(UpdatePlatform.LINUX, UpdateRepositoryImpl.detectPlatformFromFileName("melo-2.2.1-linux.tar.gz"))

        assertEquals(UpdatePlatform.MACOS, UpdateRepositoryImpl.detectPlatformFromFileName("Melo-2.2.1.dmg"))
        assertEquals(UpdatePlatform.MACOS, UpdateRepositoryImpl.detectPlatformFromFileName("Melo-2.2.1.pkg"))
        assertEquals(UpdatePlatform.MACOS, UpdateRepositoryImpl.detectPlatformFromFileName("melo-2.2.1-macos.tar.gz"))

        assertEquals(UpdatePlatform.ANDROID, UpdateRepositoryImpl.detectPlatformFromFileName("app-release.apk"))

        assertEquals(UpdatePlatform.UNKNOWN, UpdateRepositoryImpl.detectPlatformFromFileName("checksums.sha256"))
        assertEquals(UpdatePlatform.UNKNOWN, UpdateRepositoryImpl.detectPlatformFromFileName("README.md"))
    }

    @Test
    fun `downloadAsset streams chunks and emits progress and completed`() =
        runTest {
            val payload = ByteArray(2048) { (it % 128).toByte() }
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(payload),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentLength, payload.size.toString()),
                    )
                }
            val client =
                HttpClient(mockEngine) {
                    install(HttpTimeout)
                }
            val repo = UpdateRepositoryImpl(client, Dispatchers.Unconfined)

            val tempFile = File.createTempFile("melo_update_test", ".exe")
            tempFile.delete()

            try {
                val asset =
                    ReleaseAsset(
                        name = "Melo-Setup.exe",
                        downloadUrl = "https://example.com/Melo-Setup.exe",
                        sizeBytes = payload.size.toLong(),
                        platform = UpdatePlatform.WINDOWS,
                    )

                val events = repo.downloadAsset(asset, tempFile.absolutePath).toList()

                assertTrue(events.isNotEmpty())
                assertIs<DownloadProgress.Progress>(events.first())
                val lastEvent = events.last()
                assertIs<DownloadProgress.Completed>(lastEvent)
                assertEquals(tempFile.absolutePath, lastEvent.filePath)

                assertTrue(tempFile.exists())
                assertEquals(payload.size.toLong(), tempFile.length())
                assertEquals(payload.toList(), tempFile.readBytes().toList())
            } finally {
                tempFile.delete()
                File("${tempFile.absolutePath}.tmp").delete()
            }
        }

    @Test
    fun `downloadAsset throws when HTTP fails and cleans up temp file`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound,
                    )
                }
            val client =
                HttpClient(mockEngine) {
                    install(HttpTimeout)
                }
            val repo = UpdateRepositoryImpl(client, Dispatchers.Unconfined)

            val tempFile = File.createTempFile("melo_update_fail_test", ".exe")
            tempFile.delete()

            try {
                val asset =
                    ReleaseAsset(
                        name = "Melo-Setup.exe",
                        downloadUrl = "https://example.com/404.exe",
                        sizeBytes = 1000L,
                        platform = UpdatePlatform.WINDOWS,
                    )

                assertFailsWith<IllegalStateException> {
                    repo.downloadAsset(asset, tempFile.absolutePath).toList()
                }

                assertFalse(tempFile.exists())
                assertFalse(File("${tempFile.absolutePath}.tmp").exists())
            } finally {
                tempFile.delete()
                File("${tempFile.absolutePath}.tmp").delete()
            }
        }

    @Test
    fun `downloadAsset throws when downloaded content is empty`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(ByteArray(0)),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentLength, "0"),
                    )
                }
            val client =
                HttpClient(mockEngine) {
                    install(HttpTimeout)
                }
            val repo = UpdateRepositoryImpl(client, Dispatchers.Unconfined)

            val tempFile = File.createTempFile("melo_update_empty_test", ".exe")
            tempFile.delete()

            try {
                val asset =
                    ReleaseAsset(
                        name = "Melo-Setup.exe",
                        downloadUrl = "https://example.com/empty.exe",
                        sizeBytes = 0L,
                        platform = UpdatePlatform.WINDOWS,
                    )

                assertFailsWith<IllegalStateException> {
                    repo.downloadAsset(asset, tempFile.absolutePath).toList()
                }

                assertFalse(tempFile.exists())
                assertFalse(File("${tempFile.absolutePath}.tmp").exists())
            } finally {
                tempFile.delete()
                File("${tempFile.absolutePath}.tmp").delete()
            }
        }
}
