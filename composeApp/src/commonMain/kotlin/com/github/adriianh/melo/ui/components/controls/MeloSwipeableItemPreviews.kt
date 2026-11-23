@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun MockTrackItem(
    title: String,
    artist: String,
    duration: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color(0xFF242424))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.LightGray,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = duration,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color.Gray,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewMeloSwipeableItemIdle() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(16.dp),
        ) {
            MeloSwipeableItem(
                onSwipeRight = {},
                trailingActions = listOf(MeloSwipeActions.queue() to {}, MeloSwipeActions.remove() to {}),
            ) {
                MockTrackItem(
                    title = "Starboy",
                    artist = "The Weeknd ft. Daft Punk",
                    duration = "3:50",
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMeloSwipeableItemLeadingPartial() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(16.dp),
        ) {
            MeloSwipeableItem(
                onSwipeRight = {},
                initialOffsetDp = 44f,
            ) {
                MockTrackItem(
                    title = "Starboy",
                    artist = "The Weeknd ft. Daft Punk",
                    duration = "3:50",
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMeloSwipeableItemLeadingCommitted() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(16.dp),
        ) {
            MeloSwipeableItem(
                onSwipeRight = {},
                initialOffsetDp = 68f,
            ) {
                MockTrackItem(
                    title = "Starboy",
                    artist = "The Weeknd ft. Daft Punk",
                    duration = "3:50",
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMeloSwipeableItemTrailingRevealed() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(16.dp),
        ) {
            MeloSwipeableItem(
                onSwipeRight = {},
                trailingActions = listOf(MeloSwipeActions.queue() to {}, MeloSwipeActions.remove() to {}),
                initialOffsetDp = -152f,
            ) {
                MockTrackItem(
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    duration = "3:20",
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMeloSwipeableItemListOneOpen() {
    MaterialTheme {
        val group = rememberSwipeRevealGroup()
        CompositionLocalProvider(LocalSwipeRevealGroup provides group) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121212))
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tracks =
                    listOf(
                        Triple("Get Lucky", "Daft Punk ft. Pharrell Williams", "4:08"),
                        Triple("As It Was", "Harry Styles", "2:47"),
                        Triple("Save Your Tears", "The Weeknd", "3:35"),
                    )

                tracks.forEachIndexed { index, (title, artist, duration) ->
                    val offset = if (index == 1) -152f else 0f
                    MeloSwipeableItem(
                        onSwipeRight = {},
                        trailingActions = listOf(MeloSwipeActions.queue() to {}, MeloSwipeActions.remove() to {}),
                        initialOffsetDp = offset,
                    ) {
                        MockTrackItem(
                            title = title,
                            artist = artist,
                            duration = duration,
                        )
                    }
                }
            }
        }
    }
}
