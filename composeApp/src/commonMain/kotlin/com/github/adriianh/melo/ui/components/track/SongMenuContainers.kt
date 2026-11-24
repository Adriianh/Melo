@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.github.adriianh.melo.ui.i18n.resolve
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private val MEDIUM_POPUP_WIDTH = 300.dp
private val MEDIUM_ROW_HEIGHT = 48.dp
private val EXPANDED_POPUP_WIDTH = 256.dp
private val EXPANDED_ROW_HEIGHT = 36.dp
private val POPUP_WINDOW_MARGIN = 12.dp
private const val SUBMENU_OPEN_DELAY_MS = 220L
private const val SUBMENU_CLOSE_DELAY_MS = 180L

@Composable
internal fun SongMenuAnchoredPopup(
    entries: List<SongMenuEntry>,
    anchor: SongMenuAnchor?,
    windowSize: SongMenuWindowSize,
    onDismissRequest: () -> Unit,
    initialSubmenuOpen: Boolean = false,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val marginPx = with(density) { POPUP_WINDOW_MARGIN.roundToPx() }
    val maxPopupHeight =
        with(density) {
            (windowInfo.containerSize.height.toDp() - POPUP_WINDOW_MARGIN * 2).coerceAtLeast(240.dp)
        }
    val positionProvider = remember(anchor, marginPx) { SongMenuPopupPositionProvider(anchor, marginPx) }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        SongMenuPopupSurface(
            entries = entries,
            isExpandedDesktop = windowSize == SongMenuWindowSize.Expanded,
            maxHeight = maxPopupHeight,
            onDismissRequest = onDismissRequest,
            initialSubmenuOpen = initialSubmenuOpen,
        )
    }
}

@Composable
internal fun SongMenuPopupSurface(
    entries: List<SongMenuEntry>,
    isExpandedDesktop: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 480.dp,
    initialSubmenuOpen: Boolean = false,
) {
    val width = if (isExpandedDesktop) EXPANDED_POPUP_WIDTH else MEDIUM_POPUP_WIDTH
    val inputModeManager = LocalInputModeManager.current
    val interactiveEntries = remember(entries) { entries.filter { it !is SongMenuEntry.Divider } }
    val containerFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember(interactiveEntries.size) { List(interactiveEntries.size) { FocusRequester() } }
    var focusedIndex by remember(interactiveEntries.size) { mutableStateOf(-1) }
    val initialSubmenu = entries.filterIsInstance<SongMenuEntry.Submenu>().firstOrNull()?.id
    var openSubmenuId by remember {
        mutableStateOf(if (initialSubmenuOpen && isExpandedDesktop) initialSubmenu else null)
    }
    var submenuOpenedByHover by remember { mutableStateOf(false) }
    var menuBoundsInWindow by remember { mutableStateOf<IntRect?>(null) }
    val rowBoundsMap = remember { mutableStateMapOf<String, IntRect>() }

    SongMenuFocusOnOpenEffect(
        isExpandedDesktop = isExpandedDesktop,
        interactiveEntries = interactiveEntries,
        inputModeManager = inputModeManager,
        containerFocusRequester = containerFocusRequester,
        focusRequesters = focusRequesters,
        onFocusedIndexResolved = { focusedIndex = it },
    )

    val popupModifier =
        modifier
            .width(width)
            .heightIn(max = maxHeight)
            .focusRequester(containerFocusRequester)
            .focusable()
            .onGloballyPositioned { menuBoundsInWindow = it.boundsInWindow().toIntRect() }
            .onPreviewKeyEvent { ev ->
                handlePopupKeyEvent(ev, openSubmenuId != null, { openSubmenuId = null }, onDismissRequest) { delta ->
                    moveFocusToNextEnabled(interactiveEntries, focusRequesters, focusedIndex, delta) {
                        focusedIndex = it
                    }
                }
            }.semantics { collectionInfo = CollectionInfo(rowCount = interactiveEntries.size, columnCount = 1) }

    Surface(
        modifier = popupModifier,
        shape = RoundedCornerShape(12.dp),
        color = MeloColors.menuPopupSurface,
        border = BorderStroke(1.dp, MeloColors.menuPopupBorder),
        shadowElevation = 12.dp,
    ) {
        SongMenuPopupEntriesColumn(
            entries = entries,
            isExpandedDesktop = isExpandedDesktop,
            focusRequesters = focusRequesters,
            openSubmenuId = openSubmenuId,
            submenuOpenedByHover = submenuOpenedByHover,
            menuBoundsInWindow = menuBoundsInWindow,
            rowBoundsMap = rowBoundsMap,
            onFocusedIndexChange = { focusedIndex = it },
            onOpenSubmenuChange = { id, byHover ->
                openSubmenuId = id
                submenuOpenedByHover = byHover
            },
            onDismissRequest = onDismissRequest,
        )
    }
}

