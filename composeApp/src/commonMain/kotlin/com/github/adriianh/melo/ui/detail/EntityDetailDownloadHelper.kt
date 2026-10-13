package com.github.adriianh.melo.ui.detail

import com.github.adriianh.core.domain.cache.EntityCache
import com.github.adriianh.core.domain.cache.LibraryCache
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.util.buildCompletedTrackLookups as coreBuildCompletedTrackLookups
import com.github.adriianh.core.util.cleanTrackId as coreCleanTrackId
import com.github.adriianh.core.util.findMatchingOfflineArtistTracks as coreFindMatchingOfflineArtistTracks
import com.github.adriianh.core.util.isSongCompleted as coreIsSongCompleted
import com.github.adriianh.core.util.normalizeTrackTitle as coreNormalizeTrackTitle
import com.github.adriianh.core.util.resolveOfflineEntity as coreResolveOfflineEntity

internal fun cleanTrackId(id: String): String = coreCleanTrackId(id)

internal fun normalizeTrackTitle(title: String): String = coreNormalizeTrackTitle(title)

internal fun buildCompletedTrackLookups(completedOffline: List<OfflineTrack>): Pair<Set<String>, Set<String>> =
    coreBuildCompletedTrackLookups(completedOffline)

internal fun isSongCompleted(
    song: Track,
    completedKeys: Set<String>,
    completedSignatures: Set<String>,
): Boolean = coreIsSongCompleted(song, completedKeys, completedSignatures)

internal fun computeOverallDownloadProgress(
    songs: List<Track>,
    completedTrackIds: Set<String>,
    activeDownloads: Map<String, Float>,
): Float =
    if (songs.isNotEmpty()) {
        val totalTrackProgress =
            songs.sumOf { track ->
                val cleanId = cleanTrackId(track.id)
                when {
                    track.id in completedTrackIds || cleanId in completedTrackIds -> 1.0
                    track.id in activeDownloads -> activeDownloads[track.id]?.toDouble() ?: 0.0
                    cleanId in activeDownloads -> activeDownloads[cleanId]?.toDouble() ?: 0.0
                    else -> 0.0
                }
            }
        (totalTrackProgress / songs.size).toFloat()
    } else {
        0f
    }

internal fun findMatchingOfflineArtistTracks(
    artist: SearchResult.Artist,
    offlineTracks: List<OfflineTrack>,
    initialArtistId: String = "",
): List<Track> = coreFindMatchingOfflineArtistTracks(artist, offlineTracks, initialArtistId)

internal suspend fun resolveOfflineEntity(
    initial: SearchResult,
    entityCache: EntityCache?,
    offlineRepository: OfflineRepository,
    libraryCache: LibraryCache? = null,
): SearchResult? = coreResolveOfflineEntity(initial, entityCache, offlineRepository, libraryCache)
