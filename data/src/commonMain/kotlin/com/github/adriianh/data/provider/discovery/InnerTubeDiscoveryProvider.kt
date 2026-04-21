package com.github.adriianh.data.provider.discovery

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.WatchEndpoint

class InnerTubeDiscoveryProvider : DiscoveryProvider {

    override suspend fun getSimilarTracks(artist: String, title: String, limit: Int): List<SimilarTrack> {
        val query = "$title $artist"
        val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        val videoId = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.id ?: return emptyList()

        val tracks = mutableListOf<SimilarTrack>()

        var nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()

        while (nextResult != null && tracks.size < limit) {
            val songs = nextResult.items.filter { it.id != videoId }
            for (song in songs) {
                if (tracks.size >= limit) break
                val songTitle = song.title
                val songArtist = song.artists.firstOrNull()?.name ?: "Unknown"

                if (tracks.none { it.title == songTitle && it.artist == songArtist }) {
                    tracks.add(
                        SimilarTrack(
                            title = songTitle,
                            artist = songArtist,
                            match = 0.8 - (tracks.size.toDouble() / (limit * 2))
                        )
                    )
                }
            }

            val continuation = nextResult.continuation
            nextResult = if (continuation != null && tracks.size < limit) {
                YouTube.next(nextResult.endpoint, continuation).getOrNull()
            } else {
                null
            }
        }

        return tracks
    }

    override suspend fun getGenres(artist: String): List<String> {
        return emptyList()
    }
}
