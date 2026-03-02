package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.usecase.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.GetTrackUseCase
import com.github.adriianh.core.domain.usecase.LoadMoreTracksUseCase
import com.github.adriianh.core.domain.usecase.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

class SearchCommand : CliktCommand(
    name = "search"
), KoinComponent {

    override fun run() {
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

        try {
            MeloScreen(searchTracks, loadMoreTracks, getTrack, getLyrics, getSimilarTracks).run()
        } finally {
            stopKoin()
        }
    }
}