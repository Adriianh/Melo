package com.github.adriianh.core.domain.model.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateStateResolutionTest {
    @Test
    fun `appimage install gets the appimage`() {
        val release = releaseOf(APPIMAGE, GUI_TARBALL, DEB)

        val state = resolveUpdateState(release, UpdatePlatform.LINUX, UpdateInstallMethod.APPIMAGE, CURRENT)

        assertIs<UpdateState.UpdateAvailable>(state)
        assertEquals(APPIMAGE.name, state.targetAsset?.name)
        assertEquals(CURRENT, state.currentVersion)
    }

    @Test
    fun `portable install gets the desktop archive, never the tui one`() {
        val release = releaseOf(APPIMAGE, TUI_TARBALL, GUI_TARBALL)

        val state = resolveUpdateState(release, UpdatePlatform.LINUX, UpdateInstallMethod.TARBALL, CURRENT)

        assertIs<UpdateState.UpdateAvailable>(state)
        assertEquals(GUI_TARBALL.name, state.targetAsset?.name)
    }

    @Test
    fun `portable install without a matching archive is a manual update`() {
        val release = releaseOf(APPIMAGE, TUI_TARBALL)

        val state = resolveUpdateState(release, UpdatePlatform.LINUX, UpdateInstallMethod.TARBALL, CURRENT)

        assertIs<UpdateState.ManualUpdate>(state)
        assertEquals(UpdateInstallMethod.TARBALL, state.method)
        assertEquals(release, state.release)
    }

    @Test
    fun `package managed install is always a manual update`() {
        val release = releaseOf(APPIMAGE, GUI_TARBALL)

        val state = resolveUpdateState(release, UpdatePlatform.LINUX, UpdateInstallMethod.PACKAGE_MANAGER, CURRENT)

        assertIs<UpdateState.ManualUpdate>(state)
        assertEquals(UpdateInstallMethod.PACKAGE_MANAGER, state.method)
    }

    @Test
    fun `unknown install method keeps the legacy asset priority`() {
        val release = releaseOf(TUI_TARBALL, DEB, APPIMAGE)

        val state = resolveUpdateState(release, UpdatePlatform.LINUX, UpdateInstallMethod.UNKNOWN, CURRENT)

        assertIs<UpdateState.UpdateAvailable>(state)
        assertEquals(APPIMAGE.name, state.targetAsset?.name)
    }

    @Test
    fun `release without an asset for the platform is a manual update`() {
        val release = releaseOf(GUI_TARBALL)

        val state = resolveUpdateState(release, UpdatePlatform.ANDROID, UpdateInstallMethod.APK, CURRENT)

        assertIs<UpdateState.ManualUpdate>(state)
        assertEquals(UpdateInstallMethod.APK, state.method)
    }

    private fun releaseOf(vararg assets: ReleaseAsset): AppRelease =
        AppRelease(
            version = CURRENT,
            tagName = "v$CURRENT",
            name = "Melo $CURRENT",
            releaseNotes = "Notes",
            htmlUrl = "https://example.com/melo",
            publishedAt = "2026-09-27T00:00:00Z",
            assets = assets.toList(),
        )

    private companion object {
        const val CURRENT = "2.2.1"
        val APPIMAGE = asset("Melo-2.2.1-x86_64.AppImage")
        val DEB = asset("melo_2.2.1_amd64.deb")
        val GUI_TARBALL = asset("melo-2.2.1-linux-x64.tar.gz")
        val TUI_TARBALL = asset("melo-2.2.1-linux.tar.gz")

        fun asset(name: String): ReleaseAsset = ReleaseAsset(name, "https://example.com/$name", 1_000L, UpdatePlatform.LINUX)
    }
}
