package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors

@Composable
fun PlaylistMosaicCover(
    artworks: List<String>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    placeholderIconSize: Dp = 44.dp,
    fallbackIcon: ImageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
) {
    val validArtworks = artworks.filter { it.isNotBlank() }.distinct()

    Box(
        modifier = modifier
            .clip(shape)
            .background(MeloColors.surface2),
        contentAlignment = Alignment.Center
    ) {
        when {
            validArtworks.size >= 4 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            MeloAsyncImage(
                                url = validArtworks[0],
                                contentDescription = null,
                                shape = RectangleShape,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            MeloAsyncImage(
                                url = validArtworks[1],
                                contentDescription = null,
                                shape = RectangleShape,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            MeloAsyncImage(
                                url = validArtworks[2],
                                contentDescription = null,
                                shape = RectangleShape,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            MeloAsyncImage(
                                url = validArtworks[3],
                                contentDescription = null,
                                shape = RectangleShape,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            validArtworks.isNotEmpty() -> {
                MeloAsyncImage(
                    url = validArtworks.first(),
                    contentDescription = null,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(placeholderIconSize)
                )
            }
        }
    }
}