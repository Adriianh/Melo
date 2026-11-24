@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.resources.a11y_play_all
import com.github.adriianh.melo.resources.a11y_radio
import com.github.adriianh.melo.resources.a11y_save_to_library
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.songMenuRightClick
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CuratedHighlightCard(
    modifier: Modifier = Modifier,
    title: String,
    author: String,
    metadata: String,
    artworkUrl: String?,
    tracks: List<Track>,
    onPlayAll: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackMoreClick: ((Track, SongMenuAnchor?) -> Unit)? = null,
    onRadioClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
) {
    Card(
        modifier =
            modifier
                .width(330.dp)
                .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MeloColors.surface1),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MeloAsyncImage(
                    url = artworkUrl,
                    contentDescription = title,
                    size = 64.dp,
                    shape = RoundedCornerShape(8.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = author,
                        style = MeloType.labelMedium,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (metadata.isNotBlank()) {
                        Text(
                            text = metadata,
                            style = MeloType.labelSmall,
                            color = MeloColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tracks.take(3).forEach { track ->
                    var rowBounds by remember { mutableStateOf<Rect?>(null) }
                    var moreBounds by remember { mutableStateOf<Rect?>(null) }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                                .songMenuRightClick(
                                    enabled = onTrackMoreClick != null,
                                    onOpenMenu = { anchor -> onTrackMoreClick?.invoke(track, anchor) },
                                ).combinedClickable(
                                    onClick = { onTrackClick(track) },
                                    onLongClick =
                                        onTrackMoreClick?.let { open ->
                                            { open(track, rowBounds?.let { SongMenuAnchor.Bounds(it) }) }
                                        },
                                ).padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MeloAsyncImage(
                            url = track.artworkUrl,
                            contentDescription = track.title,
                            size = 40.dp,
                            shape = RoundedCornerShape(6.dp),
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MeloType.body,
                                color = MeloColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MeloType.labelSmall,
                                color = MeloColors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (onTrackMoreClick != null) {
                            IconButton(
                                onClick = {
                                    onTrackMoreClick(track, moreBounds?.let { SongMenuAnchor.Bounds(it) })
                                },
                                modifier =
                                    Modifier
                                        .size(28.dp)
                                        .onGloballyPositioned { moreBounds = it.boundsInWindow() },
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(Res.string.a11y_more_options),
                                    tint = MeloColors.textMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.a11y_more_options),
                                tint = MeloColors.textMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MeloColors.surface2,
                    modifier = Modifier.size(44.dp).clickable(onClick = onPlayAll),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(Res.string.a11y_play_all),
                            tint = MeloColors.textPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MeloColors.surface2,
                    modifier = Modifier.size(44.dp).clickable(onClick = onRadioClick),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Radio,
                            contentDescription = stringResource(Res.string.a11y_radio),
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MeloColors.surface2,
                    modifier = Modifier.size(44.dp).clickable(onClick = onBookmarkClick),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(Res.string.a11y_save_to_library),
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
