package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.DownloadActionHandler
import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class DownloadCommand : CliktCommand(
    name = "download"
), KoinComponent {
    private val query by argument(name = "query", help = "Track name to download")
    private val terminal = Terminal()
    override fun help(context: Context): String =
        "Download the best matching track for the given query directly"

    override fun run() {
        if (resolveEnv("LASTFM_API_KEY") == null) {
            echo(Messages.get("error.missing_lastfm_key", "configDir" to configDir), err = true)
            exitProcess(1)
        }

        startKoin { modules(appModule) }

        try {
            val searchTracks: SearchTracksUseCase by inject()
            val getStream: GetStreamUseCase by inject()
            val getSettings: GetSettingsUseCase by inject()
            val downloadTrack: DownloadTrackUseCase by inject()
            runBlocking {
                terminal.println(gray("Searching for '${query}'..."))
                val track = searchTracks(query).firstOrNull()
                if (track == null) {
                    terminal.println("No results found for '${query}'.")
                    return@runBlocking
                }
                DownloadActionHandler.downloadTrack(
                    track,
                    getStream,
                    getSettings,
                    downloadTrack,
                    terminal
                )
            }
        } finally {
            stopKoin()
        }
    }
}
