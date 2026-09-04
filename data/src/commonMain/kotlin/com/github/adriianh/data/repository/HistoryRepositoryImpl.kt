package com.github.adriianh.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.local.Play_history
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.YouTubeClient
import com.github.adriianh.innertube.models.response.PlayerResponse
import com.github.adriianh.innertube.utils.YouTubeStreamUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HistoryRepositoryImpl(
    database: MeloDatabase,
    private val settingsRepository: SettingsRepository,
) : HistoryRepository {

    constructor(database: MeloDatabase) : this(
        database = database,
        settingsRepository = object : SettingsRepository {
            private val flow = MutableStateFlow(Settings(syncHistoryToYouTube = false))
            override fun getSettingsFlow() = flow
            override suspend fun getSettings() = flow.value
            override suspend fun updateSettings(settings: Settings) {}
        }
    )

    private val queries = database.playHistoryQueries

    override fun getRecentTracks(limit: Int): Flow<List<HistoryEntry>> =
        queries.selectDistinctRecentTracks(limit.toLong())
            .asFlow()
            .mapToList(MeloDispatchers.IO)
            .map { rows -> rows.map { it.toHistoryEntry() } }

    override suspend fun recordPlay(entry: HistoryEntry) {
        withContext(MeloDispatchers.IO) {
            queries.insertPlay(
                track_id = entry.track.id,
                title = entry.track.title,
                artist = entry.track.artist,
                album = entry.track.album,
                duration_ms = entry.track.durationMs,
                artwork_url = entry.track.artworkUrl,
                source_id = entry.track.sourceId,
                played_at = entry.playedAt,
            )

            try {
                val settings = settingsRepository.getSettings()
                val cookies = settings.sessionCookies
                val isLoggedIn = !cookies.isNullOrBlank()
                val shouldSync = settings.syncHistoryToYouTube
                if (isLoggedIn && shouldSync) {
                    if (YouTube.cookie.isNullOrBlank()) {
                        YouTube.cookie = cookies
                        YouTube.useLoginForBrowse = true
                    }
                    val rawVideoId = entry.track.sourceId?.takeIf { it.isNotBlank() }
                        ?: entry.track.id.removePrefix("piped:").takeIf {
                            !it.startsWith("local:") && !it.startsWith("itunes:") && !it.startsWith(
                                "spotify:"
                            )
                        }
                    if (rawVideoId != null) {
                        val sts = YouTubeStreamUtils.getSignatureTimestamp(rawVideoId).getOrNull()
                        val clientsToTry = listOf(
                            YouTubeClient.WEB_REMIX,
                            YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
                            YouTubeClient.ANDROID_MUSIC,
                            YouTubeClient.ANDROID_VR_NO_AUTH,
                            YouTubeClient.IOS
                        )
                        var playerRes: PlayerResponse? = null
                        for (client in clientsToTry) {
                            val clientSts = if (client.useSignatureTimestamp) sts else null
                            val playerResult = YouTube.player(
                                videoId = rawVideoId,
                                client = client,
                                signatureTimestamp = clientSts
                            )
                            val res = playerResult.getOrNull()
                            if (res?.playbackTracking?.videostatsPlaybackUrl?.baseUrl != null) {
                                playerRes = res
                                break
                            }
                        }
                        val trackingUrl =
                            playerRes?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        val watchtimeUrl =
                            playerRes?.playbackTracking?.videostatsWatchtimeUrl?.baseUrl
                        if (trackingUrl != null) {
                            YouTube.registerPlayback(
                                playbackTracking = trackingUrl,
                                watchtimeTracking = watchtimeUrl
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun pruneOldHistory() {
        withContext(MeloDispatchers.IO) {
            queries.deleteOldHistory()
        }
    }

    private fun Play_history.toHistoryEntry() = HistoryEntry(
        track = Track(
            id = track_id,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration_ms,
            genres = emptyList(),
            artworkUrl = artwork_url,
            sourceId = source_id,
        ),
        playedAt = played_at,
    )
}
