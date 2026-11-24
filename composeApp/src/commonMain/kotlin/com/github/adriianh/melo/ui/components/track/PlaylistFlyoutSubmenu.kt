@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_cancel
import com.github.adriianh.melo.resources.common_action_create
import com.github.adriianh.melo.resources.playlist_sheet_empty
import com.github.adriianh.melo.resources.playlist_sheet_new_name_placeholder
import com.github.adriianh.melo.resources.track_menu_new_playlist
import com.github.adriianh.melo.resources.track_menu_playlist_search_placeholder
import com.github.adriianh.melo.ui.components.cards.PlaylistMosaicCover
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource

private val FLYOUT_WIDTH = 248.dp
private val FLYOUT_MAX_HEIGHT = 320.dp
private val FLYOUT_MARGIN = 8.dp
private val FLYOUT_GAP = 4.dp

@Composable
internal fun PlaylistFlyoutPopup(
    parentMenuBounds: IntRect,
    submenuRowBounds: IntRect?,
    content: PlaylistSubmenuContent,
    onDismissSubmenu: () -> Unit,
    onDismissAll: () -> Unit,
    focusable: Boolean = true,
    onHoverChanged: (Boolean) -> Unit = {},
) {
    val positionProvider =
        remember(parentMenuBounds, submenuRowBounds) {
            FlyoutPopupPositionProvider(
                parentBounds = parentMenuBounds,
                rowBounds = submenuRowBounds ?: parentMenuBounds,
            )
        }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissAll,
        properties = PopupProperties(focusable = focusable, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        PlaylistFlyoutContent(
            content = content,
            onDismissSubmenu = onDismissSubmenu,
            onDismissAll = onDismissAll,
            autoFocusSearch = focusable,
            onHoverChanged = onHoverChanged,
        )
    }
}

@Composable
internal fun PlaylistFlyoutContent(
    content: PlaylistSubmenuContent,
    onDismissSubmenu: () -> Unit,
    onDismissAll: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true,
    onHoverChanged: (Boolean) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val newNameFocusRequester = remember { FocusRequester() }
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()

    LaunchedEffect(isHovered) { onHoverChanged(isHovered) }
    LaunchedEffect(autoFocusSearch) { if (autoFocusSearch) runCatching { searchFocusRequester.requestFocus() } }
    LaunchedEffect(isCreatingNew) { if (isCreatingNew) runCatching { newNameFocusRequester.requestFocus() } }

    val filteredPlaylists =
        remember(content.playlists, searchQuery) {
            val q = searchQuery.trim()
            if (q.isEmpty()) content.playlists else content.playlists.filter { it.name.contains(q, ignoreCase = true) }
        }

    Surface(
        modifier =
            modifier
                .width(FLYOUT_WIDTH)
                .heightIn(max = FLYOUT_MAX_HEIGHT)
                .hoverable(hoverSource)
                .onPreviewKeyEvent { event ->
                    handleFlyoutKeyEvent(event, !isCreatingNew && searchQuery.isEmpty(), onDismissSubmenu)
                },
        shape = RoundedCornerShape(12.dp),
        color = MeloColors.menuPopupSurface,
        border = BorderStroke(1.dp, MeloColors.menuPopupBorder),
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            PlaylistFlyoutHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchFocusRequester = searchFocusRequester,
                isCreatingNew = isCreatingNew,
                onStartCreate = { isCreatingNew = true },
                newPlaylistName = newPlaylistName,
                onNewPlaylistNameChange = { newPlaylistName = it },
                newNameFocusRequester = newNameFocusRequester,
                onConfirmCreate = {
                    newPlaylistName.trim().takeIf { it.isNotEmpty() }?.let {
                        content.onCreatePlaylistAndAdd(it)
                        onDismissAll()
                    }
                },
                onCancelCreate = {
                    isCreatingNew = false
                    newPlaylistName = ""
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MeloColors.menuDivider)
            PlaylistFlyoutList(
                playlists = filteredPlaylists,
                containingPlaylistIds = content.containingPlaylistIds,
                onSelectPlaylist = { content.onSelectPlaylist(it); onDismissAll() },
            )
        }
    }
}

