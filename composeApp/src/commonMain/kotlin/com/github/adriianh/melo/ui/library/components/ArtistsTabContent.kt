package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.MeloEmptyState
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformAsyncImage

@Composable
fun ArtistsTabContent(
    artists: List<SearchResult.Artist>,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        MeloEmptyState(message = "No sigues a ningún artista actualmente.", modifier = modifier)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(artists) { artist ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onArtistClick(artist.id) }
                    .padding(4.dp)
            ) {
                PlatformAsyncImage(
                    url = artist.artworkUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    size = 100.dp,
                    shape = CircleShape
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = artist.name,
                    style = MeloType.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
