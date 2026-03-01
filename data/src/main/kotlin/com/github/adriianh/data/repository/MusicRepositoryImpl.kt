package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.provider.PaginableMusicProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MusicRepositoryImpl(
    private val musicProvider: MusicProvider,
    private val audioProvider: AudioProvider?,
    private val discoveryProvider: DiscoveryProvider?
) : MusicRepository {

    override suspend fun search(query: String): List<Track> {
        return musicProvider.search(query)
    }

    override suspend fun loadMore(query: String, offset: Int): List<Track> {
        return (musicProvider as? PaginableMusicProvider)?.searchPage(query, offset) ?: emptyList()
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