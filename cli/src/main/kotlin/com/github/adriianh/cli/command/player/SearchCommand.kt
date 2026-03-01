package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.usecase.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.GetTrackUseCase
import com.github.adriianh.core.domain.usecase.LoadMoreTracksUseCase
import com.github.adriianh.core.domain.usecase.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchCommand : CliktCommand(
    name = "search"
), KoinComponent {

    private val searchTracks: SearchTracksUseCase by inject()
    private val loadMoreTracks: LoadMoreTracksUseCase by inject()
    private val getTrack: GetTrackUseCase by inject()
    private val getLyrics: GetLyricsUseCase by inject()
    private val getSimilarTracks: GetSimilarTracksUseCase by inject()

    override fun run() {
        MeloScreen(searchTracks, loadMoreTracks, getTrack, getLyrics, getSimilarTracks).run()
    }
}