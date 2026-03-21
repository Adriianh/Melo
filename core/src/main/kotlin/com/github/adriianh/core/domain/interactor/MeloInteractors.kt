package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.*

data class StatsInteractors(
    val getTopTracks: GetTopTracksUseCase,
    val getTopArtists: GetTopArtistsUseCase,
    val getListeningStats: GetListeningStatsUseCase,
)

data class SessionInteractors(
    val saveSession: SaveSessionUseCase,
    val restoreSession: RestoreSessionUseCase,
    val clearSession: ClearSessionUseCase,
)

data class SettingsInteractors(
    val getSettings: GetSettingsUseCase,
    val updateSettings: UpdateSettingsUseCase,
)
