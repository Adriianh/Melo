package com.github.adriianh.cli.command

import com.github.adriianh.cli.command.player.UpdateCommand
import com.github.adriianh.cli.service.SelfUpdater
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdatePlatform
import com.github.adriianh.core.domain.model.update.currentPlatform
import com.github.adriianh.core.domain.repository.DownloadProgress
import com.github.adriianh.core.domain.repository.UpdateRepository
import com.github.adriianh.core.domain.usecase.update.CheckForUpdateUseCase
import com.github.adriianh.core.domain.usecase.update.DownloadUpdateUseCase
import com.github.ajalt.clikt.testing.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateCommandTest {
    private class FakeUpdateRepository(
        var releaseToReturn: AppRelease? = null,
        var shouldThrow: Boolean = false,
        var downloadedBytesEmitted: Long = 1024L,
    ) : UpdateRepository {
        var requestedVersion: String? = null
        var requestedChannel: UpdateChannel? = null
        var downloadCalledWithAsset: ReleaseAsset? = null

        override suspend fun checkForUpdate(
            currentVersion: String,
            channel: UpdateChannel,
        ): Result<AppRelease?> {
            requestedVersion = currentVersion
            requestedChannel = channel
            if (shouldThrow) {
                return Result.failure(IllegalStateException("Simulated network timeout"))
            }
            return Result.success(releaseToReturn)
        }

        override fun downloadAsset(
            asset: ReleaseAsset,
            destinationFilePath: String,
        ): Flow<DownloadProgress> =
            flow {
                downloadCalledWithAsset = asset
                emit(DownloadProgress.Progress(downloadedBytesEmitted, asset.sizeBytes))
                emit(DownloadProgress.Completed(destinationFilePath))
            }
    }

    private class FakeSelfUpdater(
        val targetExec: File = File("/tmp/mock-melo/melo"),
    ) : SelfUpdater {
        var extractCalled = false
        var applyUpdateCalled = false
        var cleanedBackups = false

        override fun getCurrentExecutable(): File? = targetExec

        override fun getDefaultExecutable(): File = targetExec

        override fun extractArchive(
            archiveFile: File,
            destinationDir: File,
        ) {
            extractCalled = true
        }

        override fun findBinaryInExtracted(extractedDir: File): File = targetExec

        override fun applyUpdate(
            extractedDir: File,
            targetExecutable: File,
        ) {
            applyUpdateCalled = true
        }

        override fun cleanupOldBackups(installDir: File) {
            cleanedBackups = true
        }
    }

    private fun sampleRelease(
        version: String = "2.3.0",
        notes: String = "### New Features\n* Added self-updater",
    ): AppRelease {
        val platform = currentPlatform()
        val assetName =
            when (platform) {
                UpdatePlatform.WINDOWS -> "melo-$version-windows.zip"
                UpdatePlatform.MACOS -> "melo-$version-macos.tar.gz"
                else -> "melo-$version-linux.tar.gz"
            }
        return AppRelease(
            version = version,
            tagName = "v$version",
            name = "Melo $version",
            releaseNotes = notes,
            htmlUrl = "https://github.com/Adriianh/Melo/releases/tag/v$version",
            publishedAt = "2026-10-05T00:00:00Z",
            assets =
                listOf(
                    ReleaseAsset(
                        name = assetName,
                        downloadUrl = "https://example.com/$assetName",
                        sizeBytes = 25_000_000L,
                        platform = platform,
                    ),
                ),
        )
    }

    @Test
    fun `help displays flags and examples`() {
        val cmd = UpdateCommand()
        val result = cmd.test("--help")

        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("Check for and install updates to Melo."))
        assertTrue(result.output.contains("--check"))
        assertTrue(result.output.contains("-y, --yes"))
        assertTrue(result.output.contains("--force"))
        assertTrue(result.output.contains("--nightly"))
        assertTrue(result.output.contains("--stable"))
        assertTrue(result.output.contains("melo update --check"))
        assertTrue(result.output.contains("melo upgrade"))
    }

    @Test
    fun `resolves nightly channel with nightly flag`() {
        val repo = FakeUpdateRepository(releaseToReturn = null)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val cmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1",
            )

        val result = cmd.test("--nightly")
        assertEquals(0, result.statusCode)
        assertEquals(UpdateChannel.NIGHTLY, repo.requestedChannel)
    }

    @Test
    fun `resolves stable channel with stable flag`() {
        val repo = FakeUpdateRepository(releaseToReturn = null)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val cmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1-nightly.20261005.0915",
            )

        val result = cmd.test("--stable")
        assertEquals(0, result.statusCode)
        assertEquals(UpdateChannel.STABLE, repo.requestedChannel)
    }

    @Test
    fun `auto-detects channel based on current version`() {
        val repo = FakeUpdateRepository(releaseToReturn = null)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val nightlyCmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1-nightly.20261005.0915",
            )
        nightlyCmd.test()
        assertEquals(UpdateChannel.NIGHTLY, repo.requestedChannel)

        val stableCmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1",
            )
        stableCmd.test()
        assertEquals(UpdateChannel.STABLE, repo.requestedChannel)
    }

    @Test
    fun `displays up to date when no newer version is found`() {
        val repo = FakeUpdateRepository(releaseToReturn = null)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val cmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1",
            )

        val result = cmd.test()
        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("Melo is up to date (v2.2.1, channel: stable)."))
        assertEquals("2.2.1", repo.requestedVersion)
        assertEquals(false, selfUpdater.applyUpdateCalled)
    }

    @Test
    fun `check flag displays update diff and release notes without downloading`() {
        val release = sampleRelease("2.3.0")
        val repo = FakeUpdateRepository(releaseToReturn = release)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val cmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1",
            )

        val result = cmd.test("--check")
        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("Update available: v2.2.1 → v2.3.0"))
        assertTrue(result.output.contains("Release Notes:"))
        assertTrue(result.output.contains("Added self-updater"))
        assertTrue(result.output.contains("Run 'melo update' to install this update."))
        assertEquals(false, selfUpdater.applyUpdateCalled)
        assertEquals(null, repo.downloadCalledWithAsset)
    }

    @Test
    fun `force flag checks against zero version`() {
        val release = sampleRelease("2.2.1")
        val repo = FakeUpdateRepository(releaseToReturn = release)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val cmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1",
            )

        val result = cmd.test("--force", "--check")
        assertEquals(0, result.statusCode)
        assertEquals("0.0.0", repo.requestedVersion)
    }

    @Test
    fun `yes flag executes full download and update pipeline`() {
        val release = sampleRelease("2.3.0")
        val repo = FakeUpdateRepository(releaseToReturn = release)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val cmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1",
            )

        val result = cmd.test("-y")
        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("Downloading"))
        assertTrue(result.output.contains("Download complete."))
        assertTrue(result.output.contains("Applying update..."))
        assertTrue(result.output.contains("Successfully updated Melo to v2.3.0!"))
        assertTrue(selfUpdater.extractCalled)
        assertTrue(selfUpdater.applyUpdateCalled)
    }

    @Test
    fun `displays error message when check fails`() {
        val repo = FakeUpdateRepository(shouldThrow = true)
        val checkUseCase = CheckForUpdateUseCase(repo)
        val downloadUseCase = DownloadUpdateUseCase(repo)
        val selfUpdater = FakeSelfUpdater()

        val cmd =
            UpdateCommand(
                checkForUpdateUseCase = checkUseCase,
                downloadUpdateUseCase = downloadUseCase,
                selfUpdater = selfUpdater,
                currentVersion = "2.2.1",
            )

        val result = cmd.test()
        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("Failed to check for updates: Simulated network timeout"))
        assertEquals(false, selfUpdater.applyUpdateCalled)
    }
}
