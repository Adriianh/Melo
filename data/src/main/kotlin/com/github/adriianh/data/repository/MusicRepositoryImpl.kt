package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
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
    private val discoveryProvider: DiscoveryProvider?
) : MusicRepository {

    private val pageSize = 20
    private val scope = CoroutineScope(Dispatchers.IO)

    private var cachedResults: List<Track> = emptyList()
    private var backgroundFetch: Job? = null

    override suspend fun search(query: String): List<Track> {
        // Cancel any previous background fetch
        backgroundFetch?.cancel()
        cachedResults = emptyList()

        // Return first 20 immediately for fast response
        val initial = musicProvider.search(query)
        cachedResults = initial

        // Fetch all 200 in background and update cache silently
        backgroundFetch = scope.launch {
            val full = musicProvider.searchAll(query)
            if (full.size > cachedResults.size) {
                cachedResults = full
            }
        }

        return initial.take(pageSize)
    }

    override suspend fun loadMore(query: String, offset: Int): List<Track> {
        // Wait for background fetch to complete before serving next pages
        backgroundFetch?.join()
        return cachedResults.drop(offset).take(pageSize)
    }

    override fun hasMore(offset: Int): Boolean {
        // If background fetch is still running we optimistically say true,
        // loadMore will join() and then return the real next page
        if (backgroundFetch?.isActive == true) return true
        return offset < cachedResults.size
    }

    override suspend fun getTrack(id: String): Track? = coroutineScope {
        val track = musicProvider.getTrack(id) ?: return@coroutineScope null
        val genres = async { discoveryProvider?.getGenres(track.artist) ?: emptyList() }
        val sourceId = async { audioProvider?.getSourceId(track.title, track.artist, track.durationMs) }
        track.copy(
            genres = genres.await(),
            sourceId = sourceId.await()
        )
    }
}