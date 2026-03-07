package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.ArtistStat
import com.github.adriianh.core.domain.model.ListeningStats
import com.github.adriianh.core.domain.model.StatsPeriod
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackStat
import com.github.adriianh.core.domain.repository.StatsRepository
import com.github.adriianh.data.local.MeloDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class StatsRepositoryImpl(database: MeloDatabase) : StatsRepository {

    private val queries = database.playHistoryQueries

    override suspend fun getTopTracks(period: StatsPeriod, limit: Int): List<TrackStat> =
        withContext(Dispatchers.IO) {
            queries.selectTopTracks(
                since = period.sinceEpochMillis(),
                limit = limit.toLong(),
            ).executeAsList().map { row ->
                TrackStat(
                    track = Track(
                        id = row.track_id,
                        title = row.title,
                        artist = row.artist,
                        album = row.album,
                        durationMs = (row.total_ms ?: 0) / row.play_count,
                        genres = emptyList(),
                        artworkUrl = row.artwork_url,
                        sourceId = row.source_id,
                    ),
                    playCount = row.play_count.toInt(),
                    totalMs = row.total_ms ?: 0,
                )
            }
        }

    override suspend fun getTopArtists(period: StatsPeriod, limit: Int): List<ArtistStat> =
        withContext(Dispatchers.IO) {
            queries.selectTopArtists(
                since = period.sinceEpochMillis(),
                limit = limit.toLong(),
            ).executeAsList().map { row ->
                ArtistStat(
                    artist = row.artist,
                    playCount = row.play_count.toInt(),
                    totalMs = row.total_ms ?: 0,
                )
            }
        }

    override suspend fun getListeningStats(period: StatsPeriod): ListeningStats =
        withContext(Dispatchers.IO) {
            val row = queries.selectTotalListeningMs(
                since = period.sinceEpochMillis(),
            ).executeAsOne()
            ListeningStats(
                totalMs = row.total_ms,
                totalPlays = row.total_plays.toInt(),
                uniqueTracks = row.unique_tracks.toInt(),
                uniqueArtists = row.unique_artists.toInt(),
            )
        }

    private fun StatsPeriod.sinceEpochMillis(): Long = when (this) {
        StatsPeriod.WEEK -> Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()
        StatsPeriod.MONTH -> Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
        StatsPeriod.ALL_TIME -> 0L
    }
}