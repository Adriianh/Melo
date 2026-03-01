package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.PaginableMusicProvider
import com.github.adriianh.data.remote.itunes.ItunesApiClient
import com.github.adriianh.data.remote.itunes.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ItunesMusicProvider(
    private val apiClient: ItunesApiClient
) : PaginableMusicProvider {

    private var cachedResults: List<Track> = emptyList()
    private val pageSize = 20
    private val scope = CoroutineScope(Dispatchers.IO)
    private var prefetchJob: Job? = null

    override suspend fun search(query: String): List<Track> {
        val initial = apiClient.search(query, limit = 50).map { it.toDomain() }
        cachedResults = deduplicate(initial)

        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            val full = apiClient.search(query, limit = 200).map { it.toDomain() }
            cachedResults = deduplicate(full)
        }

        return cachedResults.take(pageSize)
    }

    private fun deduplicate(tracks: List<Track>): List<Track> {
        val seen = mutableSetOf<String>()
        return tracks.filter { track ->
            val normalizedTitle = track.title
                .lowercase()
                .replace(Regex("\\s*\\(.*?\\)"), "")
                .replace(Regex("\\s*\\[.*?]"), "")
                .trim()
            val key = "${track.artist.lowercase()}|$normalizedTitle"
            seen.add(key)
        }
    }

    override suspend fun searchPage(query: String, offset: Int): List<Track> {
        prefetchJob?.join()
        return cachedResults.drop(offset).take(pageSize)
    }

    override fun hasMore(offset: Int): Boolean = offset < cachedResults.size

    override suspend fun getTrack(id: String): Track? {
        val trackId = id.removePrefix("itunes:")
        return apiClient.getTrack(trackId)?.toDomain()
    }
}
