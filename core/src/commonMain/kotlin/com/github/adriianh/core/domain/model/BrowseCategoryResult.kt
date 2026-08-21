package com.github.adriianh.core.domain.model

import com.github.adriianh.core.domain.model.search.SearchResult

data class BrowseCategoryResult(
    val title: String?,
    val sections: List<BrowseCategorySection>,
)

data class BrowseCategorySection(
    val title: String?,
    val items: List<SearchResult>,
)