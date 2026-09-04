@file:OptIn(ExperimentalSerializationApi::class)

package com.github.adriianh.innertube.models

import com.github.adriianh.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.github.adriianh.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.github.adriianh.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import com.github.adriianh.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PLAYLIST
import com.github.adriianh.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typical list item
 * Used in [MusicCarouselShelfRenderer], [MusicShelfRenderer]
 * Appears in quick picks, search results, table items, etc.
 */
@Serializable
data class MusicResponsiveListItemRenderer(
    val badges: List<Badges>?,
    val fixedColumns: List<FlexColumn>?,
    val flexColumns: List<FlexColumn>,
    val thumbnail: ThumbnailRenderer?,
    val menu: Menu?,
    val playlistItemData: PlaylistItemData?,
    val overlay: Overlay?,
    val navigationEndpoint: NavigationEndpoint?,
) {
    val isSong: Boolean
        get() = navigationEndpoint == null || navigationEndpoint.watchEndpoint != null || navigationEndpoint.watchPlaylistEndpoint != null
    val isPlaylist: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_PLAYLIST
    val isAlbum: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_ALBUM ||
                navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_AUDIOBOOK
    val isArtist: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_ARTIST

    val musicVideoType: String?
        get() =
            overlay
                ?.musicItemThumbnailOverlayRenderer
                ?.content
                ?.musicPlayButtonRenderer
                ?.playNavigationEndpoint
                ?.musicVideoType
                ?: navigationEndpoint?.musicVideoType

    val videoId: String?
        get() = playlistItemData?.videoId
            ?: navigationEndpoint?.watchEndpoint?.videoId
            ?: overlay?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
            ?: flexColumns.firstNotNullOfOrNull { col ->
                col.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstNotNullOfOrNull { it.navigationEndpoint?.watchEndpoint?.videoId }
            }
            ?: menu?.menuRenderer?.items?.firstNotNullOfOrNull {
                it.menuNavigationItemRenderer?.navigationEndpoint?.watchEndpoint?.videoId
                    ?: it.menuServiceItemRenderer?.serviceEndpoint?.queueAddEndpoint?.queueTarget?.videoId
            }
    val isAudioTrack: Boolean
        get() =
            overlay
                ?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                ?.watchEndpoint?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig?.musicVideoType == MUSIC_VIDEO_TYPE_ATV

    @Serializable
    data class FlexColumn(
        @SerialName("musicResponsiveListItemFixedColumnRenderer")
        private val _musicResponsiveListItemFixedColumnRenderer: MusicResponsiveListItemFlexColumnRenderer? = null,
        @SerialName("musicResponsiveListItemFlexColumnRenderer")
        private val _musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer? = null,
    ) {
        val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer?
            get() = _musicResponsiveListItemFixedColumnRenderer ?: _musicResponsiveListItemFlexColumnRenderer

        @Serializable
        data class MusicResponsiveListItemFlexColumnRenderer(
            val text: Runs?,
            val navigationEndpoint: NavigationEndpoint?,
        )
    }

    @Serializable
    data class PlaylistItemData(
        val playlistSetVideoId: String?,
        val videoId: String,
    )

    @Serializable
    data class Overlay(
        val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer,
    ) {
        @Serializable
        data class MusicItemThumbnailOverlayRenderer(
            val content: Content,
        ) {
            @Serializable
            data class Content(
                val musicPlayButtonRenderer: MusicPlayButtonRenderer,
            ) {
                @Serializable
                data class MusicPlayButtonRenderer(
                    val playNavigationEndpoint: NavigationEndpoint?,
                )
            }
        }
    }
}
