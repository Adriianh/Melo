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
            return SongItem(
                id = renderer.playlistItemData?.videoId
                    ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                    ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                        ?.content?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint?.watchEndpoint?.videoId
                    ?: renderer.flexColumns.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text?.runs?.firstOrNull()
                        ?.navigationEndpoint?.watchEndpoint?.videoId
                    ?: return null,
                title = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO")
                    .firstOrNull()?.text ?: return null,
                artists = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                    ?.splitBySeparator()?.firstOrNull()?.extractArtists().orEmpty().ifEmpty {
                        listOf(Artist(name = "Unknown", id = null))
                    },
                album = album?.let {
                    Album(it.title, it.browseId)
                }
                    ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                        ?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return null
                            )
                        },
                duration = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text?.parseTime() ?: return null,
                setVideoId = renderer.playlistItemData?.playlistSetVideoId
                    ?: renderer.navigationEndpoint?.watchEndpoint?.playlistSetVideoId
                    ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                        ?.content?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint?.watchEndpoint?.playlistSetVideoId
                    ?: renderer.flexColumns.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text?.runs?.firstOrNull()
                        ?.navigationEndpoint?.watchEndpoint?.playlistSetVideoId
                    ?: return null,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: album?.thumbnail
                    ?: return null,
                musicVideoType = renderer.musicVideoType,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null
            )
        }
    }
}