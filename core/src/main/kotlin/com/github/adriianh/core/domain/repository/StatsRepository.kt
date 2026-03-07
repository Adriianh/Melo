package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.ArtistStat
import com.github.adriianh.core.domain.model.ListeningStats
import com.github.adriianh.core.domain.model.StatsPeriod
import com.github.adriianh.core.domain.model.TrackStat

/** Provides aggregated listening statistics derived from play history. */
interface StatsRepository {
    suspend fun getTopTracks(period: StatsPeriod, limit: Int = 10): List<TrackStat>
    suspend fun getTopArtists(period: StatsPeriod, limit: Int = 10): List<ArtistStat>
    suspend fun getListeningStats(period: StatsPeriod): ListeningStats
}