package com.github.adriianh.innertube.pages

import com.github.adriianh.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
