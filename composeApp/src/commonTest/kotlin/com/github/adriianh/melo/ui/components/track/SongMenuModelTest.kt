package com.github.adriianh.melo.ui.components.track

import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.track_menu_add_to_likes
import com.github.adriianh.melo.resources.track_menu_remove_download
import com.github.adriianh.melo.resources.track_menu_remove_from_likes
import com.github.adriianh.melo.resources.track_menu_remove_from_queue
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.util.DarkMeloColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SongMenuModelTest {
    private val sampleTrack =
        Track(
            id = "yt:abc123",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            durationMs = 243_000L,
            genres = emptyList(),
            artworkUrl = null,
            sourceId = null,
        )

    @Test
    fun `default context produces playback, library, navigation, and share groups with dividers`() {
        val entries =
            buildSongMenuEntries(
                track = sampleTrack,
                context =
                    SongMenuContext(
                        onDownload = {},
                        onGoToArtist = {},
                        onGoToAlbum = {},
                        onShare = {},
                    ),
            )

        val ids =
            entries.map { entry ->
                when (entry) {
                    is SongMenuEntry.Action -> entry.id
                    is SongMenuEntry.Submenu -> entry.id
                    SongMenuEntry.Divider -> "|"
                }
            }

        assertEquals(
            listOf(
                "play_next",
                "add_to_queue",
                "|",
                "toggle_like",
                "add_to_playlist",
                "download",
                "|",
                "go_to_artist",
                "go_to_album",
                "|",
                "share",
            ),
            ids,
        )
    }

    @Test
    fun `state-aware labels and tints update for liked, downloaded, and queued tracks`() {
        val entries =
            buildSongMenuEntries(
                track = sampleTrack,
                context =
                    SongMenuContext(
                        isLiked = true,
                        isDownloaded = true,
                        inQueue = true,
                        onRemoveFromQueue = {},
                        onDeleteDownload = {},
                        onRemoveFromPlaylist = {},
                    ),
            )

        val queueEntry = entries.filterIsInstance<SongMenuEntry.Action>().first { it.id == "remove_from_queue" }
        assertEquals(UiText.Resource(Res.string.track_menu_remove_from_queue), queueEntry.label)
        assertEquals(DarkMeloColors.menuQueueTint, queueEntry.tint)

        val likeEntry = entries.filterIsInstance<SongMenuEntry.Action>().first { it.id == "toggle_like" }
        assertEquals(UiText.Resource(Res.string.track_menu_remove_from_likes), likeEntry.label)
        assertEquals(DarkMeloColors.menuLikeTint, likeEntry.tint)

        val downloadEntry = entries.filterIsInstance<SongMenuEntry.Action>().first { it.id == "remove_download" }
        assertEquals(UiText.Resource(Res.string.track_menu_remove_download), downloadEntry.label)
        assertEquals(SongMenuEntryStyle.Destructive, downloadEntry.style)
        assertEquals(DarkMeloColors.menuDestructive, downloadEntry.tint)

        val lastEntry = assertIs<SongMenuEntry.Action>(entries.last())
        assertEquals("remove_from_playlist", lastEntry.id)
        assertEquals(SongMenuEntryStyle.Destructive, lastEntry.style)
    }

    @Test
    fun `hides go to artist and go to album when already on those screens`() {
        val entries =
            buildSongMenuEntries(
                track = sampleTrack,
                context =
                    SongMenuContext(
                        isOnArtistScreen = true,
                        isOnAlbumScreen = true,
                        onGoToArtist = {},
                        onGoToAlbum = {},
                    ),
            )

        val ids = entries.filterIsInstance<SongMenuEntry.Action>().map { it.id }
        assertFalse("go_to_artist" in ids)
        assertFalse("go_to_album" in ids)
    }

    @Test
    fun `unliked and local track hides download and shows add to likes`() {
        val localTrack = sampleTrack.copy(id = "local:42")
        val entries =
            buildSongMenuEntries(
                track = localTrack,
                context =
                    SongMenuContext(
                        isLiked = false,
                        onDownload = {},
                    ),
            )

        val actions = entries.filterIsInstance<SongMenuEntry.Action>()
        assertFalse(actions.any { it.id == "download" })
        val likeEntry = actions.first { it.id == "toggle_like" }
        assertEquals(UiText.Resource(Res.string.track_menu_add_to_likes), likeEntry.label)
    }

    @Test
    fun `window size classification matches breakpoints`() {
        assertEquals(SongMenuWindowSize.Compact, songMenuWindowSizeForWidth(360.dp))
        assertEquals(SongMenuWindowSize.Compact, songMenuWindowSizeForWidth(599.dp))
        assertEquals(SongMenuWindowSize.Medium, songMenuWindowSizeForWidth(600.dp))
        assertEquals(SongMenuWindowSize.Medium, songMenuWindowSizeForWidth(839.dp))
        assertEquals(SongMenuWindowSize.Expanded, songMenuWindowSizeForWidth(840.dp))
        assertTrue(songMenuWindowSizeForWidth(1280.dp) == SongMenuWindowSize.Expanded)
    }
}
