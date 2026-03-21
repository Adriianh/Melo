package com.github.adriianh.cli.command

import com.github.adriianh.cli.command.config.ConfigCommand
import com.github.adriianh.cli.command.player.SearchCommand
import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.interactor.*
import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import io.ktor.client.*
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class MeloCommand : CliktCommand(
    name = "melo"
), KoinComponent {
    init {
        subcommands(
            SearchCommand(),
            ConfigCommand(),
        )
    }

    override val invokeWithoutSubcommand: Boolean = true

    override fun help(context: Context): String = Messages.get("help.melo_command")

    override fun run() {
        if (currentContext.invokedSubcommand != null) return

        if (resolveEnv("LASTFM_API_KEY") == null) {
            echo(Messages.get("error.missing_lastfm_key", "configDir" to configDir), err = true)
            exitProcess(1)
        }

        startKoin { modules(appModule) }

        val searchInteractors: SearchInteractors by inject()
        val libraryInteractors: LibraryInteractors by inject()
        val playbackInteractors: PlaybackInteractors by inject()
        val offlineInteractors: OfflineInteractors by inject()
        val statsInteractors: StatsInteractors by inject()
        val sessionInteractors: SessionInteractors by inject()
        val settingsInteractors: SettingsInteractors by inject()
        val artworkRenderer: ArtworkRenderer by inject()
        val artworkProvider: ArtworkProvider by inject()
        val pipedApiClient: PipedApiClient by inject()
        val offlineRepository: OfflineRepository by inject()
        val httpClient: HttpClient by inject()
        val dispatcher: CoroutineDispatcher by inject()
        val audioProvider: AudioProvider by inject()

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
                artworkProvider = artworkProvider,
                audioProvider = audioProvider,
                dispatcher = dispatcher
            ).run()
        } finally {
            stopKoin()
        }
    }
}
