package com.github.adriianh.cli.command

import com.github.adriianh.cli.command.config.ConfigCommand
import com.github.adriianh.cli.command.player.SearchCommand
import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.usecase.AddFavoriteUseCase
import com.github.adriianh.core.domain.usecase.GetFavoritesUseCase
import com.github.adriianh.core.domain.usecase.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.GetTrackUseCase
import com.github.adriianh.core.domain.usecase.IsFavoriteUseCase
import com.github.adriianh.core.domain.usecase.LoadMoreTracksUseCase
import com.github.adriianh.core.domain.usecase.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.RemoveFavoriteUseCase
import com.github.adriianh.core.domain.usecase.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.GetStreamUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.getValue
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

        val searchTracks: SearchTracksUseCase by inject()
        val loadMoreTracks: LoadMoreTracksUseCase by inject()
        val getTrack: GetTrackUseCase by inject()
        val getLyrics: GetLyricsUseCase by inject()
        val getSimilarTracks: GetSimilarTracksUseCase by inject()
        val getFavorites: GetFavoritesUseCase by inject()
        val addFavorite: AddFavoriteUseCase by inject()
        val removeFavorite: RemoveFavoriteUseCase by inject()
        val isFavorite: IsFavoriteUseCase by inject()
        val getRecentTracks: GetRecentTracksUseCase by inject()
        val recordPlay: RecordPlayUseCase by inject()
        val getStream: GetStreamUseCase by inject()

        try {
            MeloScreen(
                searchTracks, loadMoreTracks, getTrack, getLyrics, getSimilarTracks,
                getFavorites, addFavorite, removeFavorite, isFavorite,
                getRecentTracks, recordPlay, getStream,
            ).run()
        } finally {
            stopKoin()
        }

    }
}