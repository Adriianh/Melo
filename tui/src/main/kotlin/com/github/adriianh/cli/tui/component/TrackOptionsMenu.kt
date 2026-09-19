package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.HomeTab
import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

enum class TrackMenuAction {
    PLAY,
    ADD_TO_QUEUE,
    ADD_TO_PLAYLIST,
    ADD_TO_FAVORITES,
    REMOVE_FROM_FAVORITES,
    TOGGLE_FAVORITE,
    REMOVE_FROM_PLAYLIST,
    DOWNLOAD_OFFLINE,
    DELETE_DOWNLOAD,
    VIEW_SIMILAR,
    CLEAR_SELECTION
}

/**
 * Visual grouping used to separate related actions in the overlay.
 */
enum class TrackMenuGroup(val title: String) {
    PLAYBACK("Playback"),
    LIBRARY("Library"),
    DOWNLOAD("Download"),
    MORE("More"),
}

data class TrackMenuItem(
    val action: TrackMenuAction,
    val label: String,
    val group: TrackMenuGroup,
)

/**
 * Screen-independent snapshot of the state the menu is rendered for.
 * Pure data — makes [resolveTrackMenuItems] a trivial filter over [TrackMenuSpec].
 */
data class TrackMenuContext(
    val isBatch: Boolean,
    val count: Int,
    val isFav: Boolean,
    val isManualDownloaded: Boolean,
    val isDownloading: Boolean,
    val isLocalPlaylist: Boolean,
    val isFavorites: Boolean,
    val isOffline: Boolean,
    val isLocalFiles: Boolean,
)

/**
 * Declarative menu entry: one line defines visibility, label and group.
 * Replaces the previous duplicated when-branches.
 */
private data class TrackMenuSpec(
    val action: TrackMenuAction,
    val label: (TrackMenuContext) -> String,
    val group: TrackMenuGroup,
    val visible: (TrackMenuContext) -> Boolean = { true },
)

private val SINGLE_TRACK_SPECS = listOf(
    TrackMenuSpec(TrackMenuAction.PLAY, { "Play Now" }, TrackMenuGroup.PLAYBACK),
    TrackMenuSpec(TrackMenuAction.ADD_TO_QUEUE, { "Add to Queue" }, TrackMenuGroup.PLAYBACK),

    TrackMenuSpec(
        TrackMenuAction.ADD_TO_FAVORITES,
        { "Add to Favorites" },
        TrackMenuGroup.LIBRARY,
        visible = { !it.isFav },
    ),
    TrackMenuSpec(
        TrackMenuAction.REMOVE_FROM_FAVORITES,
        { "Remove from Favorites" },
        TrackMenuGroup.LIBRARY,
        visible = { it.isFav },
    ),
    TrackMenuSpec(
        TrackMenuAction.ADD_TO_PLAYLIST,
        { if (it.isLocalPlaylist) "Add to Another Playlist" else "Add to Playlist" },
        TrackMenuGroup.LIBRARY,
    ),
    TrackMenuSpec(
        TrackMenuAction.REMOVE_FROM_PLAYLIST,
        { "Remove from Playlist" },
        TrackMenuGroup.LIBRARY,
        visible = { it.isLocalPlaylist },
    ),

    TrackMenuSpec(
        TrackMenuAction.DOWNLOAD_OFFLINE,
        { "Download for Offline" },
        TrackMenuGroup.DOWNLOAD,
        visible = { !it.isOffline && !it.isLocalFiles && !it.isManualDownloaded && !it.isDownloading },
    ),
    TrackMenuSpec(
        TrackMenuAction.DELETE_DOWNLOAD,
        { "Remove from Offline" },
        TrackMenuGroup.DOWNLOAD,
        visible = { !it.isOffline && !it.isLocalFiles && it.isManualDownloaded },
    ),
    TrackMenuSpec(
        TrackMenuAction.DELETE_DOWNLOAD,
        { "Delete Download" },
        TrackMenuGroup.DOWNLOAD,
        visible = { it.isOffline },
    ),

    TrackMenuSpec(TrackMenuAction.VIEW_SIMILAR, { "View Similar Tracks" }, TrackMenuGroup.MORE),
)

private val BATCH_TRACK_SPECS = listOf(
    TrackMenuSpec(
        TrackMenuAction.PLAY,
        { "Play Selection (${it.count})" },
        TrackMenuGroup.PLAYBACK
    ),
    TrackMenuSpec(TrackMenuAction.ADD_TO_QUEUE, { "Add All to Queue" }, TrackMenuGroup.PLAYBACK),

    TrackMenuSpec(
        TrackMenuAction.TOGGLE_FAVORITE,
        { "Toggle Favorite" },
        TrackMenuGroup.LIBRARY,
        visible = { !it.isFavorites },
    ),
    TrackMenuSpec(
        TrackMenuAction.REMOVE_FROM_FAVORITES,
        { "Remove All from Favorites (${it.count})" },
        TrackMenuGroup.LIBRARY,
        visible = { it.isFavorites },
    ),
    TrackMenuSpec(
        TrackMenuAction.ADD_TO_PLAYLIST,
        { if (it.isLocalPlaylist) "Add All to Another Playlist" else "Add All to Playlist" },
        TrackMenuGroup.LIBRARY,
    ),
    TrackMenuSpec(
        TrackMenuAction.REMOVE_FROM_PLAYLIST,
        { "Remove All from Playlist (${it.count})" },
        TrackMenuGroup.LIBRARY,
        visible = { it.isLocalPlaylist },
    ),

    TrackMenuSpec(
        TrackMenuAction.DOWNLOAD_OFFLINE,
        { "Download All for Offline" },
        TrackMenuGroup.DOWNLOAD,
        visible = { !it.isOffline && !it.isLocalFiles },
    ),
    TrackMenuSpec(
        TrackMenuAction.DELETE_DOWNLOAD,
        { "Delete All Downloads (${it.count})" },
        TrackMenuGroup.DOWNLOAD,
        visible = { it.isOffline },
    ),

    TrackMenuSpec(TrackMenuAction.CLEAR_SELECTION, { "Clear Selection" }, TrackMenuGroup.MORE),
)

