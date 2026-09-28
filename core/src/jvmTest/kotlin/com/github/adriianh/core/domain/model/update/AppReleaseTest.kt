package com.github.adriianh.core.domain.model.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppReleaseTest {
    @Test
    fun `windows prioritizes setup exe over msi and zip`() {
        val release =
            AppRelease(
                version = "2.2.1",
                tagName = "v2.2.1",
                name = "Melo 2.2.1",
                releaseNotes = "Notes",
                htmlUrl = "https://github.com/Adriianh/Melo/releases/tag/v2.2.1",
                publishedAt = "2026-09-27T00:00:00Z",
                assets =
                    listOf(
                        ReleaseAsset("melo-2.2.1-windows.zip", "https://example.com/win.zip", 20_000_000L, UpdatePlatform.WINDOWS),
                        ReleaseAsset("Melo-2.2.1.msi", "https://example.com/melo.msi", 70_000_000L, UpdatePlatform.WINDOWS),
                        ReleaseAsset("Melo-Setup.exe", "https://example.com/setup.exe", 75_000_000L, UpdatePlatform.WINDOWS),
                    ),
            )

        val selected = release.getAssetForPlatform(UpdatePlatform.WINDOWS)
        assertNotNull(selected)
        assertEquals("Melo-Setup.exe", selected.name)
    }

    @Test
    fun `windows selects msi when no exe is present`() {
        val release =
            AppRelease(
                version = "2.2.1",
                tagName = "v2.2.1",
                name = "Melo 2.2.1",
                releaseNotes = "Notes",
                htmlUrl = "https://example.com",
                publishedAt = "2026-09-27T00:00:00Z",
                assets =
                    listOf(
                        ReleaseAsset("melo-2.2.1-windows.zip", "https://example.com/win.zip", 20_000_000L, UpdatePlatform.WINDOWS),
                        ReleaseAsset("Melo-2.2.1.msi", "https://example.com/melo.msi", 70_000_000L, UpdatePlatform.WINDOWS),
                    ),
            )

        val selected = release.getAssetForPlatform(UpdatePlatform.WINDOWS)
        assertNotNull(selected)
        assertEquals("Melo-2.2.1.msi", selected.name)
    }

    @Test
    fun `linux prioritizes appimage over deb and tar gz`() {
        val release =
            AppRelease(
                version = "2.2.1",
                tagName = "v2.2.1",
                name = "Melo 2.2.1",
                releaseNotes = "Notes",
                htmlUrl = "https://example.com",
                publishedAt = "2026-09-27T00:00:00Z",
                assets =
                    listOf(
                        ReleaseAsset("melo-2.2.1-linux.tar.gz", "https://example.com/tui.tar.gz", 15_000_000L, UpdatePlatform.LINUX),
                        ReleaseAsset("melo_2.2.1_amd64.deb", "https://example.com/melo.deb", 65_000_000L, UpdatePlatform.LINUX),
                        ReleaseAsset("Melo-2.2.1-x86_64.AppImage", "https://example.com/melo.AppImage", 80_000_000L, UpdatePlatform.LINUX),
                    ),
            )

        val selected = release.getAssetForPlatform(UpdatePlatform.LINUX)
        assertNotNull(selected)
        assertEquals("Melo-2.2.1-x86_64.AppImage", selected.name)
    }

    @Test
    fun `macos prioritizes dmg over tar gz and zip`() {
        val release =
            AppRelease(
                version = "2.2.1",
                tagName = "v2.2.1",
                name = "Melo 2.2.1",
                releaseNotes = "Notes",
                htmlUrl = "https://example.com",
                publishedAt = "2026-09-27T00:00:00Z",
                assets =
                    listOf(
                        ReleaseAsset("melo-2.2.1-macos.tar.gz", "https://example.com/tui.tar.gz", 15_000_000L, UpdatePlatform.MACOS),
                        ReleaseAsset("Melo-2.2.1.dmg", "https://example.com/melo.dmg", 70_000_000L, UpdatePlatform.MACOS),
                    ),
            )

        val selected = release.getAssetForPlatform(UpdatePlatform.MACOS)
        assertNotNull(selected)
        assertEquals("Melo-2.2.1.dmg", selected.name)
    }

    @Test
    fun `returns null when platform has no matching asset`() {
        val release =
            AppRelease(
                version = "2.2.1",
                tagName = "v2.2.1",
                name = "Melo 2.2.1",
                releaseNotes = "Notes",
                htmlUrl = "https://example.com",
                publishedAt = "2026-09-27T00:00:00Z",
                assets =
                    listOf(
                        ReleaseAsset("Melo-Setup.exe", "https://example.com/setup.exe", 75_000_000L, UpdatePlatform.WINDOWS),
                    ),
            )

        assertNull(release.getAssetForPlatform(UpdatePlatform.LINUX))
        assertNull(release.getAssetForPlatform(UpdatePlatform.MACOS))
    }
}
