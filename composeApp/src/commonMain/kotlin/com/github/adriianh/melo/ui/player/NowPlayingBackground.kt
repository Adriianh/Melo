package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors

@Composable
fun NowPlayingBackground(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MeloColors.surface0)) {
        if (artworkUrl != null) {
            MeloAsyncImage(
                url = artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.4f)
                    .blur(24.dp),
                shape = RoundedCornerShape(0.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.95f),
                            )
                        )
                    )
            )
        }
    }
}