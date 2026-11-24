@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
