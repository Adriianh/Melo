package com.github.adriianh.core.domain.model

import com.github.adriianh.core.domain.model.search.SearchResult

enum class FavoriteEntityType {
    ALBUM,
    ARTIST,
    PLAYLIST
}

data class FavoriteEntity(
    val id: String,
    val type: FavoriteEntityType,
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val trackCount: Int? = null,
    val addedAt: Long = 0L,
)

fun SearchResult.Album.toFavoriteEntity(addedAt: Long = 0L): FavoriteEntity = FavoriteEntity(
    id = id,
    type = FavoriteEntityType.ALBUM,
    title = title,
    subtitle = author + (year?.let { " • $it" } ?: ""),
    artworkUrl = artworkUrl,
    trackCount = songs?.size,
    addedAt = addedAt
)

fun SearchResult.Artist.toFavoriteEntity(addedAt: Long = 0L): FavoriteEntity = FavoriteEntity(
    id = id,
    type = FavoriteEntityType.ARTIST,
    title = name,
    subtitle = listOfNotNull(monthlyListenerCount, subscriberCountText).firstOrNull(),
    artworkUrl = artworkUrl,
    trackCount = null,
    addedAt = addedAt
)

fun SearchResult.Playlist.toFavoriteEntity(addedAt: Long = 0L): FavoriteEntity = FavoriteEntity(
    id = id,
    type = FavoriteEntityType.PLAYLIST,
    title = title,
    subtitle = author,
    artworkUrl = artworkUrl,
    trackCount = trackCount ?: songs?.size,
    addedAt = addedAt
)

fun FavoriteEntity.toSearchResult(): SearchResult = when (type) {
    FavoriteEntityType.ALBUM -> SearchResult.Album(
        id = id,
        title = title,
        author = subtitle?.substringBefore(" • ") ?: "",
        year = subtitle?.substringAfter(" • ", "")?.takeIf { it.isNotBlank() },
        artworkUrl = artworkUrl,
    )

    FavoriteEntityType.ARTIST -> SearchResult.Artist(
        id = id,
        name = title,
        artworkUrl = artworkUrl,
        monthlyListenerCount = subtitle
    )

    FavoriteEntityType.PLAYLIST -> SearchResult.Playlist(
        id = id,
        title = title,
        author = subtitle ?: "",
        artworkUrl = artworkUrl,
        trackCount = trackCount
    )
}