package com.github.adriianh.innertube.pages

import com.github.adriianh.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)