package com.github.adriianh.innertube.pages

import com.github.adriianh.innertube.models.Album
import com.github.adriianh.innertube.models.AlbumItem
import com.github.adriianh.innertube.models.Artist
import com.github.adriianh.innertube.models.MusicResponsiveListItemRenderer
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.extractArtists
import com.github.adriianh.innertube.models.splitBySeparator
import com.github.adriianh.innertube.utils.parseTime

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
) {
    companion object {
        fun getSong(
            renderer: MusicResponsiveListItemRenderer,
            album: AlbumItem? = null
        ): SongItem? {
            val videoId = renderer.playlistItemData?.videoId
                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()
                    ?.navigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.flexColumns.getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()
                    ?.navigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.menu?.menuRenderer?.items?.firstNotNullOfOrNull {
                    it.menuNavigationItemRenderer?.navigationEndpoint?.watchEndpoint?.videoId
                        ?: it.menuServiceItemRenderer?.serviceEndpoint?.queueAddEndpoint?.queueTarget?.videoId
                }
                ?: return null

            val title = renderer.flexColumns.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text?.runs?.firstOrNull()?.text
                ?: PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text
                ?: return null

            val artists = renderer.flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                ?.splitBySeparator()?.firstOrNull()?.extractArtists().orEmpty().ifEmpty {
                    album?.artists.orEmpty().ifEmpty {
                        listOf(Artist(name = "Unknown", id = null))
                    }
                }

            val albumObj = album?.let {
                Album(it.title, it.browseId)
            } ?: renderer.flexColumns.getOrNull(2)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                ?.let {
                    val albumId = it.navigationEndpoint?.browseEndpoint?.browseId
                    if (albumId != null) Album(name = it.text, id = albumId) else null
                }

            val duration = renderer.fixedColumns?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                ?.text?.parseTime()

            val setVideoId = renderer.playlistItemData?.playlistSetVideoId
                ?: renderer.navigationEndpoint?.watchEndpoint?.playlistSetVideoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint?.watchEndpoint?.playlistSetVideoId
                ?: renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()
                    ?.navigationEndpoint?.watchEndpoint?.playlistSetVideoId

            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: album?.thumbnail
                ?: ""

            return SongItem(
                id = videoId,
                title = title,
                artists = artists,
                album = albumObj,
                duration = duration,
                setVideoId = setVideoId,
                thumbnail = thumbnail,
                musicVideoType = renderer.musicVideoType,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint
            )
        }
    }
}