private fun Rect.toIntRect(): IntRect =
    IntRect(
        left = left.roundToInt(),
        top = top.roundToInt(),
        right = right.roundToInt(),
        bottom = bottom.roundToInt(),
    )

@Composable
private fun SongMenuPopupEntriesColumn(
    entries: List<SongMenuEntry>,
    isExpandedDesktop: Boolean,
    focusRequesters: List<FocusRequester>,
    openSubmenuId: String?,
    submenuOpenedByHover: Boolean,
    menuBoundsInWindow: IntRect?,
    rowBoundsMap: MutableMap<String, IntRect>,
    onFocusedIndexChange: (Int) -> Unit,
    onOpenSubmenuChange: (String?, Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val rowStyle =
        if (isExpandedDesktop) {
            PopupRowStyle(EXPANDED_ROW_HEIGHT, 10.dp, 18.dp, isExpandedDesktop = true)
        } else {
            PopupRowStyle(MEDIUM_ROW_HEIGHT, 14.dp, 20.dp, isExpandedDesktop = false)
        }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        var interactiveIdx = 0
        entries.forEach { entry ->
            when (entry) {
                SongMenuEntry.Divider ->
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MeloColors.menuDivider,
                    )

                is SongMenuEntry.Action -> {
                    val idx = interactiveIdx++
                    PopupActionEntryRow(
                        entry = entry,
                        rowStyle = rowStyle,
                        focusRequester = focusRequesters[idx],
                        onFocused = { onFocusedIndexChange(idx) },
                        onHover = {
                            if (isExpandedDesktop && openSubmenuId != null && submenuOpenedByHover) {
                                onOpenSubmenuChange(null, false)
                            }
                        },
                        onDismissRequest = onDismissRequest,
                    )
                }

                is SongMenuEntry.Submenu -> {
                    val idx = interactiveIdx++
                    PopupSubmenuEntryRow(
                        entry = entry,
                        rowStyle = rowStyle,
                        focusRequester = focusRequesters[idx],
                        isSubmenuOpen = openSubmenuId == entry.id,
                        openedByHover = submenuOpenedByHover,
                        menuBoundsInWindow = menuBoundsInWindow,
                        rowBoundsMap = rowBoundsMap,
                        onFocused = { onFocusedIndexChange(idx) },
                        onOpenSubmenuChange = onOpenSubmenuChange,
                        onDismissRequest = onDismissRequest,
                    )
                }
            }
        }
    }
}

private data class PopupRowStyle(
    val rowHeight: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val isExpandedDesktop: Boolean,
)

@Composable
private fun PopupActionEntryRow(
    entry: SongMenuEntry.Action,
    rowStyle: PopupRowStyle,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onHover: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val textColor =
        if (entry.style == SongMenuEntryStyle.Destructive) MeloColors.menuDestructive else MeloColors.textPrimary
    PopupMenuRow(
        label = entry.label.resolve(),
        icon = entry.icon,
        iconTint = entry.tint ?: textColor,
        textColor = textColor,
        rowStyle = rowStyle,
        enabled = entry.enabled,
        focusRequester = focusRequester,
        onFocused = onFocused,
        onHoverChanged = { hovered -> if (hovered) onHover() },
        onClick = {
            entry.onClick()
            onDismissRequest()
        },
    )
}