/**
 * Derives a pure [TrackMenuContext] from the current [MeloState].
 */
fun buildTrackMenuContext(state: MeloState): TrackMenuContext {
    val isBatch = state.trackOptions.isBatch
    val track = state.trackOptions.track
    val screen = state.screen

    val isLocalPlaylist =
        screen is ScreenState.Library &&
                screen.libraryTab == LibraryTab.PLAYLISTS &&
                screen.isInPlaylistDetail &&
                screen.selectedPlaylist != null
    val isFavorites =
        (screen is ScreenState.Library && screen.libraryTab == LibraryTab.FAVORITES) ||
                (screen is ScreenState.Home && screen.homeTab == HomeTab.FAVORITES)
    val isOffline = screen is ScreenState.Offline
    val isLocalFiles = screen is ScreenState.Library && screen.libraryTab == LibraryTab.LOCAL

    val offlineTrack =
        if (isBatch) null
        else track?.let { state.collections.offlineTracks.find { entry -> entry.track.id == it.id } }

    return TrackMenuContext(
        isBatch = isBatch,
        count = if (isBatch) state.trackOptions.batchTracks.size else 0,
        isFav = !isBatch && track != null && state.isFavoriteTrack(track),
        isManualDownloaded = !isBatch &&
                offlineTrack?.downloadStatus == DownloadStatus.COMPLETED &&
                offlineTrack.downloadType == DownloadType.MANUAL,
        isDownloading = !isBatch &&
                (offlineTrack?.downloadStatus == DownloadStatus.DOWNLOADING ||
                        offlineTrack?.downloadStatus == DownloadStatus.PENDING),
        isLocalPlaylist = isLocalPlaylist,
        isFavorites = isFavorites,
        isOffline = isOffline,
        isLocalFiles = isLocalFiles,
    )
}

/**
 * Resolves the menu entries for the given state.
 * Pure filter + map over the declarative specs; order is spec order.
 */
fun resolveTrackMenuItems(state: MeloState): List<TrackMenuItem> {
    if (!state.trackOptions.isBatch && state.trackOptions.track == null) return emptyList()

    val context = buildTrackMenuContext(state)
    val specs = if (context.isBatch) BATCH_TRACK_SPECS else SINGLE_TRACK_SPECS
    return specs
        .filter { it.visible(context) }
        .map { TrackMenuItem(action = it.action, label = it.label(context), group = it.group) }
}

/**
 * Floating context menu for track-specific actions.
 */
class TrackOptionsOverlay(
    private val stateProvider: () -> MeloState,
    private val onKeyEvent: (KeyEvent) -> EventResult
) : Element {

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        val isBatch = state.trackOptions.isBatch
        val track = state.trackOptions.track
        if (!isBatch && track == null) return
        val menuItems = resolveTrackMenuItems(state)
        if (menuItems.isEmpty()) return

        val separatorCount = menuItems.zipWithNext().count { (a, b) -> a.group != b.group }
        val overlayW = (area.width() * 0.45).toInt().coerceAtLeast(46)
        val overlayH = menuItems.size + separatorCount + 6
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.buffer().clear(overlayArea)

        val hint = "[↑↓] navigate   [Enter] select   [Esc] close"
        val subtitle = if (isBatch) {
            "${state.trackOptions.batchTracks.size} tracks selected"
        } else {
            "${track?.title} — ${track?.artist}"
        }
        val panelTitle = if (isBatch) "Batch Options" else "Track Options"

        val safeSelectedIndex =
            state.trackOptions.selectedIndex.coerceIn(0, (menuItems.size - 1).coerceAtLeast(0))

        val separatorLine = "─".repeat((overlayW - 4).coerceAtLeast(1))

        val rows = buildList {
            var lastGroup: TrackMenuGroup? = null
            for ((index, item) in menuItems.withIndex()) {
                if (lastGroup != null && item.group != lastGroup) {
                    add(row(text(separatorLine).fg(TEXT_DIM)))
                }
                lastGroup = item.group

                val isSelected = index == safeSelectedIndex
                add(
                    row(
                        text(if (isSelected) " ${MeloTheme.ICON_ARROW} " else "   ").fg(
                            PRIMARY_COLOR
                        )
                            .length(3),
                        text(item.label).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY).fill(),
                    )
                )
            }
        }

        val content = column(
            text(subtitle).fg(TEXT_DIM).centered(),
            text("").length(1),
            *rows.toTypedArray(),
            spacer(),
            text(hint).fg(TEXT_DIM).centered(),
        )

        panel(content)
            .title(panelTitle)
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("track-options-panel")
            .onKeyEvent(onKeyEvent)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(
        availableWidth: Int,
        availableHeight: Int,
        context: RenderContext
    ): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult {
        if (!focused || !stateProvider().trackOptions.isVisible) return EventResult.UNHANDLED
        return onKeyEvent(event)
    }
}