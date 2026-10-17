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

    @Test
    fun `tui asset selection picks native archives correctly`() {
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
                        ReleaseAsset("Melo-2.2.1.msi", "https://example.com/melo.msi", 70_000_000L, UpdatePlatform.WINDOWS),
                        ReleaseAsset("melo-2.2.1-windows.zip", "https://example.com/win.zip", 20_000_000L, UpdatePlatform.WINDOWS),
                        ReleaseAsset("Melo-2.2.1-x86_64.AppImage", "https://example.com/appimage", 80_000_000L, UpdatePlatform.LINUX),
                        ReleaseAsset("melo_2.2.1_amd64.deb", "https://example.com/deb", 65_000_000L, UpdatePlatform.LINUX),
                        ReleaseAsset("melo-2.2.1-linux-x64.tar.gz", "https://example.com/compose-tar", 160_000_000L, UpdatePlatform.LINUX),
                        ReleaseAsset("melo-2.2.1-linux.tar.gz", "https://example.com/tui-linux", 15_000_000L, UpdatePlatform.LINUX),
                        ReleaseAsset("Melo-2.2.1.dmg", "https://example.com/dmg", 70_000_000L, UpdatePlatform.MACOS),
                        ReleaseAsset("melo-2.2.1-macos.tar.gz", "https://example.com/tui-mac", 15_000_000L, UpdatePlatform.MACOS),
                    ),
            )

        val winTui = release.getTuiAssetForPlatform(UpdatePlatform.WINDOWS)
        assertNotNull(winTui)
        assertEquals("melo-2.2.1-windows.zip", winTui.name)

        val linuxTui = release.getTuiAssetForPlatform(UpdatePlatform.LINUX)
        assertNotNull(linuxTui)
        assertEquals("melo-2.2.1-linux.tar.gz", linuxTui.name)

        val macTui = release.getTuiAssetForPlatform(UpdatePlatform.MACOS)
        assertNotNull(macTui)
        assertEquals("melo-2.2.1-macos.tar.gz", macTui.name)
    }

    @Test
    fun `tui asset selection returns null when no tui archive exists`() {
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
                        ReleaseAsset("Melo-2.2.1.deb", "https://example.com/deb", 65_000_000L, UpdatePlatform.LINUX),
                        ReleaseAsset("Melo-2.2.1.dmg", "https://example.com/dmg", 70_000_000L, UpdatePlatform.MACOS),
                    ),
            )

        assertNull(release.getTuiAssetForPlatform(UpdatePlatform.WINDOWS))
        assertNull(release.getTuiAssetForPlatform(UpdatePlatform.LINUX))
        assertNull(release.getTuiAssetForPlatform(UpdatePlatform.MACOS))
    }

    @Test
    fun `currentPlatform returns non-unknown platform on host JVM`() {
        val platform = currentPlatform()
        assertNotNull(platform)
    }

    @Test
    fun `appimage install selects the appimage`() {
        val release = releaseOf(APPIMAGE, DEB, GUI_TARBALL, TUI_TARBALL)

        val selected = release.getAssetForPlatform(UpdatePlatform.LINUX, UpdateInstallMethod.APPIMAGE)

        assertNotNull(selected)
        assertEquals(APPIMAGE.name, selected.name)
    }

    @Test
    fun `tarball install selects the desktop archive and never the tui one`() {
        val release = releaseOf(APPIMAGE, DEB, GUI_TARBALL, TUI_TARBALL)

        val selected = release.getAssetForPlatform(UpdatePlatform.LINUX, UpdateInstallMethod.TARBALL)

        assertNotNull(selected)
        assertEquals(GUI_TARBALL.name, selected.name)
    }

    @Test
    fun `tarball install returns null when the release only ships the tui archive`() {
        val release = releaseOf(APPIMAGE, TUI_TARBALL)

        assertNull(release.getAssetForPlatform(UpdatePlatform.LINUX, UpdateInstallMethod.TARBALL))
    }

    @Test
    fun `unknown install method keeps the legacy linux priority`() {
        val release = releaseOf(TUI_TARBALL, DEB, APPIMAGE)

        val selected = release.getAssetForPlatform(UpdatePlatform.LINUX, UpdateInstallMethod.UNKNOWN)

        assertNotNull(selected)
        assertEquals(APPIMAGE.name, selected.name)
    }

    @Test
    fun `apk install still selects the apk`() {
        val release = releaseOf(ReleaseAsset("composeApp-release.apk", "https://example.com/melo.apk", 50_000_000L, UpdatePlatform.ANDROID))

        val selected = release.getAssetForPlatform(UpdatePlatform.ANDROID, UpdateInstallMethod.APK)

        assertNotNull(selected)
        assertEquals("composeApp-release.apk", selected.name)
    }

    private fun releaseOf(vararg assets: ReleaseAsset): AppRelease =
        AppRelease(
            version = "2.2.1",
            tagName = "v2.2.1",
            name = "Melo 2.2.1",
            releaseNotes = "Notes",
            htmlUrl = "https://example.com",
            publishedAt = "2026-09-27T00:00:00Z",
            assets = assets.toList(),
        )

    private companion object {
        val APPIMAGE = asset("Melo-2.2.1-x86_64.AppImage")
        val DEB = asset("melo_2.2.1_amd64.deb")
        val GUI_TARBALL = asset("melo-2.2.1-linux-x64.tar.gz")
        val TUI_TARBALL = asset("melo-2.2.1-linux.tar.gz")

        fun asset(name: String): ReleaseAsset = ReleaseAsset(name, "https://example.com/$name", 1_000L, UpdatePlatform.LINUX)
    }
}
