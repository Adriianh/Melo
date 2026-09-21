package com.github.adriianh.innertube.pages

import com.github.adriianh.innertube.models.Album
import com.github.adriianh.innertube.models.AlbumItem
import com.github.adriianh.innertube.models.Artist
import com.github.adriianh.innertube.models.ArtistItem
import com.github.adriianh.innertube.models.BrowseEndpoint
import com.github.adriianh.innertube.models.GridRenderer
import com.github.adriianh.innertube.models.MusicCarouselShelfRenderer
import com.github.adriianh.innertube.models.MusicResponsiveListItemRenderer
import com.github.adriianh.innertube.models.MusicShelfRenderer
import com.github.adriianh.innertube.models.MusicTastebuilderShelfRenderer
import com.github.adriianh.innertube.models.MusicTwoRowItemRenderer
import com.github.adriianh.innertube.models.PlaylistItem
import com.github.adriianh.innertube.models.SectionListRenderer
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.YTItem
import com.github.adriianh.innertube.models.extractArtists
import com.github.adriianh.innertube.models.extractDuration
import com.github.adriianh.innertube.models.getItems
import com.github.adriianh.innertube.models.isInvalidArtistName
import com.github.adriianh.innertube.models.isKnownTypeLabel
import com.github.adriianh.innertube.models.isTimeDuration
import com.github.adriianh.innertube.models.splitBySeparator
import com.github.adriianh.innertube.utils.parseTime

