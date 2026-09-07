package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.update.UpdatePlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `checkForUpdate returns release when newer version exists`() = runTest {
        val mockJson = """
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

        val mockEngine = MockEngine { _ ->
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
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
    fun `checkForUpdate returns null when already on latest version`() = runTest {
        val mockJson = """
            {
                "tag_name": "v1.0.1",
                "name": "Melo 1.0.1",
                "body": "Initial release",
                "html_url": "https://github.com/Adriianh/Melo/releases/tag/v1.0.1",
                "published_at": "2026-09-06T00:00:00Z",
                "assets": []
            }
        """.trimIndent()

        val mockEngine = MockEngine { _ ->
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val repo = UpdateRepositoryImpl(client, Dispatchers.Unconfined)
        val result = repo.checkForUpdate("1.0.1")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
}