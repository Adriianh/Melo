package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchUiState

@Composable
fun SongsResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    queueViewModel: QueueViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = searchResultContentPadding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(uiState.songResults) { track ->
            TrackRow(track = track, onClick = { queueViewModel.playTrack(track) })
        }
    }
}