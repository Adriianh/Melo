@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.ui.components.track.CardContextMenuButton
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.songMenuRightClick
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 140.dp,
    onOpenContextMenu: ((SongMenuAnchor?) -> Unit)? = null,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier =
            modifier
                .width(cardWidth)
                .clip(RoundedCornerShape(8.dp))
                .hoverable(hoverSource)
                .onGloballyPositioned { cardBounds = it.boundsInWindow() }
                .songMenuRightClick(
                    enabled = onOpenContextMenu != null,
                    onOpenMenu = { anchor -> onOpenContextMenu?.invoke(anchor) },
                ).combinedClickable(
                    onClick = onClick,
                    onLongClick =
                        onOpenContextMenu?.let { open ->
                            { open(cardBounds?.let { SongMenuAnchor.Bounds(it) }) }
                        },
                ).padding(4.dp),
    ) {
        Box(modifier = Modifier.size(cardWidth)) {
            MeloAsyncImage(
                url = artworkUrl,
                contentDescription = title,
                size = cardWidth,
                shape = RoundedCornerShape(8.dp),
            )
            if (onOpenContextMenu != null) {
                CardContextMenuButton(
                    isVisible = isHovered,
                    onOpenMenu = onOpenContextMenu,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MeloType.labelMedium,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MeloType.labelSmall,
            color = MeloColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
