package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MusicRepositoryImpl(
    private val musicProvider: MusicProvider,
    private val audioProvider: AudioProvider?,
    private val discoveryProvider: DiscoveryProvider?,
    private val artworkProvider: ArtworkProvider? = null
) : MusicRepository {

    private val pageSize = 20
    private val scope = CoroutineScope(Dispatchers.IO)

    private var cachedResults: List<Track> = emptyList()
    private var backgroundFetch: Job? = null

    override suspend fun search(query: String): List<Track> {
        backgroundFetch?.cancel()
        cachedResults = emptyList()

        val initial = deduplicate(musicProvider.search(query))
        cachedResults = initial

        backgroundFetch = scope.launch {
            val full = deduplicate(musicProvider.searchAll(query))
            if (full.size > cachedResults.size) {
                cachedResults = full
            }
        }

        return initial.take(pageSize)
    }

    override suspend fun loadMore(query: String, offset: Int): List<Track> {
        backgroundFetch?.join()
        return cachedResults.drop(offset).take(pageSize)
    }

    override fun hasMore(offset: Int): Boolean {
        if (backgroundFetch?.isActive == true) return true
        return offset < cachedResults.size
    }

    private fun deduplicate(tracks: List<Track>): List<Track> {
        val seen = mutableSetOf<String>()
        return tracks.filter { track ->
            val title = track.title
                .lowercase()
                .replace(Regex("""\s*[(\[{].*?[)\]}]"""), "")
                .trim()
            val durationBucket = track.durationMs / 10_000  // 10-second buckets
            val key = "${track.artist.lowercase()}|$title|$durationBucket"
            seen.add(key)
        }
    }

    override suspend fun getTrack(id: String): Track? = coroutineScope {
        val track = musicProvider.getTrack(id) ?: return@coroutineScope null
        val genres   = async { discoveryProvider?.getGenres(track.artist) ?: emptyList() }
        val sourceId = if (track.sourceId != null) {
            async { track.sourceId }
        } else {
            async { audioProvider?.getSourceId(track.title, track.artist, track.durationMs) }
        }
        val artwork = if (track.artworkUrl != null) {
            async { track.artworkUrl }
        } else {
            async { artworkProvider?.resolveArtwork(track.title, track.artist) }
        }
        track.copy(
            genres     = genres.await(),
            sourceId   = sourceId.await(),
            artworkUrl = artwork.await()
        )
    }
}