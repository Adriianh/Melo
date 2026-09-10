package com.github.adriianh.core.domain.model

data class TrackStat(
    val track: Track,
    val playCount: Int,
    val totalMs: Long,
)

data class ArtistStat(
    val artist: String,
    val playCount: Int,
    val totalMs: Long,
)

data class ListeningStats(
    val totalMs: Long,
    val totalPlays: Int,
    val uniqueTracks: Int,
    val uniqueArtists: Int,
)

/** Time range for stats queries. */
enum class StatsPeriod(val labelShort: String) {
    WEEK("7d"),
    MONTH("30d"),
    ALL_TIME("All"),
}