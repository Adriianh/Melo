package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdatePlatform
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.domain.usecase.update.CheckForUpdateUseCase
import com.github.adriianh.core.domain.usecase.update.DownloadUpdateUseCase
import com.github.adriianh.melo.ui.settings.UpdateViewModel
import com.github.adriianh.melo.util.PlatformUpdateInstaller
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val checkForUpdateUseCase = mockk<CheckForUpdateUseCase>()
    private val downloadUpdateUseCase = mockk<DownloadUpdateUseCase>()
    private val installer = mockk<PlatformUpdateInstaller>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        UpdateViewModel(
            checkForUpdateUseCase = checkForUpdateUseCase,
            downloadUpdateUseCase = downloadUpdateUseCase,
            installer = installer,
        )

    private fun fakeRelease(version: String) =
        AppRelease(
            version = version,
            tagName = "v$version",
            name = "Release $version",
            releaseNotes = "Notes",
            htmlUrl = "https://github.com/Adriianh/Melo/releases/tag/v$version",
            publishedAt = "2026-09-27T00:00:00Z",
            assets =
                listOf(
                    ReleaseAsset(
                        name = "melo-release.apk",
                        downloadUrl = "https://example.com/app.apk",
                        sizeBytes = 25_000_000L,
                        platform = UpdatePlatform.ANDROID,
                    ),
                ),
        )

    @Test
    fun `checkForUpdates transitions to UpdateAvailable when newer release is found`() =
        runTest {
            val release = fakeRelease("9.9.9")
            coEvery { checkForUpdateUseCase(any(), any()) } returns Result.success(release)

            val viewModel = createViewModel()
            viewModel.checkForUpdates(UpdateChannel.STABLE)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertIs<UpdateState.UpdateAvailable>(state)
            assertEquals("9.9.9", state.release.version)
        }

    @Test
    fun `checkForUpdates with NIGHTLY channel passes channel to usecase`() =
        runTest {
            val nightlyRelease =
                fakeRelease("2.3.0-nightly.20261008.0100").copy(channel = UpdateChannel.NIGHTLY)
            coEvery { checkForUpdateUseCase(any(), UpdateChannel.NIGHTLY) } returns Result.success(nightlyRelease)

            val viewModel = createViewModel()
            viewModel.checkForUpdates(UpdateChannel.NIGHTLY)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertIs<UpdateState.UpdateAvailable>(state)
            assertEquals("2.3.0-nightly.20261008.0100", state.release.version)
            assertEquals(UpdateChannel.NIGHTLY, state.release.channel)
        }

    @Test
    fun `checkForUpdates transitions to UpToDate when no newer release`() =
        runTest {
            coEvery { checkForUpdateUseCase(any(), any()) } returns Result.success(null)

            val viewModel = createViewModel()
            viewModel.checkForUpdates(UpdateChannel.STABLE)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertIs<UpdateState.UpToDate>(state)
            assertEquals(UpdateChannel.STABLE, state.channel)
        }

    @Test
    fun `cancelDownload resets state to Idle`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.cancelDownload()

            assertIs<UpdateState.Idle>(viewModel.uiState.value)
        }

    @Test
    fun `installUpdate delegates to installer`() {
        every { installer.installAndRestart(any()) } just runs
        val viewModel = createViewModel()

        viewModel.installUpdate("/path/to/installer.apk")

        verify { installer.installAndRestart("/path/to/installer.apk") }
    }
}
