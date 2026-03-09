package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import io.ktor.utils.io.CancellationException

private const val SAME_ARTIST_LIMIT = 3
private const val DISCOVERY_LIMIT = 5

/**
 * Resolves a list of [Track]s similar to [seed] by merging two parallel sources:
 *
 * 1. **Same-artist** — Piped search for the seed's artist, returning up to
 *    [SAME_ARTIST_LIMIT] tracks from the same artist (excluding the seed itself).
 *    These are placed **first** in the result list to prioritise the artist.
 *
 * 2. **Discovery** — Last.fm (or Deezer as fallback), resolved to playable Piped
 *    video IDs. Filter out tracks whose `sourceId` already appeared in source 1.
 *
 * [offset] is used for progressive loading. When offset > 0, same-artist checks
 * are skipped, and discovery results are dropped by `offset * DISCOVERY_LIMIT`
 * prior to Piped resolution.
 */
internal suspend fun MeloScreen.resolveSimilarTracks(seed: Track, limit: Int = 10, offset: Int = 0): List<Track> =
    supervisorScope {
        // --- parallel fetch ---
        val sameArtistDeferred = async {
            if (offset > 0) emptyList() else {
                pipedApiClient.searchTracks(seed.artist, limit = SAME_ARTIST_LIMIT * 2)
                    .filter { track ->
                        val trackId = track.sourceId
                        val sameTitle = track.title.trim().equals(seed.title.trim(), ignoreCase = true)
                        val sameSource = trackId != null && trackId == seed.sourceId
                        !sameTitle && !sameSource
                    }
                    .take(SAME_ARTIST_LIMIT)
            }
        }

        val lastFmDeferred = async {
            val totalRequired = (offset + limit) * DISCOVERY_LIMIT
            val similar = getSimilarTracks(seed.artist, seed.title, totalRequired)
            if (similar.isNotEmpty()) {
                similar
                    .drop(offset * DISCOVERY_LIMIT)
                    .take(limit * DISCOVERY_LIMIT)
                    .map { s ->
                        async {
                            pipedApiClient.searchTracks("${s.title} ${s.artist}", limit = 1).firstOrNull()
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .distinctBy { it.sourceId }
                    .take(limit)
            } else {
                emptyList()
            }
        }

        val sameArtist = sameArtistDeferred.await()
        val discovery = lastFmDeferred.await()

        val seenIds = sameArtist.mapNotNull { it.sourceId }.toMutableSet()
        val filtered = discovery.filter { track ->
            val id = track.sourceId
            id == null || seenIds.add(id)
        }
        val merged = (sameArtist + filtered).take(limit)

        if (merged.isNotEmpty()) return@supervisorScope merged

        val videoId = pipedApiClient.resolveVideoId(seed) ?: return@supervisorScope emptyList<Track>()
        if (offset > 0) return@supervisorScope emptyList()

        pipedApiClient.getRelatedTracks(
            videoId = videoId,
            limit = limit,
            fallbackArtist = seed.artist,
            fallbackTitle = seed.title,
        )
    }

internal fun MeloScreen.loadMoreSimilar() {
    val seed = state.detail.selectedTrack ?: return
    if (state.detail.isLoadingMoreSimilar || !state.detail.hasMoreSimilar) return

    val currentOffset = state.detail.similarTracks.size
    state = state.copy(detail = state.detail.copy(isLoadingMoreSimilar = true))

    scope.launch {
        try {
            var more = resolveSimilarTracks(seed, limit = 10, offset = currentOffset)
            
            if (more.isEmpty() && state.detail.similarTracks.isNotEmpty()) {
                val newSeed = state.detail.similarTracks.random()
                more = resolveSimilarTracks(newSeed, limit = 10, offset = 0)
            }

            if (isActive) appRunner()?.runOnRenderThread {
                val updatedList = (state.detail.similarTracks + more).distinctBy { it.id }
                state = state.copy(
                    detail = state.detail.copy(
                        similarTracks = updatedList,
                        isLoadingMoreSimilar = false,
                        hasMoreSimilar = more.isNotEmpty() || updatedList.size > state.detail.similarTracks.size
                    )
                )
            }
        } catch (_: CancellationException) {
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread { state = state.copy(detail = state.detail.copy(isLoadingMoreSimilar = false)) }
        }
    }
}