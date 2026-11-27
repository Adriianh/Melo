package com.github.adriianh.core.domain.model.search

data class SearchPage<T>(
    val items: List<T> = emptyList(),
    val continuation: String? = null,
)
