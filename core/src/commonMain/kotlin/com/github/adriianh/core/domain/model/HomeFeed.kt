package com.github.adriianh.core.domain.model

import com.github.adriianh.core.domain.model.search.SearchResult

data class HomeFeedChip(
    val title: String,
    val params: String? = null,
)

enum class HomeSectionType {
    SONGS, ALBUMS, PLAYLISTS, ARTISTS, VIDEOS, MIXED
}

data class HomeSection(
    val title: String,
    val type: HomeSectionType,
    val items: List<SearchResult>,
)

data class HomeFeed(
    val chips: List<HomeFeedChip> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val continuation: String? = null,
)
