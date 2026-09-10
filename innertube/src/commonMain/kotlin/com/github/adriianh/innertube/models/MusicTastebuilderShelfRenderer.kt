package com.github.adriianh.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicTastebuilderShelfRenderer(
    val title: Runs?,
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
        val musicNavigationButtonRenderer: MusicNavigationButtonRenderer?,
    )
}