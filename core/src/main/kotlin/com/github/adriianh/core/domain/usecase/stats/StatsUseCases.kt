package com.github.adriianh.core.domain.usecase.stats

import com.github.adriianh.core.domain.model.ArtistStat
import com.github.adriianh.core.domain.model.ListeningStats
import com.github.adriianh.core.domain.model.StatsPeriod
import com.github.adriianh.core.domain.model.TrackStat
import com.github.adriianh.core.domain.repository.StatsRepository

class GetTopTracksUseCase(private val repository: StatsRepository) {
    suspend operator fun invoke(period: StatsPeriod, limit: Int = 10): List<TrackStat> =
        repository.getTopTracks(period, limit)
}

class GetTopArtistsUseCase(private val repository: StatsRepository) {
    suspend operator fun invoke(period: StatsPeriod, limit: Int = 10): List<ArtistStat> =
        repository.getTopArtists(period, limit)
}

class GetListeningStatsUseCase(private val repository: StatsRepository) {
    suspend operator fun invoke(period: StatsPeriod): ListeningStats =
        repository.getListeningStats(period)
}