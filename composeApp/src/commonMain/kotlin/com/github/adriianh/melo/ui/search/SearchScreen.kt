package com.github.adriianh.melo.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.CategoryCard
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.SongFourRowCarousel
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: SearchViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box {
            SearchTopBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onClear = viewModel::clearQuery,
                onOpenSettings = onOpenSettings,
                onFocusChanged = { isFocused = it },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            if (isFocused && uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty()) {
                SuggestionsDropdown(
                    suggestions = uiState.suggestions,
                    onSelect = { suggestion ->
                        viewModel.onQueryChange(suggestion)
                        isFocused = false
                    },
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 56.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isBrowsingCategory || uiState.browseCategoryResult != null -> {
                    if (uiState.isBrowsingCategory) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val browseResult = uiState.browseCategoryResult
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 24.dp,
                                end = 24.dp,
                                top = 8.dp,
                                bottom = paddingValues.calculateBottomPadding() + 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(key = "back_button") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.exitBrowsing() }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Volver",
                                        style = MeloType.body,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            browseResult?.sections?.forEach { section ->
                                val sectionTitle = section.title
                                if (sectionTitle != null) {
                                    item(key = "section_$sectionTitle") {
                                        SectionHeader(title = sectionTitle)
                                    }
                                }
                                items(section.items) { item ->
                                    when (item) {
                                        is SearchResult.Song -> TrackRow(
                                            track = item.track,
                                            onClick = { queueViewModel.playTrack(item.track) }
                                        )

                                        is SearchResult.Album -> AlbumCard(
                                            title = item.title,
                                            subtitle = item.author,
                                            artworkUrl = item.artworkUrl,
                                            cardWidth = 140.dp,
                                            onClick = { onAlbumClick(item.id) }
                                        )

                                        is SearchResult.Artist -> ArtistCircle(
                                            name = item.name,
                                            artworkUrl = item.artworkUrl,
                                            size = 80.dp,
                                            onClick = { onArtistClick(item.id) }
                                        )

                                        is SearchResult.Playlist -> AlbumCard(
                                            title = item.title,
                                            subtitle = item.author,
                                            artworkUrl = item.artworkUrl,
                                            cardWidth = 140.dp,
                                            onClick = { onPlaylistClick(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                isFocused && uiState.query.isBlank() -> {
                    RecentSearchesList(
                        searches = uiState.recentSearches,
                        onQueryChange = {
                            viewModel.onQueryChange(it)
                            isFocused = false
                        }
                    )
                }

                uiState.query.isNotBlank() -> {
                    if (uiState.isSearching) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (uiState.results.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No se encontraron resultados",
                                style = MeloType.body,
                                color = MeloColors.textMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 24.dp,
                                end = 24.dp,
                                top = 8.dp,
                                bottom = paddingValues.calculateBottomPadding() + 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.results) { track ->
                                TrackRow(
                                    track = track,
                                    onClick = { queueViewModel.playTrack(track) }
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 32.dp
                        )
                    ) {
                        if (uiState.recentHistory.isNotEmpty()) {
                            item(key = "recent_history") {
                                SectionHeader(title = "Vuelve a escuchar")
                                SongFourRowCarousel(
                                    tracks = uiState.recentHistory,
                                    onTrackClick = queueViewModel::playTrack
                                )
                            }
                        }

                        uiState.exploreSections.forEach { section ->
                            when (section.type) {
                                HomeSectionType.SONGS -> {
                                    val tracks = section.items.filterIsInstance<SearchResult.Song>()
                                        .map { it.track }
                                    item(key = section.title) {
                                        SectionHeader(title = section.title)
                                        SongFourRowCarousel(
                                            tracks = tracks,
                                            onTrackClick = queueViewModel::playTrack
                                        )
                                    }
                                }

                                HomeSectionType.ALBUMS -> {
                                    item(key = section.title) {
                                        SectionHeader(title = section.title)
                                        AdaptiveLazyRow(
                                            items = section.items,
                                            minCardWidth = 140.dp,
                                            spacing = 12.dp
                                        ) { item, cardWidth ->
                                            when (item) {
                                                is SearchResult.Album -> AlbumCard(
                                                    title = item.title,
                                                    subtitle = item.author,
                                                    artworkUrl = item.artworkUrl,
                                                    cardWidth = cardWidth,
                                                    onClick = { onAlbumClick(item.id) }
                                                )

                                                is SearchResult.Playlist -> AlbumCard(
                                                    title = item.title,
                                                    subtitle = item.author,
                                                    artworkUrl = item.artworkUrl,
                                                    cardWidth = cardWidth,
                                                    onClick = { onPlaylistClick(item.id) }
                                                )

                                                is SearchResult.Artist -> ArtistCircle(
                                                    name = item.name,
                                                    artworkUrl = item.artworkUrl,
                                                    size = cardWidth,
                                                    onClick = { onArtistClick(item.id) }
                                                )

                                                else -> {}
                                            }
                                        }
                                    }
                                }

                                HomeSectionType.PLAYLISTS -> {
                                    item(key = section.title) {
                                        SectionHeader(title = section.title)
                                        AdaptiveLazyRow(
                                            items = section.items,
                                            minCardWidth = 140.dp,
                                            spacing = 14.dp
                                        ) { item, cardWidth ->
                                            when (item) {
                                                is SearchResult.Playlist -> AlbumCard(
                                                    title = item.title,
                                                    subtitle = item.author,
                                                    artworkUrl = item.artworkUrl,
                                                    cardWidth = cardWidth,
                                                    onClick = { onPlaylistClick(item.id) }
                                                )

                                                is SearchResult.Album -> AlbumCard(
                                                    title = item.title,
                                                    subtitle = item.author,
                                                    artworkUrl = item.artworkUrl,
                                                    cardWidth = cardWidth,
                                                    onClick = { onAlbumClick(item.id) }
                                                )

                                                else -> {}
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }

                        if (uiState.isLoadingMoodAndGenres) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else if (uiState.moodAndGenres.isNotEmpty()) {
                            uiState.moodAndGenres.forEach { group ->
                                item(key = "mood_${group.title}") {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            group.title,
                                            style = MeloType.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MeloColors.textPrimary,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )

                                        val columns = 5
                                        group.items.chunked(columns).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                rowItems.forEach { category ->
                                                    CategoryCard(
                                                        title = category.title,
                                                        color = Color(category.stripeColor.toInt()),
                                                        onClick = {
                                                            viewModel.browseCategory(
                                                                category.browseId,
                                                                category.params
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                repeat(columns - rowItems.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionsDropdown(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = {},
        modifier = modifier.fillMaxWidth()
    ) {
        suggestions.forEach { suggestion ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = suggestion,
                            style = MeloType.body,
                            color = MeloColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                onClick = { onSelect(suggestion) }
            )
        }
    }
}

@Composable
private fun RecentSearchesList(
    searches: List<String>,
    onQueryChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
    ) {
        item {
            Text(
                "Búsquedas recientes",
                style = MeloType.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        items(searches) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onQueryChange(query) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = query,
                    style = MeloType.body,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Buscar canciones, artistas, álbumes...",
                    style = MeloType.body,
                    color = MeloColors.textMuted
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MeloColors.textMuted
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Cuenta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MeloColors.glassSurface,
                unfocusedContainerColor = MeloColors.glassSurface.copy(alpha = 0.45f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = MeloColors.textPrimary,
                unfocusedTextColor = MeloColors.textPrimary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .blur(if (query.isEmpty()) 0.dp else 0.dp)
                .drawBehind {}
        )
    }
}