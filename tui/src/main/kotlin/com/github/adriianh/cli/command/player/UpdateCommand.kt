package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.service.SelfUpdater
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.currentPlatform
import com.github.adriianh.core.domain.repository.DownloadProgress
import com.github.adriianh.core.domain.usecase.update.CheckForUpdateUseCase
import com.github.adriianh.core.domain.usecase.update.DownloadUpdateUseCase
import com.github.adriianh.core.util.MeloVersion
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.speed
import com.github.ajalt.mordant.widgets.progress.text
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import java.nio.file.Files

private inline fun <T> withKoin(block: () -> T): T {
    val startedHere = GlobalContext.getOrNull() == null
    if (startedHere) {
        startKoin { modules(appModule) }
    }
    return try {
        block()
    } finally {
        if (startedHere) {
            stopKoin()
        }
    }
}

class UpdateCommand(
    private val checkForUpdateUseCase: CheckForUpdateUseCase? = null,
    private val downloadUpdateUseCase: DownloadUpdateUseCase? = null,
    private val selfUpdater: SelfUpdater? = null,
    private val currentVersion: String = MeloVersion.CURRENT,
    private val terminalOverride: Terminal? = null,
) : CliktCommand(name = "update"),
    KoinComponent {
    private val terminal: Terminal get() = terminalOverride ?: currentContext.terminal

    override fun help(context: Context): String = "Check for and install updates to Melo."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo update\n" +
            "  melo update --check\n" +
            "  melo update -y\n" +
            "  melo update --force\n" +
            "  melo upgrade"

    val checkOnly by option(
        "--check",
        help = "Check for updates without downloading or installing.",
    ).flag(default = false)

    val yes by option(
        "-y",
        "--yes",
        help = "Automatically answer yes to confirmation prompts.",
    ).flag(default = false)

    val force by option(
        "--force",
        help = "Force download and reinstall even if up to date.",
    ).flag(default = false)

    override fun run() {
        val needsKoin = checkForUpdateUseCase == null || downloadUpdateUseCase == null || selfUpdater == null
        if (needsKoin) {
            withKoin { executeCommand() }
        } else {
            executeCommand()
        }
    }

    private fun executeCommand() {
        val actualCheckUseCase = checkForUpdateUseCase ?: inject<CheckForUpdateUseCase>().value
        val actualDownloadUseCase = downloadUpdateUseCase ?: inject<DownloadUpdateUseCase>().value
        val actualUpdater = selfUpdater ?: inject<SelfUpdater>().value

        terminal.println(cyan("Checking for updates..."))

        runBlocking {
            val versionToCheck = if (force) "0.0.0" else currentVersion
            val result = actualCheckUseCase(versionToCheck)

            result.fold(
                onSuccess = { release ->
                    handleRelease(release, actualUpdater, actualDownloadUseCase)
                },
                onFailure = { error ->
                    terminal.println(red("Failed to check for updates: ${error.message}"))
                },
            )
        }
    }

    private suspend fun handleRelease(
        release: AppRelease?,
        actualUpdater: SelfUpdater,
        actualDownloadUseCase: DownloadUpdateUseCase,
    ) {
        if (release == null) {
            terminal.println(green("Melo is up to date (v$currentVersion)."))
            return
        }

        val platform = currentPlatform()
        val targetAsset = release.getTuiAssetForPlatform(platform)
        if (targetAsset == null) {
            val msg = "Update v${release.version} is available, but no pre-built TUI archive exists for $platform."
            terminal.println(yellow(msg))
            terminal.println("Please visit ${release.htmlUrl} to download manually.")
        } else {
            proceedWithAsset(release, targetAsset, actualUpdater, actualDownloadUseCase)
        }
    }

    private suspend fun proceedWithAsset(
        release: AppRelease,
        targetAsset: ReleaseAsset,
        actualUpdater: SelfUpdater,
        actualDownloadUseCase: DownloadUpdateUseCase,
    ) {
        displayReleaseInfo(release, targetAsset)

        if (checkOnly) {
            terminal.println()
            terminal.println(cyan("Run 'melo update' to install this update."))
        } else if (confirmInstallation()) {
            val targetExec = actualUpdater.getCurrentExecutable() ?: actualUpdater.getDefaultExecutable()
            terminal.println(cyan("Target installation path: ${targetExec.absolutePath}"))
            downloadAndApplyUpdate(release, targetAsset, targetExec, actualUpdater, actualDownloadUseCase)
        }
    }

    private fun displayReleaseInfo(
        release: AppRelease,
        targetAsset: ReleaseAsset,
    ) {
        terminal.println(
            bold("Update available: ") + gray("v$currentVersion") + " → " + green("v${release.version}"),
        )

        if (release.releaseNotes.isNotBlank()) {
            terminal.println()
            terminal.println(bold("Release Notes:"))
            terminal.println(Markdown(release.releaseNotes.trim()))
            terminal.println()
        }

        val sizeMb = "%.1f MB".format(targetAsset.sizeBytes.toDouble() / (1024 * 1024))
        terminal.println("Package: ${targetAsset.name} ($sizeMb)")
    }

    private fun confirmInstallation(): Boolean {
        if (yes) return true
        terminal.print("Do you want to download and install this update? [Y/n]: ")
        val input = readlnOrNull()?.trim()?.lowercase()
        val confirmed = input.isNullOrEmpty() || input == "y" || input == "yes"
        if (!confirmed) {
            terminal.println(yellow("Update cancelled."))
        }
        return confirmed
    }

    private suspend fun downloadAndApplyUpdate(
        release: AppRelease,
        targetAsset: ReleaseAsset,
        targetExec: File,
        actualUpdater: SelfUpdater,
        actualDownloadUseCase: DownloadUpdateUseCase,
    ) {
        val tempDownloadDir = Files.createTempDirectory("melo-update-download-").toFile()
        val tempExtractDir = Files.createTempDirectory("melo-update-extract-").toFile()

        try {
            val downloadedArchive = File(tempDownloadDir, targetAsset.name)
            downloadAssetWithProgress(targetAsset, downloadedArchive, actualDownloadUseCase)

            terminal.println(green("Download complete."))
            terminal.println(cyan("Extracting archive..."))
            actualUpdater.extractArchive(downloadedArchive, tempExtractDir)

            terminal.println(cyan("Applying update..."))
            actualUpdater.applyUpdate(tempExtractDir, targetExec)

            terminal.println()
            terminal.println(bold(green("Successfully updated Melo to v${release.version}!")))
            terminal.println("Run 'melo --version' or 'melo' to use the new version.")
        } finally {
            tempDownloadDir.deleteRecursively()
            tempExtractDir.deleteRecursively()
        }
    }

    private suspend fun downloadAssetWithProgress(
        targetAsset: ReleaseAsset,
        downloadedArchive: File,
        actualDownloadUseCase: DownloadUpdateUseCase,
    ) {
        terminal.println(cyan("Downloading ${targetAsset.name}..."))

        val layout =
            progressBarLayout {
                text(targetAsset.name)
                percentage()
                progressBar()
                completed()
                speed()
                timeRemaining()
            }
        val totalBytes = if (targetAsset.sizeBytes > 0) targetAsset.sizeBytes else null
        val progressLayout = layout.animateOnThread(terminal, total = totalBytes)

        val progressScope = CoroutineScope(Dispatchers.Default)
        val progressJob = progressScope.launch { progressLayout.execute() }

        var lastBytesWritten = 0L
        try {
            actualDownloadUseCase(targetAsset, downloadedArchive.absolutePath).collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        val delta = progress.bytesDownloaded - lastBytesWritten
                        if (delta > 0) {
                            progressLayout.advance(delta)
                            lastBytesWritten = progress.bytesDownloaded
                        }
                    }
                    is DownloadProgress.Completed -> {
                        val remaining = targetAsset.sizeBytes - lastBytesWritten
                        if (remaining > 0) {
                            progressLayout.advance(remaining)
                        }
                    }
                }
            }
        } finally {
            progressJob.cancel()
        }
    }
}
