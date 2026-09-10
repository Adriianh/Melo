package com.github.adriianh.core.domain.model

data class MoodAndGenreGroup(
    val title: String,
    val items: List<MoodAndGenreCategory>,
)

data class MoodAndGenreCategory(
    val title: String,
    val stripeColor: Long,
    val browseId: String,
    val params: String? = null,
)