@Composable
private fun PlaylistFlyoutHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    isCreatingNew: Boolean,
    onStartCreate: () -> Unit,
    newPlaylistName: String,
    onNewPlaylistNameChange: (String) -> Unit,
    newNameFocusRequester: FocusRequester,
    onConfirmCreate: () -> Unit,
    onCancelCreate: () -> Unit,
) {
    PlaylistFlyoutSearchBar(
        query = searchQuery,
        onQueryChange = onSearchQueryChange,
        focusRequester = searchFocusRequester,
    )
    if (isCreatingNew) {
        PlaylistFlyoutCreateRow(
            name = newPlaylistName,
            onNameChange = onNewPlaylistNameChange,
            focusRequester = newNameFocusRequester,
            onConfirm = onConfirmCreate,
            onCancel = onCancelCreate,
        )
    } else {
        FlyoutActionRow(
            onClick = onStartCreate,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MeloColors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = stringResource(Res.string.track_menu_new_playlist),
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun handleFlyoutKeyEvent(
    event: KeyEvent,
    canCloseWithLeft: Boolean,
    onDismissSubmenu: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val shouldClose = event.key == Key.Escape || (event.key == Key.DirectionLeft && canCloseWithLeft)
    if (shouldClose) {
        onDismissSubmenu()
    }
    return shouldClose
}

@Composable
private fun PlaylistFlyoutSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MeloColors.surface0.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MeloColors.textMuted,
            modifier = Modifier.size(16.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(Res.string.track_menu_playlist_search_placeholder),
                    style = MeloType.labelSmall,
                    color = MeloColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MeloType.labelSmall.copy(color = MeloColors.textPrimary),
                cursorBrush = SolidColor(MeloColors.textPrimary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
    }
}

@Composable
private fun PlaylistFlyoutCreateRow(
    name: String,
    onNameChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PlaylistFlyoutNameInput(
            name = name,
            onNameChange = onNameChange,
            focusRequester = focusRequester,
            onConfirm = onConfirm,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onConfirm,
            enabled = name.isNotBlank(),
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(Res.string.common_action_create),
                tint = if (name.isNotBlank()) MeloColors.textPrimary else MeloColors.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.common_action_cancel),
                tint = MeloColors.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PlaylistFlyoutNameInput(
    name: String,
    onNameChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MeloColors.surface0.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        if (name.isEmpty()) {
            Text(
                text = stringResource(Res.string.playlist_sheet_new_name_placeholder),
                style = MeloType.labelSmall,
                color = MeloColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = MeloType.labelSmall.copy(color = MeloColors.textPrimary),
            cursorBrush = SolidColor(MeloColors.textPrimary),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        val isSubmit = event.type == KeyEventType.KeyDown && event.key == Key.Enter
                        if (isSubmit && name.isNotBlank()) {
                            onConfirm()
                            true
                        } else {
                            false
                        }
                    },
        )
    }
}

@Composable
private fun PlaylistFlyoutList(
    playlists: List<Playlist>,
    containingPlaylistIds: Set<Long>,
    onSelectPlaylist: (Playlist) -> Unit,
) {
    if (playlists.isEmpty()) {
        Text(
            text = stringResource(Res.string.playlist_sheet_empty),
            style = MeloType.labelSmall,
            color = MeloColors.textMuted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                val alreadyInPlaylist = playlist.id in containingPlaylistIds
                PlaylistFlyoutItem(
                    playlist = playlist,
                    alreadyInPlaylist = alreadyInPlaylist,
                    onClick = {
                        if (!alreadyInPlaylist) onSelectPlaylist(playlist)
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaylistFlyoutItem(
    playlist: Playlist,
    alreadyInPlaylist: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val highlight = if (isHovered || isFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(highlight)
                .hoverable(interactionSource)
                .clickable(
                    enabled = !alreadyInPlaylist,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaylistMosaicCover(
            artworks = playlist.artworks,
            modifier = Modifier.size(22.dp),
            placeholderIconSize = 14.dp,
            shape = RoundedCornerShape(4.dp),
        )
        Text(
            text = playlist.name,
            style = MeloType.labelMedium,
            color = if (alreadyInPlaylist) MeloColors.textMuted else MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (alreadyInPlaylist) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MeloColors.textMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FlyoutActionRow(
    onClick: () -> Unit,
    leadingIcon: @Composable () -> Unit,
    label: String,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val highlight = if (isHovered || isFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(highlight)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leadingIcon()
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = label,
            style = MeloType.labelMedium,
            fontWeight = fontWeight,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private class FlyoutPopupPositionProvider(
    private val parentBounds: IntRect,
    private val rowBounds: IntRect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val marginPx = FLYOUT_MARGIN.value.toInt().coerceAtLeast(8)
        val gapPx = FLYOUT_GAP.value.toInt().coerceAtLeast(4)

        val rightX = parentBounds.right + gapPx
        val leftX = parentBounds.left - popupContentSize.width - gapPx
        val x =
            when {
                rightX + popupContentSize.width <= windowSize.width - marginPx -> rightX
                leftX >= marginPx -> leftX
                else -> (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
            }

        val preferredY = rowBounds.top
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
        val y = preferredY.coerceIn(marginPx, maxY)

        return IntOffset(x, y)
    }
}
