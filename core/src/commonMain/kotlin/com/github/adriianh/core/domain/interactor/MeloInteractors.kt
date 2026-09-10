package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.session.ClearSessionUseCase
import com.github.adriianh.core.domain.usecase.session.RestoreSessionUseCase
import com.github.adriianh.core.domain.usecase.session.SaveSessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetListeningStatsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetTopArtistsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetTopTracksUseCase

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
