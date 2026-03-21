package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.launch

internal fun MeloScreen.toggleFavorite(track: Track) {
    scope.launch {
        if (isFavoriteUseCase(track.id)) removeFavorite(track.id) else addFavorite(track)
        val isFav = isFavoriteUseCase(track.id)
        appRunner()?.runOnRenderThread { state = state.copy(player = state.player.copy(isFavorite = isFav)) }
    }
}

internal fun MeloScreen.removeFavoriteTrack(track: Track) {
    scope.launch { removeFavorite(track.id) }
}

internal fun MeloScreen.checkIsFavorite(trackId: String) {
    scope.launch {
        val isFav = isFavoriteUseCase(trackId)
        appRunner()?.runOnRenderThread { state = state.copy(player = state.player.copy(isFavorite = isFav)) }
    }
}

