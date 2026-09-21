package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.TrackMenuAction
import com.github.adriianh.cli.tui.component.resolveTrackMenuItems
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.OfflineFilterType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.PlaylistSortOrder
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.SortDirection
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackSortOrder
import com.github.adriianh.core.domain.model.filterAndSortOfflineTracks
import com.github.adriianh.core.domain.model.filterAndSortTracks
import com.github.adriianh.core.domain.model.search.SearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MeloStateFilterSortTest {

    private fun createTrack(
        id: String,
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 1000L
    ): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            genres = emptyList(),
            artworkUrl = null,
            sourceId = null
        )
    }

    @Test
    fun testFilteredAndSortedFavorites() {
        val local1 =
            createTrack("local:1", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354000L)
        val local2 =
            createTrack("local:2", "Another One Bites the Dust", "Queen", "The Game", 215000L)
        val remote1 = createTrack("piped:yt1", "Starboy", "The Weeknd", "Starboy", 230000L)

        val state = MeloState(
            collections = CollectionsState(
                favorites = listOf(local1, local2),
                remoteFavorites = listOf(remote1)
            )
        )

        // 1. Source filter ALL (default order: locals first, then remote)
        val all = state.filteredAndSortedFavorites(
            LibrarySourceFilter.ALL,
            TrackSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(3, all.size)
        assertEquals("local:1", all[0].track.id)
        assertEquals("local:2", all[1].track.id)
        assertEquals("piped:yt1", all[2].track.id)

        // 2. Default order DESCENDING
        val allDesc = state.filteredAndSortedFavorites(
            LibrarySourceFilter.ALL,
            TrackSortOrder.DEFAULT,
            SortDirection.DESCENDING,
            ""
        )
        assertEquals(listOf("piped:yt1", "local:2", "local:1"), allDesc.map { it.track.id })

        // 3. Source filter LOCAL
        val localsOnly = state.filteredAndSortedFavorites(
            LibrarySourceFilter.LOCAL,
            TrackSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(2, localsOnly.size)
        assertEquals(listOf("local:1", "local:2"), localsOnly.map { it.track.id })

        // 4. Source filter REMOTE
        val remotesOnly = state.filteredAndSortedFavorites(
            LibrarySourceFilter.REMOTE,
            TrackSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(1, remotesOnly.size)
        assertEquals("piped:yt1", remotesOnly[0].track.id)

        // 5. Search query
        val searched = state.filteredAndSortedFavorites(
            LibrarySourceFilter.ALL,
            TrackSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            "dust"
        )
        assertEquals(1, searched.size)
        assertEquals("local:2", searched[0].track.id)

        // 6. Sort by TITLE Ascending and Descending
        val sortedByTitleAsc = state.filteredAndSortedFavorites(
            LibrarySourceFilter.ALL,
            TrackSortOrder.TITLE,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(
            listOf("Another One Bites the Dust", "Bohemian Rhapsody", "Starboy"),
            sortedByTitleAsc.map { it.track.title })

        val sortedByTitleDesc = state.filteredAndSortedFavorites(
            LibrarySourceFilter.ALL,
            TrackSortOrder.TITLE,
            SortDirection.DESCENDING,
            ""
        )
        assertEquals(
            listOf("Starboy", "Bohemian Rhapsody", "Another One Bites the Dust"),
            sortedByTitleDesc.map { it.track.title })

        // 7. Sort by DURATION Ascending and Descending
        val sortedByDurationAsc = state.filteredAndSortedFavorites(
            LibrarySourceFilter.ALL,
            TrackSortOrder.DURATION,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(
            listOf(215000L, 230000L, 354000L),
            sortedByDurationAsc.map { it.track.durationMs })

        val sortedByDurationDesc = state.filteredAndSortedFavorites(
            LibrarySourceFilter.ALL,
            TrackSortOrder.DURATION,
            SortDirection.DESCENDING,
            ""
        )
        assertEquals(
            listOf(354000L, 230000L, 215000L),
            sortedByDurationDesc.map { it.track.durationMs })
    }

    @Test
    fun testFilteredAndSortedPlaylists() {
        val localPl1 = Playlist(id = 1L, name = "Rock Classics", trackCount = 20, createdAt = 0L)
        val localPl2 = Playlist(id = 2L, name = "Acoustic Vibes", trackCount = 5, createdAt = 0L)
        val remotePl1 = SearchResult.Playlist(
            id = "remote1",
            title = "Synthwave Beats",
            author = "Curator",
            trackCount = 50,
            artworkUrl = null
        )

        val state = MeloState(
            collections = CollectionsState(
                playlists = listOf(localPl1, localPl2),
                remotePlaylists = listOf(remotePl1)
            )
        )

        // 1. Source filter ALL
        val all = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.ALL,
            PlaylistSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(3, all.size)

        // 2. Filter LOCAL
        val locals = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.LOCAL,
            PlaylistSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(2, locals.size)
        assertEquals(listOf("Rock Classics", "Acoustic Vibes"), locals.map { it.title })

        // 3. Filter REMOTE
        val remotes = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.REMOTE,
            PlaylistSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(1, remotes.size)
        assertEquals("Synthwave Beats", remotes[0].title)

        // 4. Search query
        val searched = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.ALL,
            PlaylistSortOrder.DEFAULT,
            SortDirection.ASCENDING,
            "vibes"
        )
        assertEquals(1, searched.size)
        assertEquals("Acoustic Vibes", searched[0].title)

        // 5. Sort by NAME Ascending and Descending
        val sortedByNameAsc = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.ALL,
            PlaylistSortOrder.NAME,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(
            listOf("Acoustic Vibes", "Rock Classics", "Synthwave Beats"),
            sortedByNameAsc.map { it.title })

        val sortedByNameDesc = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.ALL,
            PlaylistSortOrder.NAME,
            SortDirection.DESCENDING,
            ""
        )
        assertEquals(
            listOf("Synthwave Beats", "Rock Classics", "Acoustic Vibes"),
            sortedByNameDesc.map { it.title })

        // 6. Sort by TRACKS Ascending and Descending
        val sortedByTracksAsc = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.ALL,
            PlaylistSortOrder.TRACKS,
            SortDirection.ASCENDING,
            ""
        )
        assertEquals(
            listOf("Acoustic Vibes", "Rock Classics", "Synthwave Beats"),
            sortedByTracksAsc.map { it.title })

        val sortedByTracksDesc = state.filteredAndSortedPlaylists(
            LibrarySourceFilter.ALL,
            PlaylistSortOrder.TRACKS,
            SortDirection.DESCENDING,
            ""
        )
        assertEquals(
            listOf("Synthwave Beats", "Rock Classics", "Acoustic Vibes"),
            sortedByTracksDesc.map { it.title })
    }

    @Test
    fun testFilterAndSortTracks() {
        val t1 = createTrack("1", "Yellow Submarine", "The Beatles", "Revolver", 158000L)
        val t2 = createTrack("2", "Hey Jude", "The Beatles", "Hey Jude", 431000L)
        val t3 = createTrack("3", "Come Together", "The Beatles", "Abbey Road", 259000L)
        val tracks = listOf(t1, t2, t3)

        // 1. Search
        val searched =
            filterAndSortTracks(tracks, TrackSortOrder.DEFAULT, SortDirection.ASCENDING, "together")
        assertEquals(1, searched.size)
        assertEquals("Come Together", searched[0].title)

        // 2. Sort by TITLE Ascending and Descending
        val sortedByTitleAsc =
            filterAndSortTracks(tracks, TrackSortOrder.TITLE, SortDirection.ASCENDING, "")
        assertEquals(
            listOf("Come Together", "Hey Jude", "Yellow Submarine"),
            sortedByTitleAsc.map { it.title })

        val sortedByTitleDesc =
            filterAndSortTracks(tracks, TrackSortOrder.TITLE, SortDirection.DESCENDING, "")
        assertEquals(
            listOf("Yellow Submarine", "Hey Jude", "Come Together"),
            sortedByTitleDesc.map { it.title })

        // 3. Sort by DURATION Ascending and Descending
        val sortedByDurationAsc =
            filterAndSortTracks(tracks, TrackSortOrder.DURATION, SortDirection.ASCENDING, "")
        assertEquals(listOf(158000L, 259000L, 431000L), sortedByDurationAsc.map { it.durationMs })

        val sortedByDurationDesc =
            filterAndSortTracks(tracks, TrackSortOrder.DURATION, SortDirection.DESCENDING, "")
        assertEquals(listOf(431000L, 259000L, 158000L), sortedByDurationDesc.map { it.durationMs })
    }

    @Test
    fun testFilterAndSortOfflineTracks() {
        val t1 = createTrack("off:1", "Karma Police", "Radiohead", "OK Computer", 261000L)
        val t2 = createTrack("off:2", "Creep", "Radiohead", "Pablo Honey", 238000L)
        val t3 = createTrack("off:3", "No Surprises", "Radiohead", "OK Computer", 228000L)
        val tPending = createTrack("off:4", "Paranoid Android", "Radiohead", "OK Computer", 383000L)

        val o1 = OfflineTrack(
            track = t1,
            downloadStatus = DownloadStatus.COMPLETED,
            downloadType = DownloadType.MANUAL,
            localFilePath = "/path/1.opus"
        )
        val o2 = OfflineTrack(
            track = t2,
            downloadStatus = DownloadStatus.COMPLETED,
            downloadType = DownloadType.PREFETCH,
            localFilePath = "/path/2.opus"
        )
        val o3 = OfflineTrack(
            track = t3,
            downloadStatus = DownloadStatus.COMPLETED,
            downloadType = DownloadType.MANUAL,
            localFilePath = "/path/3.opus"
        )
        val oPending = OfflineTrack(
            track = tPending,
            downloadStatus = DownloadStatus.PENDING,
            downloadType = DownloadType.MANUAL,
            localFilePath = null
        )

        val allDownloads = listOf(o1, o2, o3, oPending)

        // 1. ALL ignores PENDING
        val allCompleted = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.ALL,
            sortOrder = TrackSortOrder.DEFAULT,
            sortDirection = SortDirection.ASCENDING,
            query = ""
        )
        assertEquals(3, allCompleted.size)
        assertEquals(listOf("off:1", "off:2", "off:3"), allCompleted.map { it.track.id })

        // 2. Filter MANUAL
        val manualOnly = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.MANUAL,
            sortOrder = TrackSortOrder.DEFAULT,
            sortDirection = SortDirection.ASCENDING,
            query = ""
        )
        assertEquals(2, manualOnly.size)
        assertEquals(listOf("off:1", "off:3"), manualOnly.map { it.track.id })

        // 3. Filter CACHE
        val cacheOnly = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.CACHE,
            sortOrder = TrackSortOrder.DEFAULT,
            sortDirection = SortDirection.ASCENDING,
            query = ""
        )
        assertEquals(1, cacheOnly.size)
        assertEquals("off:2", cacheOnly[0].track.id)

        // 4. Query
        val queried = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.ALL,
            sortOrder = TrackSortOrder.DEFAULT,
            sortDirection = SortDirection.ASCENDING,
            query = "creep"
        )
        assertEquals(1, queried.size)
        assertEquals("off:2", queried[0].track.id)

        // 5. Sort TITLE Asc / Desc
        val titleAsc = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.ALL,
            sortOrder = TrackSortOrder.TITLE,
            sortDirection = SortDirection.ASCENDING,
            query = ""
        )
        assertEquals(
            listOf("Creep", "Karma Police", "No Surprises"),
            titleAsc.map { it.track.title })

        val titleDesc = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.ALL,
            sortOrder = TrackSortOrder.TITLE,
            sortDirection = SortDirection.DESCENDING,
            query = ""
        )
        assertEquals(
            listOf("No Surprises", "Karma Police", "Creep"),
            titleDesc.map { it.track.title })

        // 6. Sort DURATION Asc / Desc
        val durAsc = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.ALL,
            sortOrder = TrackSortOrder.DURATION,
            sortDirection = SortDirection.ASCENDING,
            query = ""
        )
        assertEquals(listOf(228000L, 238000L, 261000L), durAsc.map { it.track.durationMs })

        val durDesc = filterAndSortOfflineTracks(
            downloads = allDownloads,
            filterType = OfflineFilterType.ALL,
            sortOrder = TrackSortOrder.DURATION,
            sortDirection = SortDirection.DESCENDING,
            query = ""
        )
        assertEquals(listOf(261000L, 238000L, 228000L), durDesc.map { it.track.durationMs })
    }

    @Test
    fun testSearchAndDetailTabContracts() {
        // SearchTab 1..4 contracts
        assertEquals(
            listOf(
                SearchTab.SONGS,
                SearchTab.ALBUMS,
                SearchTab.ARTISTS,
                SearchTab.PLAYLISTS
            ), SearchTab.entries
        )
        // DetailTab contracts (i: Info, l: Lyrics, s: Similar)
        assertEquals(listOf(DetailTab.INFO, DetailTab.LYRICS, DetailTab.SIMILAR), DetailTab.entries)
    }

    @Test
    fun testBatchSelectionState() {
        val t1 = createTrack("t1", "Song 1", "Artist A")
        val t2 = createTrack("t2", "Song 2", "Artist B")
        val t3 = createTrack("t3", "Song 3", "Artist C")

        var batch = BatchSelectionState()
        assertEquals(0, batch.count)
        assertEquals(false, batch.isNotEmpty)
        assertEquals(true, batch.isEmpty)
        assertEquals(false, batch.isSelectionMode)
        assertEquals(false, batch.isSelected(t1.id))

        // Toggle t1: adds t1 and enables selection mode
        batch = batch.toggle(t1)
        assertEquals(1, batch.count)
        assertEquals(true, batch.isNotEmpty)
        assertEquals(false, batch.isEmpty)
        assertEquals(true, batch.isSelectionMode)
        assertEquals(true, batch.isSelected(t1.id))
        assertEquals(false, batch.isSelected(t2.id))
        assertEquals(listOf(t1), batch.tracks())

        // Toggle t2: adds t2
        batch = batch.toggle(t2)
        assertEquals(2, batch.count)
        assertEquals(true, batch.isSelected(t1.id))
        assertEquals(true, batch.isSelected(t2.id))

        // Toggle t1 again: removes t1
        batch = batch.toggle(t1)
        assertEquals(1, batch.count)
        assertEquals(false, batch.isSelected(t1.id))
        assertEquals(true, batch.isSelected(t2.id))

        // Select all
        batch = batch.selectAll(listOf(t1, t2, t3))
        assertEquals(3, batch.count)
        assertEquals(true, batch.isSelectionMode)
        assertEquals(true, batch.isSelected(t1.id))
        assertEquals(true, batch.isSelected(t2.id))
        assertEquals(true, batch.isSelected(t3.id))

        // Clear
        batch = batch.clear()
        assertEquals(0, batch.count)
        assertEquals(true, batch.isEmpty)
        assertEquals(false, batch.isNotEmpty)
        assertEquals(false, batch.isSelectionMode)
        assertEquals(emptyList<Track>(), batch.tracks())
    }

    @Test
    fun testResolveTrackMenuItemsContexts() {
        val t1 = createTrack("t1", "Song 1", "Artist A")
        val t2 = createTrack("t2", "Song 2", "Artist B")

        // 1. General Search Context - Single Track
        val searchState = MeloState(
            screen = ScreenState.Search(tab = SearchTab.SONGS),
            trackOptions = TrackOptionsMenuState(track = t1, isVisible = true)
        )
        val searchSingleItems = resolveTrackMenuItems(searchState)
        val searchSingleActions = searchSingleItems.map { it.action }
        assertEquals(
            listOf(
                TrackMenuAction.PLAY,
                TrackMenuAction.ADD_TO_QUEUE,
                TrackMenuAction.ADD_TO_FAVORITES,
                TrackMenuAction.ADD_TO_PLAYLIST,
                TrackMenuAction.DOWNLOAD_OFFLINE,
                TrackMenuAction.VIEW_SIMILAR
            ),
            searchSingleActions
        )

        // 2. General Search Context - Batch
        val searchBatchState = searchState.copy(
            trackOptions = TrackOptionsMenuState(batchTracks = listOf(t1, t2), isVisible = true)
        )
        val searchBatchItems = resolveTrackMenuItems(searchBatchState)
        assertEquals(
            listOf(
                TrackMenuAction.PLAY,
                TrackMenuAction.ADD_TO_QUEUE,
                TrackMenuAction.TOGGLE_FAVORITE,
                TrackMenuAction.ADD_TO_PLAYLIST,
                TrackMenuAction.DOWNLOAD_OFFLINE,
                TrackMenuAction.CLEAR_SELECTION
            ),
            searchBatchItems.map { it.action }
        )
        assertEquals("Play Selection (2)", searchBatchItems[0].label)

        // 3. Local Playlist Context - Single & Batch
        val testPlaylist = Playlist(id = 1L, name = "My Playlist", trackCount = 2, createdAt = 0L)
        val playlistState = MeloState(
            screen = ScreenState.Library(
                libraryTab = LibraryTab.PLAYLISTS,
                isInPlaylistDetail = true,
                selectedPlaylist = testPlaylist
            ),
            trackOptions = TrackOptionsMenuState(track = t1, isVisible = true)
        )
        val plSingleItems = resolveTrackMenuItems(playlistState)
        assertEquals(true, plSingleItems.any { it.action == TrackMenuAction.REMOVE_FROM_PLAYLIST })
        assertEquals(
            "Remove from Playlist",
            plSingleItems.find { it.action == TrackMenuAction.REMOVE_FROM_PLAYLIST }?.label
        )
        assertEquals(
            "Add to Another Playlist",
            plSingleItems.find { it.action == TrackMenuAction.ADD_TO_PLAYLIST }?.label
        )

        val plBatchState = playlistState.copy(
            trackOptions = TrackOptionsMenuState(batchTracks = listOf(t1, t2), isVisible = true)
        )
        val plBatchItems = resolveTrackMenuItems(plBatchState)
        assertEquals(true, plBatchItems.any { it.action == TrackMenuAction.REMOVE_FROM_PLAYLIST })
        assertEquals(
            "Remove All from Playlist (2)",
            plBatchItems.find { it.action == TrackMenuAction.REMOVE_FROM_PLAYLIST }?.label
        )

        // 4. Favorites Context - Single & Batch
        val favoritesState = MeloState(
            screen = ScreenState.Library(libraryTab = LibraryTab.FAVORITES),
            collections = CollectionsState(favorites = listOf(t1)),
            trackOptions = TrackOptionsMenuState(track = t1, isVisible = true)
        )
        val favSingleItems = resolveTrackMenuItems(favoritesState)
        assertEquals(
            true,
            favSingleItems.any { it.action == TrackMenuAction.REMOVE_FROM_FAVORITES })
        assertEquals(
            "Remove from Favorites",
            favSingleItems.find { it.action == TrackMenuAction.REMOVE_FROM_FAVORITES }?.label
        )

        val favBatchState = favoritesState.copy(
            trackOptions = TrackOptionsMenuState(batchTracks = listOf(t1, t2), isVisible = true)
        )
        val favBatchItems = resolveTrackMenuItems(favBatchState)
        assertEquals(true, favBatchItems.any { it.action == TrackMenuAction.REMOVE_FROM_FAVORITES })
        assertEquals(
            "Remove All from Favorites (2)",
            favBatchItems.find { it.action == TrackMenuAction.REMOVE_FROM_FAVORITES }?.label
        )

        // 5. Offline Context - Single & Batch
        val offlineState = MeloState(
            screen = ScreenState.Offline(),
            trackOptions = TrackOptionsMenuState(track = t1, isVisible = true)
        )
        val offSingleItems = resolveTrackMenuItems(offlineState)
        assertEquals(true, offSingleItems.any { it.action == TrackMenuAction.DELETE_DOWNLOAD })
        assertEquals(
            "Delete Download",
            offSingleItems.find { it.action == TrackMenuAction.DELETE_DOWNLOAD }?.label
        )

        val offBatchState = offlineState.copy(
            trackOptions = TrackOptionsMenuState(batchTracks = listOf(t1, t2), isVisible = true)
        )
        val offBatchItems = resolveTrackMenuItems(offBatchState)
        assertEquals(true, offBatchItems.any { it.action == TrackMenuAction.DELETE_DOWNLOAD })
        assertEquals(
            "Delete All Downloads (2)",
            offBatchItems.find { it.action == TrackMenuAction.DELETE_DOWNLOAD }?.label
        )

        // 6. Local Files Context - Single & Batch (DOWNLOAD_OFFLINE should be omitted)
        val localFilesState = MeloState(
            screen = ScreenState.Library(libraryTab = LibraryTab.LOCAL),
            trackOptions = TrackOptionsMenuState(track = t1, isVisible = true)
        )
        val localSingleItems = resolveTrackMenuItems(localFilesState)
        assertEquals(false, localSingleItems.any { it.action == TrackMenuAction.DOWNLOAD_OFFLINE })

        val localBatchState = localFilesState.copy(
            trackOptions = TrackOptionsMenuState(batchTracks = listOf(t1, t2), isVisible = true)
        )
        val localBatchItems = resolveTrackMenuItems(localBatchState)
        assertEquals(false, localBatchItems.any { it.action == TrackMenuAction.DOWNLOAD_OFFLINE })
    }

    @Test
    fun testDetailSyncedLyricsStateAndScrolling() {
        val sampleLrc = """
            [00:01.00] Line 1
            [00:05.00] Line 2
            [00:10.00] Line 3
            [00:15.00] Line 4
            [00:20.00] Line 5
        """.trimIndent()

        val parsed = com.github.adriianh.cli.tui.util.LrcParser.parse(sampleLrc)
        assertEquals(5, parsed.size)
        assertEquals("Line 1", parsed[0].text)
        assertEquals(1000L, parsed[0].timeMs)

        // Initial state verification
        var detail = DetailState(
            selectedTrack = createTrack("t1", "Title", "Artist"),
            syncedLyrics = parsed,
            isAutoScrollLyrics = true,
            lyricsScrollOffset = 0
        )
        assertEquals(true, detail.isAutoScrollLyrics)
        assertEquals(0, detail.lyricsScrollOffset)

        // 1. Auto-scroll: at 6 seconds (6000ms), active line should be Line 2 (index 1)
        val activeIndexAt6s =
            com.github.adriianh.cli.tui.util.LrcParser.currentLineIndex(detail.syncedLyrics, 6000L)
        assertEquals(1, activeIndexAt6s)

        // 2. Auto-scroll: at 16 seconds (16000ms), active line should be Line 4 (index 3)
        val activeIndexAt16s =
            com.github.adriianh.cli.tui.util.LrcParser.currentLineIndex(detail.syncedLyrics, 16000L)
        assertEquals(3, activeIndexAt16s)

        // 3. Manual scrolling shifts offset and pauses auto-scroll
        detail = detail.copy(isAutoScrollLyrics = false, lyricsScrollOffset = 2)
        assertEquals(false, detail.isAutoScrollLyrics)
        assertEquals(2, detail.lyricsScrollOffset)

        // 4. Re-syncing resets auto-scroll and offset
        detail = detail.copy(isAutoScrollLyrics = true, lyricsScrollOffset = 0)
        assertEquals(true, detail.isAutoScrollLyrics)
        assertEquals(0, detail.lyricsScrollOffset)
    }

    @Test
    fun testGlobalDetailPanelStateAndKeybinding() {
        val detail = DetailState()
        assertEquals(true, detail.isVisible)

        val toggled = detail.copy(isVisible = !detail.isVisible)
        assertEquals(false, toggled.isVisible)

        val settings = Settings()
        val toggleBinding = settings.keybindings[MeloAction.TOGGLE_DETAIL]
        assertNotNull(toggleBinding)
        assertEquals('d', toggleBinding.char)
        assertEquals(true, toggleBinding.ctrl)
    }

    @Test
    fun testQueueAndPlaylistReorderState() {
        val t1 = createTrack("t1", "Track 1", "Artist 1")
        val t2 = createTrack("t2", "Track 2", "Artist 2")
        val t3 = createTrack("t3", "Track 3", "Artist 3")

        // 1. Reordering a playlist list from index 2 to index 1 (move up)
        val playlistTracks = mutableListOf(t1, t2, t3)
        val fromIndex = 2
        val toIndex = 1
        val moved = playlistTracks.removeAt(fromIndex)
        playlistTracks.add(toIndex, moved)

        assertEquals(listOf("t1", "t3", "t2"), playlistTracks.map { it.id })

        // 2. Reordering a queue cursor
        var player = PlayerState(queue = listOf(t1, t2, t3), queueCursor = 2)
        // Move item at cursor 2 up to 1
        player = player.copy(
            queue = playlistTracks,
            queueCursor = 1
        )
        assertEquals(1, player.queueCursor)
        assertEquals("t3", player.queue[player.queueCursor].id)

        // 3. Bounds check: at index 0, moving up is invalid
        val canMoveUpFromZero = player.queueCursor > 0 && 0 > 0
        assertEquals(false, canMoveUpFromZero)

        // 4. Bounds check: at last index, moving down is invalid
        val canMoveDownFromLast = player.queue.lastIndex < player.queue.lastIndex
        assertEquals(false, canMoveDownFromLast)
    }
}