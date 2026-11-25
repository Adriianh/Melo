@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_collapse
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NowPlayingTopBar(
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit,
    onMoreClick: ((SongMenuAnchor?) -> Unit)? = null,
    headerSubtitle: String? = null,
    headerTitle: String? = null,
    onHeaderClick: (() -> Unit)? = null,
    iconTint: Color = Color.White,
) {
    var moreButtonBounds by remember { mutableStateOf<Rect?>(null) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.a11y_collapse),
                tint = iconTint,
                modifier = Modifier.size(30.dp),
            )
        }

        if (!headerTitle.isNullOrBlank()) {
            NowPlayingTopBarHeader(
                subtitle = headerSubtitle,
                title = headerTitle,
                iconTint = iconTint,
                onClick = onHeaderClick,
                modifier = Modifier.weight(1f),
            )
        }

        IconButton(
            onClick = {
                onMoreClick?.invoke(moreButtonBounds?.let { SongMenuAnchor.Bounds(it) })
            },
            enabled = onMoreClick != null,
            modifier =
                Modifier
                    .size(48.dp)
                    .onGloballyPositioned { moreButtonBounds = it.boundsInWindow() },
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.a11y_more_options),
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingTopBarHeader(
    subtitle: String?,
    title: String,
    iconTint: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val clickableModifier =
        modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    Column(
        modifier = clickableModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle.uppercase(),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.08.em,
                fontWeight = FontWeight.SemiBold,
                color = iconTint.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = iconTint.copy(alpha = 0.90f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
