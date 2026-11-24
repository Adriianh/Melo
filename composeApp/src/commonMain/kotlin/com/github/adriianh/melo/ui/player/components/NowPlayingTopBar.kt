@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_collapse
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.resources.player_header_now_playing
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NowPlayingTopBar(
    title: String,
    onCollapse: () -> Unit,
    onMoreClick: ((SongMenuAnchor?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var moreButtonBounds by remember { mutableStateOf<Rect?>(null) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.a11y_collapse),
                tint = MeloColors.textPrimary,
                modifier = Modifier.size(32.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.player_header_now_playing),
                style = MeloType.labelSmall,
                letterSpacing = 1.5.sp,
                color = MeloColors.textMuted,
            )
            Text(
                text = title.ifEmpty { "Melo" },
                style = MeloType.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = {
                onMoreClick?.invoke(moreButtonBounds?.let { SongMenuAnchor.Bounds(it) })
            },
            enabled = onMoreClick != null,
            modifier = Modifier.onGloballyPositioned { moreButtonBounds = it.boundsInWindow() },
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.a11y_more_options),
                tint = MeloColors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