@Composable
private fun PopupSubmenuEntryRow(
    entry: SongMenuEntry.Submenu,
    rowStyle: PopupRowStyle,
    focusRequester: FocusRequester,
    isSubmenuOpen: Boolean,
    openedByHover: Boolean,
    menuBoundsInWindow: IntRect?,
    rowBoundsMap: MutableMap<String, IntRect>,
    onFocused: () -> Unit,
    onOpenSubmenuChange: (String?, Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val isExpanded = rowStyle.isExpandedDesktop
    var isRowHovered by remember { mutableStateOf(false) }
    var isFlyoutHovered by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded, isSubmenuOpen, openedByHover, isRowHovered, isFlyoutHovered) {
        if (!isExpanded) return@LaunchedEffect
        val hoverExited = !isRowHovered && !isFlyoutHovered
        if (isRowHovered && !isSubmenuOpen) {
            delay(SUBMENU_OPEN_DELAY_MS.milliseconds)
            onOpenSubmenuChange(entry.id, true)
        } else if (isSubmenuOpen && openedByHover && hoverExited) {
            delay(SUBMENU_CLOSE_DELAY_MS.milliseconds)
            onOpenSubmenuChange(null, false)
        }
    }

    PopupMenuRow(
        label = entry.label.resolve(),
        icon = entry.icon,
        iconTint = MeloColors.textPrimary,
        textColor = MeloColors.textPrimary,
        rowStyle = rowStyle,
        enabled = true,
        focusRequester = focusRequester,
        hasSubmenu = true,
        isSubmenuExpanded = isSubmenuOpen,
        onFocused = onFocused,
        onHoverChanged = { isRowHovered = it },
        onOpenSubmenuKey = {
            if (isExpanded) onOpenSubmenuChange(entry.id, false)
            isExpanded
        },
        onRowPositioned = { rect -> rowBoundsMap[entry.id] = rect },
        onClick = {
            if (isExpanded) {
                val keepOpen = !isSubmenuOpen || openedByHover
                onOpenSubmenuChange(if (keepOpen) entry.id else null, false)
            } else {
                entry.content.onOpenSheetFallback()
                onDismissRequest()
            }
        },
    )
    menuBoundsInWindow?.takeIf { isExpanded && isSubmenuOpen }?.let { bounds ->
        PlaylistFlyoutPopup(
            parentMenuBounds = bounds,
            submenuRowBounds = rowBoundsMap[entry.id],
            content = entry.content,
            onDismissSubmenu = { onOpenSubmenuChange(null, false) },
            onDismissAll = onDismissRequest,
            onHoverChanged = { isFlyoutHovered = it },
        )
    }
}

private fun moveFocusToNextEnabled(
    interactiveEntries: List<SongMenuEntry>,
    focusRequesters: List<FocusRequester>,
    currentIndex: Int,
    delta: Int,
    onFocusMoved: (Int) -> Unit,
) {
    if (interactiveEntries.isEmpty()) return
    var candidate =
        if (currentIndex < 0) {
            if (delta > 0) {
                -1
            } else {
                0
            }
        } else {
            currentIndex
        }
    repeat(interactiveEntries.size) {
        candidate = (candidate + delta + interactiveEntries.size) % interactiveEntries.size
        val target = interactiveEntries[candidate]
        if (target !is SongMenuEntry.Action || target.enabled) {
            onFocusMoved(candidate)
            runCatching { focusRequesters[candidate].requestFocus() }
            return
        }
    }
}

private fun handlePopupKeyEvent(
    event: KeyEvent,
    hasOpenSubmenu: Boolean,
    onCloseSubmenu: () -> Unit,
    onDismiss: () -> Unit,
    onMoveFocus: (Int) -> Unit,
): Boolean =
    event.type == KeyEventType.KeyDown &&
        when (event.key) {
            Key.Escape -> {
                if (hasOpenSubmenu) onCloseSubmenu() else onDismiss()
                true
            }

            Key.DirectionDown -> {
                onMoveFocus(1)
                true
            }

            Key.DirectionUp -> {
                onMoveFocus(-1)
                true
            }

            Key.DirectionLeft -> {
                if (hasOpenSubmenu) {
                    onCloseSubmenu()
                    true
                } else {
                    false
                }
            }

            else -> false
        }

