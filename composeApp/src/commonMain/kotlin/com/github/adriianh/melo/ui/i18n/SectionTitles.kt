@file:Suppress("MatchingDeclarationName")

package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.section_albums
import com.github.adriianh.melo.resources.section_artists
import com.github.adriianh.melo.resources.section_cached_music
import com.github.adriianh.melo.resources.section_downloads
import com.github.adriianh.melo.resources.section_local_files
import com.github.adriianh.melo.resources.section_recently_played
import com.github.adriianh.melo.resources.section_songs
import com.github.adriianh.melo.resources.section_trending
import org.jetbrains.compose.resources.stringResource

object SectionTitleKeys {
    const val TRENDING = "trending"
    const val RECENTLY_PLAYED = "recently_played"
    const val DOWNLOADS = "downloads"
    const val CACHED_MUSIC = "cached_music"
    const val LOCAL_FILES = "local_files"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
}

@Composable
fun HomeSection.displayTitle(): String =
    when (titleKey) {
        SectionTitleKeys.TRENDING -> stringResource(Res.string.section_trending)
        SectionTitleKeys.RECENTLY_PLAYED -> stringResource(Res.string.section_recently_played)
        SectionTitleKeys.DOWNLOADS -> stringResource(Res.string.section_downloads)
        SectionTitleKeys.CACHED_MUSIC -> stringResource(Res.string.section_cached_music)
        SectionTitleKeys.LOCAL_FILES -> stringResource(Res.string.section_local_files)
        SectionTitleKeys.SONGS -> stringResource(Res.string.section_songs)
        SectionTitleKeys.ALBUMS -> stringResource(Res.string.section_albums)
        SectionTitleKeys.ARTISTS -> stringResource(Res.string.section_artists)
        else -> title
    }
