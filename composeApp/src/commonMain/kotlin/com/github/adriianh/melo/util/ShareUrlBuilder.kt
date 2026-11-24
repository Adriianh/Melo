package com.github.adriianh.melo.util

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult

/**
 * Builds a shareable YouTube Music URL for [track], or `null` for local tracks without a remote ID.
 */
fun buildTrackShareUrl(track: Track): String? =
    track.id
        .takeUnless { it.startsWith("local:") }
        ?.stripRemoteProviderPrefix()
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://music.youtube.com/watch?v=$it" }

/**
 * Builds a shareable YouTube Music URL for [entity], or `null` if no remote URL can be derived.
 */
fun buildEntityShareUrl(entity: SearchResult): String? =
    when (entity) {
        is SearchResult.Song -> buildTrackShareUrl(entity.track)
        is SearchResult.Album -> buildAlbumShareUrl(entity.id)
        is SearchResult.Playlist -> buildPlaylistShareUrl(entity.id)
        is SearchResult.Artist -> buildArtistShareUrl(entity.id)
    }

private fun buildAlbumShareUrl(id: String): String? =
    id
        .takeUnless { it.startsWith("local:") || it.startsWith("piped:") || it.startsWith("itunes:") }
        ?.removePrefix("yt:")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { rawId ->
            if (rawId.startsWith("OLAK")) {
                "https://music.youtube.com/playlist?list=$rawId"
            } else {
                "https://music.youtube.com/browse/$rawId"
            }
        }

private fun buildPlaylistShareUrl(id: String): String? =
    id
        .takeUnless { it.startsWith("local:") }
        ?.stripRemoteProviderPrefix()
        ?.removePrefix("VL")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://music.youtube.com/playlist?list=$it" }

private fun buildArtistShareUrl(id: String): String? =
    id
        .stripRemoteProviderPrefix()
        .takeIf { it.isNotBlank() }
        ?.let { rawId ->
            if (rawId.startsWith("UC")) {
                "https://music.youtube.com/channel/$rawId"
            } else {
                "https://music.youtube.com/browse/$rawId"
            }
        }

private fun String.stripRemoteProviderPrefix(): String =
    removePrefix("yt:")
        .removePrefix("piped:")
        .trim()
