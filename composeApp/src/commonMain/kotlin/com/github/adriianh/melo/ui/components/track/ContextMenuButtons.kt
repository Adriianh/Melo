@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.util.MeloColors
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CardContextMenuButton(
    isVisible: Boolean,
    onOpenMenu: (SongMenuAnchor?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return
    var buttonBounds by remember { mutableStateOf<Rect?>(null) }
    IconButton(
        onClick = { onOpenMenu(buttonBounds?.let { SongMenuAnchor.Bounds(it) }) },
        modifier =
            modifier
                .padding(4.dp)
                .size(28.dp)
                .onGloballyPositioned { buttonBounds = it.boundsInWindow() },
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(Res.string.a11y_more_options),
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun RowContextMenuButton(
    isVisible: Boolean,
    onOpenMenu: (SongMenuAnchor?) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    iconSize: Dp = 14.dp,
) {
    var buttonBounds by remember { mutableStateOf<Rect?>(null) }
    IconButton(
        onClick = { onOpenMenu(buttonBounds?.let { SongMenuAnchor.Bounds(it) }) },
        enabled = isVisible,
        modifier =
            modifier
                .size(size)
                .alpha(if (isVisible) 1f else 0f)
                .onGloballyPositioned { buttonBounds = it.boundsInWindow() },
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(Res.string.a11y_more_options),
            tint = MeloColors.textMuted,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
internal fun SongMenuFocusOnOpenEffect(
    isExpandedDesktop: Boolean,
    interactiveEntries: List<SongMenuEntry>,
    inputModeManager: InputModeManager,
    containerFocusRequester: FocusRequester,
    focusRequesters: List<FocusRequester>,
    onFocusedIndexResolved: (Int) -> Unit,
) {
    LaunchedEffect(interactiveEntries.size) {
        if (!isExpandedDesktop || interactiveEntries.isEmpty()) return@LaunchedEffect
        if (inputModeManager.inputMode != InputMode.Keyboard) {
            runCatching { containerFocusRequester.requestFocus() }
            return@LaunchedEffect
        }
        val firstEnabled =
            interactiveEntries
                .indexOfFirst { it !is SongMenuEntry.Action || it.enabled }
                .coerceAtLeast(0)
        onFocusedIndexResolved(firstEnabled)
        runCatching { focusRequesters[firstEnabled].requestFocus() }
    }
}
