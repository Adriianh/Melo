package com.github.adriianh.cli.command

import com.github.adriianh.cli.command.config.ConfigCommand
import com.github.adriianh.cli.command.player.DownloadCommand
import com.github.adriianh.cli.command.player.LyricsCommand
import com.github.adriianh.cli.command.player.NextCommand
import com.github.adriianh.cli.command.player.PauseCommand
import com.github.adriianh.cli.command.player.PlayCommand
import com.github.adriianh.cli.command.player.PrevCommand
import com.github.adriianh.cli.command.player.QueueCommand
import com.github.adriianh.cli.command.player.ResumeCommand
import com.github.adriianh.cli.command.player.SearchCommand
import com.github.adriianh.cli.command.player.StatusCommand
import com.github.adriianh.cli.command.player.StopCommand
import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.interactor.LibraryInteractors
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.interactor.PlaybackInteractors
import com.github.adriianh.core.domain.interactor.SearchInteractors
import com.github.adriianh.core.domain.interactor.SessionInteractors
import com.github.adriianh.core.domain.interactor.SettingsInteractors
import com.github.adriianh.core.domain.interactor.StatsInteractors
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class MeloCommand : CliktCommand(
    name = "melo"
), KoinComponent {
    init {
        subcommands(
            ConfigCommand(),
            SearchCommand(),
            PlayCommand(),
            DownloadCommand(),
            StatusCommand(),
            LyricsCommand(),
            PauseCommand(),
            ResumeCommand(),
            NextCommand(),
            PrevCommand(),
            StopCommand(),
            QueueCommand(),
        )
    }

    override val invokeWithoutSubcommand: Boolean = true

    override fun help(context: Context): String = Messages.get("help.melo_command")

    override fun run() {
        if (currentContext.invokedSubcommand != null) return

        startKoin { modules(appModule) }

        val searchInteractors: SearchInteractors by inject()
        val libraryInteractors: LibraryInteractors by inject()
        val playbackInteractors: PlaybackInteractors by inject()
        val offlineInteractors: OfflineInteractors by inject()
        val statsInteractors: StatsInteractors by inject()
        val sessionInteractors: SessionInteractors by inject()
        val settingsInteractors: SettingsInteractors by inject()
        val artworkRenderer: ArtworkRenderer by inject()
        val metadataProvider: MetadataProvider by inject()
        val pipedApiClient: PipedApiClient by inject()
        val offlineRepository: OfflineRepository by inject()
        val httpClient: HttpClient by inject()
        val dispatcher: CoroutineDispatcher by inject()
        val audioProvider: AudioProvider by inject()
        val discordRpcManager: DiscordRpcManager by inject()

        try {
            MeloScreen(
                httpClient = httpClient,
                pipedApiClient = pipedApiClient,
                searchInteractors = searchInteractors,
                libraryInteractors = libraryInteractors,
                playbackInteractors = playbackInteractors,
                offlineInteractors = offlineInteractors,
                statsInteractors = statsInteractors,
                sessionInteractors = sessionInteractors,
                settingsInteractors = settingsInteractors,
                offlineRepository = offlineRepository,
                artworkRenderer = artworkRenderer,
                metadataProvider = metadataProvider,
                audioProvider = audioProvider,
                discordRpcManager = discordRpcManager,
                dispatcher = dispatcher
            ).run()
        } finally {
            stopKoin()
        }
    }
}