data class HomePage(
    val chips: List<Chip>?,
    val sections: List<Section>,
    val continuation: String? = null,
) {
    data class Chip(
        val title: String,
        val endpoint: BrowseEndpoint?,
        val deselectEndPoint: BrowseEndpoint?,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(renderer: SectionListRenderer.Header.ChipCloudRenderer.Chip): Chip? {
                return Chip(
                    title = renderer.chipCloudChipRenderer.text?.runs?.firstOrNull()?.text
                        ?: return null,
                    endpoint = renderer.chipCloudChipRenderer.navigationEndpoint.browseEndpoint,
                    deselectEndPoint = renderer.chipCloudChipRenderer.onDeselectedCommand?.browseEndpoint,
                )
            }
        }
    }

    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
        val sectionType: SectionType,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
                return Section(
                    title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: return null,
                    label = renderer.header.musicCarouselShelfBasicHeaderRenderer.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = renderer.header.musicCarouselShelfBasicHeaderRenderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                    items = renderer.contents.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                            fromMusicTwoRowItemRenderer(
                                renderer
                            )
                        } ?: it.musicResponsiveListItemRenderer?.let { renderer ->
                            fromMusicResponsiveListItemRenderer(
                                renderer
                            )
                        }
                    }.ifEmpty {
                        return null
                    },
                    sectionType = if (renderer.contents.any { it.musicResponsiveListItemRenderer != null }) SectionType.GRID else SectionType.LIST,
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                return when {
                    renderer.isSong -> {
                        val allRuns = renderer.subtitle?.runs.orEmpty()
                        val duration = renderer.extractDuration()
                            ?: allRuns.extractDuration()
                            ?: allRuns.map { it.text.trim() }.find { it.isTimeDuration() }
                                ?.parseTime()

                        val groups = allRuns.splitBySeparator()
                        val nonDurationGroups = groups.filter { group ->
                            val text = group.joinToString("") { it.text }.trim()
                            !text.isInvalidArtistName()
                        }

                        val explicitAlbumRun = allRuns.find { run ->
                            run.navigationEndpoint?.browseEndpoint?.isAlbumEndpoint == true ||
                                    run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPRE") == true
                        }

                        val album = explicitAlbumRun?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: ""
                            )
                        } ?: run {
                            val candidateGroup = nonDurationGroups.getOrNull(1)
                            val candidateRun = candidateGroup?.firstOrNull()
                            val isArtist =
                                candidateRun?.navigationEndpoint?.browseEndpoint?.isArtistEndpoint == true ||
                                        candidateRun?.navigationEndpoint?.browseEndpoint?.browseId?.startsWith(
                                            "UC"
                                        ) == true
                            if (candidateRun != null && !isArtist) {
                                val albumName = candidateGroup.joinToString("") { it.text }.trim()
                                if (albumName.isNotEmpty()) {
                                    Album(
                                        name = albumName,
                                        id = candidateRun.navigationEndpoint?.browseEndpoint?.browseId
                                            ?: ""
                                    )
                                } else null
                            } else null
                        }

                        val artists =
                            nonDurationGroups.firstOrNull()?.extractArtists().orEmpty().ifEmpty {
                            allRuns.filter { run ->
                                val id = run.navigationEndpoint?.browseEndpoint?.browseId
                                id?.startsWith("UC") == true
                            }.map { run ->
                                Artist(
                                    name = run.text,
                                    id = run.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            }
                        }.ifEmpty {
                                nonDurationGroups.firstOrNull()?.map { run ->
                                    Artist(
                                        name = run.text,
                                        id = run.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            } ?: emptyList()
                        }

                        SongItem(
                            id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = artists,
                            album = album,
                            duration = duration,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            musicVideoType = renderer.musicVideoType,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
                        )
                    }

                    renderer.isAlbum -> {
                        val subtitleRuns = renderer.subtitle?.runs.orEmpty()
                        val groups = subtitleRuns.splitBySeparator()
                        val nonTypeGroups = groups.filter { group ->
                            val text = group.joinToString("") { it.text }.trim().lowercase()
                            !text.isKnownTypeLabel()
                        }
                        val artists = nonTypeGroups.firstOrNull()?.extractArtists()
                        val year =
                            subtitleRuns.find { it.text.trim().matches(Regex("""^\d{4}$""")) }
                                ?.text?.trim()?.toIntOrNull()

                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId
                                ?: return null,
                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint?.playlistId
                                ?: renderer.navigationEndpoint.browseEndpoint.browseId,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = artists,
                            year = year,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null
                        )
                    }

                    renderer.isPlaylist -> {
                        val subtitleRuns = renderer.subtitle?.runs.orEmpty()
                        val songCount = subtitleRuns.find { run ->
                            val t = run.text.trim().lowercase()
                            t.contains("track") || t.contains("song") || t.contains("cancion") || t.contains(
                                "canción"
                            ) || t.contains("tema")
                        }?.text?.trim() ?: subtitleRuns.lastOrNull()?.text

                        PlaylistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix(
                                "VL"
                            ) ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = Artist(
                                name = renderer.subtitle?.runs?.firstOrNull()?.text ?: return null,
                                id = null
                            ),
                            songCountText = songCount,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    renderer.isArtist -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId
                                ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            subscriptions = renderer.subtitle?.runs?.firstOrNull()?.text,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                        )
                    }

                    else -> null
                }
            }

            fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): Section? {
                return Section(
                    title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
                    label = null,
                    thumbnail = null,
                    endpoint = renderer.bottomEndpoint?.browseEndpoint,
                    items = renderer.contents?.getItems()?.mapNotNull {
                        fromMusicResponsiveListItemRenderer(it)
                    }?.ifEmpty { return null } ?: return null,
                    sectionType = SectionType.LIST,
                )
            }

            fun fromGridRenderer(renderer: GridRenderer): Section? {
                return Section(
                    title = renderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: return null,
                    label = null,
                    thumbnail = null,
                    endpoint = null,
                    items = renderer.items.mapNotNull { item ->
                        item.musicTwoRowItemRenderer?.let { fromMusicTwoRowItemRenderer(it) }
                    }.ifEmpty { return null },
                    sectionType = SectionType.GRID,
                )
            }

            fun fromMusicTasteBuilderShelfRenderer(renderer: MusicTastebuilderShelfRenderer): Section? {
                return Section(
                    title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
                    label = null,
                    thumbnail = null,
                    endpoint = null,
                    items = renderer.contents?.mapNotNull { itemContainer ->
                        itemContainer.musicResponsiveListItemRenderer?.let {
                            fromMusicResponsiveListItemRenderer(
                                it
                            )
                        }
                    }?.ifEmpty { return null } ?: return null,
                    sectionType = SectionType.LIST,
                )
            }

            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
                val secondaryLine = renderer.flexColumns.getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator()
                    ?: return null
                val thirdLine = renderer.flexColumns.getOrNull(2)
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator()
                    ?: emptyList()
                val allGroups = secondaryLine + thirdLine

                val parsedDuration = renderer.extractDuration()
                    ?: allGroups.flatten()
                        .map { it.text.trim() }
                        .find { it.isTimeDuration() }
                        ?.parseTime()
                    ?: secondaryLine.lastOrNull()?.firstOrNull()?.text?.parseTime()

                val nonDurationGroups = allGroups.filter { group ->
                    val text = group.joinToString("") { it.text }.trim()
                    !text.isInvalidArtistName()
                }

                return when {
                    renderer.isSong -> {
                        val artists =
                            nonDurationGroups.firstOrNull()?.extractArtists()?.ifEmpty { null }
                                ?: nonDurationGroups.firstOrNull()?.map {
                                    Artist(
                                        name = it.text,
                                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                                    )
                                } ?: return null

                        val explicitAlbumRun = allGroups.flatten().find { run ->
                            run.navigationEndpoint?.browseEndpoint?.isAlbumEndpoint == true ||
                                    run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPRE") == true
                        }

                        val album = explicitAlbumRun?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: ""
                            )
                        } ?: run {
                            val candidateGroup = nonDurationGroups.getOrNull(1)
                            val candidateRun = candidateGroup?.firstOrNull()
                            val isArtist =
                                candidateRun?.navigationEndpoint?.browseEndpoint?.isArtistEndpoint == true ||
                                        candidateRun?.navigationEndpoint?.browseEndpoint?.browseId?.startsWith(
                                            "UC"
                                        ) == true
                            if (candidateRun != null && !isArtist) {
                                val albumName = candidateGroup.joinToString("") { it.text }.trim()
                                if (albumName.isNotEmpty()) {
                                    Album(
                                        name = albumName,
                                        id = candidateRun.navigationEndpoint?.browseEndpoint?.browseId
                                            ?: ""
                                    )
                                } else null
                            } else null
                        }

                        SongItem(
                            id = renderer.playlistItemData?.videoId
                                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                                ?: return null,
                            title = renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
                                ?: return null,
                            artists = artists,
                            album = album,
                            duration = parsedDuration,
                            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            musicVideoType = renderer.musicVideoType,
                            explicit = renderer.badges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                            endpoint = renderer.navigationEndpoint?.watchEndpoint
                                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                        )
                    }

                    renderer.isArtist -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint?.browseEndpoint?.browseId
                                ?: return null,
                            title = renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
                                ?: return null,
                            subscriptions = null,
                            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items
                                ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                                ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items
                                .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                                ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                        )
                    }

                    renderer.isAlbum -> {
                        val year = allGroups.flatten()
                            .find { it.text.trim().matches(Regex("""^\d{4}$""")) }
                            ?.text?.trim()?.toIntOrNull()
                        val artists = nonDurationGroups.firstOrNull()?.extractArtists()

                        AlbumItem(
                            browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId
                                ?: return null,
                            playlistId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint?.anyWatchEndpoint?.playlistId
                                ?: renderer.navigationEndpoint.browseEndpoint.browseId
                                ?: "",
                            title = renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
                                ?: return null,
                            artists = artists,
                            year = year,
                            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            explicit = renderer.badges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                        )
                    }

                    renderer.isPlaylist -> {
                        val author = nonDurationGroups.firstOrNull()?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        }
                        val songCount = allGroups.flatten().find { run ->
                            val t = run.text.trim().lowercase()
                            t.contains("track") || t.contains("song") || t.contains("cancion") || t.contains(
                                "canción"
                            ) || t.contains("tema")
                        }?.text ?: renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.lastOrNull()?.text

                        PlaylistItem(
                            id = renderer.navigationEndpoint?.browseEndpoint?.browseId?.removePrefix(
                                "VL"
                            ) ?: return null,
                            title = renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
                                ?: return null,
                            author = author,
                            songCountText = songCount,
                            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            playEndpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items
                                ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                                ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items
                                .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                                ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                        )
                    }

                    else -> null
                }
            }
        }
    }

    enum class SectionType {
        LIST, GRID
    }
}