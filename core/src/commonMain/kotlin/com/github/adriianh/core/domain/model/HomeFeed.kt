package com.github.adriianh.core.domain.model

import com.github.adriianh.core.domain.model.search.SearchResult

data class HomeFeedChip(
    val title: String,
    val params: String? = null,
)

data class HomeFeed(
    val chips: List<HomeFeedChip> = emptyList(),
    val sections: List<SearchResult.ArtistSection> = emptyList(),
    val continuation: String? = null,
)