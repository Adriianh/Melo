package com.github.adriianh.innertube.models

import com.github.adriianh.innertube.models.response.BrowseResponse
import com.github.adriianh.innertube.pages.RelatedPage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class SectionListRenderer(
    val header: Header?,
    val contents: List<Content>?,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Header(
        val chipCloudRenderer: ChipCloudRenderer?,
    ) {
        @Serializable
        data class ChipCloudRenderer(
            val chips: List<Chip>,
        ) {
            @Serializable
            data class Chip(
                val chipCloudChipRenderer: ChipCloudChipRenderer,
            ) {
                @Serializable
                data class ChipCloudChipRenderer(
                    val isSelected: Boolean,
                    val navigationEndpoint: NavigationEndpoint,
                    val onDeselectedCommand: NavigationEndpoint ? = null,
                    val text: Runs?,
                    val uniqueId: String?,
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
        val musicShelfRenderer: MusicShelfRenderer?,
        val musicCardShelfRenderer: MusicCardShelfRenderer?,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
        val gridRenderer: GridRenderer?,
        val musicTastebuilderShelfRenderer: MusicTastebuilderShelfRenderer?,
        val musicResponsiveHeaderRenderer: BrowseResponse.Header.MusicHeaderRenderer?,
        val musicEditablePlaylistDetailHeaderRenderer: BrowseResponse.Header.MusicEditablePlaylistDetailHeaderRenderer?,
        val itemSectionRenderer: ItemSectionRenderer?,
    )

    companion object {
        fun Content.processMusicCarouselShelf(
            addItem: (YTItem, MusicResponsiveListItemRenderer?) -> Unit
        ) {
            musicCarouselShelfRenderer?.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    RelatedPage.fromMusicResponsiveListItemRenderer(renderer)?.let { item ->
                        addItem(item, renderer)
                    }
                }
                content.musicTwoRowItemRenderer?.let { renderer ->
                    RelatedPage.fromMusicTwoRowItemRenderer(renderer)?.let { item ->
                        addItem(item, null)
                    }
                }
            }
        }

        fun Content.processMusicShelf(
            addItem: (YTItem, MusicResponsiveListItemRenderer?) -> Unit
        ) {
            musicShelfRenderer?.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    RelatedPage.fromMusicResponsiveListItemRenderer(renderer)?.let { item ->
                        addItem(item, renderer)
                    }
                }
            }
        }

        fun Content.processItemSection(
            addItem: (YTItem, MusicResponsiveListItemRenderer?) -> Unit
        ) {
            itemSectionRenderer?.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    RelatedPage.fromMusicResponsiveListItemRenderer(renderer)?.let { item ->
                        addItem(item, renderer)
                    }
                }
            }
        }
    }
}

@Serializable
data class ItemSectionRenderer(
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    )
}