@Composable
private fun PopupMenuRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    textColor: Color,
    rowStyle: PopupRowStyle,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onHoverChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    hasSubmenu: Boolean = false,
    isSubmenuExpanded: Boolean = false,
    onOpenSubmenuKey: () -> Boolean = { false },
    onRowPositioned: ((IntRect) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isKeyboardMode = LocalInputModeManager.current.inputMode == InputMode.Keyboard

    LaunchedEffect(isHovered) { onHoverChanged(isHovered) }
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }

    val highlightColor =
        when {
            isFocused && isKeyboardMode -> Color.White.copy(alpha = 0.12f)
            isHovered || isSubmenuExpanded -> Color.White.copy(alpha = 0.08f)
            else -> Color.Transparent
        }
    val positionedModifier =
        onRowPositioned?.let { cb -> Modifier.onGloballyPositioned { cb(it.boundsInWindow().toIntRect()) } } ?: Modifier

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = rowStyle.rowHeight)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(highlightColor)
                .alpha(if (enabled) 1f else 0.5f)
                .then(positionedModifier)
                .hoverable(interactionSource)
                .focusRequester(focusRequester)
                .focusable(enabled = enabled, interactionSource = interactionSource)
                .onPreviewKeyEvent { handleRowKeyEvent(it, enabled, onClick, onOpenSubmenuKey) }
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
                    contentDescription = label
                    if (hasSubmenu) stateDescription = if (isSubmenuExpanded) "expanded" else "collapsed"
                }.padding(horizontal = rowStyle.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PopupMenuRowContent(
            label = label,
            icon = icon,
            iconTint = iconTint,
            textColor = textColor,
            rowStyle = rowStyle,
            hasSubmenu = hasSubmenu,
        )
    }
}

@Composable
private fun RowScope.PopupMenuRowContent(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    textColor: Color,
    rowStyle: PopupRowStyle,
    hasSubmenu: Boolean,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(rowStyle.iconSize),
    )
    Text(
        text = label,
        style = if (rowStyle.isExpandedDesktop) MeloType.labelMedium else MeloType.body,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
    if (hasSubmenu && rowStyle.isExpandedDesktop) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MeloColors.textMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun handleRowKeyEvent(
    event: KeyEvent,
    enabled: Boolean,
    onClick: () -> Unit,
    onOpenSubmenuKey: () -> Boolean,
): Boolean =
    !(event.type != KeyEventType.KeyDown || !enabled) &&
        when (event.key) {
            Key.Enter, Key.Spacebar -> {
                onClick()
                true
            }

            Key.DirectionRight -> onOpenSubmenuKey()
            else -> false
        }

private class SongMenuPopupPositionProvider(
    private val anchor: SongMenuAnchor?,
    private val marginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val targetRect = anchorToIntRect(anchor, windowSize)
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val preferredX =
            when (anchor) {
                is SongMenuAnchor.Bounds -> targetRect.right - popupContentSize.width
                is SongMenuAnchor.Point, null -> targetRect.left
            }
        val x = preferredX.coerceIn(marginPx, maxX)

        val spaceBelow = windowSize.height - targetRect.bottom - marginPx
        val spaceAbove = targetRect.top - marginPx
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)

        val y =
            if (spaceBelow >= popupContentSize.height || spaceBelow >= spaceAbove) {
                targetRect.bottom.coerceIn(marginPx, maxY)
            } else {
                (targetRect.top - popupContentSize.height).coerceIn(marginPx, maxY)
            }

        return IntOffset(x, y)
    }

    private fun anchorToIntRect(
        anchor: SongMenuAnchor?,
        windowSize: IntSize,
    ): IntRect =
        when (anchor) {
            is SongMenuAnchor.Bounds -> anchor.rect.toIntRect()
            is SongMenuAnchor.Point -> {
                val px = anchor.offset.x.roundToInt()
                val py = anchor.offset.y.roundToInt()
                IntRect(left = px, top = py, right = px, bottom = py)
            }

            null -> {
                val cx = windowSize.width / 2
                val cy = windowSize.height / 2
                IntRect(left = cx, top = cy, right = cx, bottom = cy)
            }
        }
}
