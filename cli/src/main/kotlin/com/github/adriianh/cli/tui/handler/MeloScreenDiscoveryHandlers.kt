package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Resolves a list of [Track]s similar to [seed] by querying Last.fm for metadata,
 * then resolving each result to a playable Piped video ID via search.
 *
 * Last.fm is used as the primary source because it returns genuinely similar tracks
 * from different artists. Piped's related-streams are used as a fallback when Last.fm
 * returns no results (e.g. API key suspended or unknown track).
 */
internal suspend fun MeloScreen.resolveSimilarTracks(seed: Track, limit: Int = 10): List<Track> {
    val lastFmResults = getSimilarTracks(seed.artist, seed.title)

    if (lastFmResults.isNotEmpty()) {
        val resolved = coroutineScope {
            lastFmResults
                .take(limit * 2)
                .map { similar ->
                    async {
                        val query = "${similar.title} ${similar.artist}"
                        pipedApiClient.searchTracks(query, limit = 1).firstOrNull()
                    }
                }
                .awaitAll()
                .filterNotNull()
                .distinctBy { it.sourceId }
                .take(limit)
        }
        if (resolved.isNotEmpty()) return resolved
    }

    val videoId = pipedApiClient.resolveVideoId(seed) ?: return emptyList()
    return pipedApiClient.getRelatedTracks(
        videoId       = videoId,
        limit         = limit,
        fallbackArtist = seed.artist,
        fallbackTitle  = seed.title,
    )
}