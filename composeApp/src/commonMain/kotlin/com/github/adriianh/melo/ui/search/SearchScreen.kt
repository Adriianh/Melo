package com.github.adriianh.melo.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.shadow
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
        SearchTopBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
            onOpenSettings = onOpenSettings,
            onFocusChanged = { focused ->
                isFocused = focused
                if (focused && uiState.query.isNotBlank()) {
                    viewModel.onQueryChange(uiState.query)
                }
            },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState = when {
                    uiState.isBrowsingCategory || uiState.browseCategoryResult != null -> "category"
                    uiState.query.isNotBlank() && !isFocused -> "results"
                    else -> "explore"
                }
            ) { state ->
                when (state) {
                    "category" -> CategoryBrowsingContent(
                        uiState,
                        paddingValues,
                        viewModel::exitBrowsing,
                        onAlbumClick,
                        onArtistClick,
                        onPlaylistClick,
                        queueViewModel
                    )

                    "results" -> SearchResultsContent(uiState, paddingValues, queueViewModel)
                    "explore" -> ExploreContent(
                        uiState,
                        paddingValues,
                        onAlbumClick,
                        onArtistClick,
                        onPlaylistClick,
                        viewModel::browseCategory,
                        queueViewModel
                    )
                }
            }

            val shouldShowOverlay = isFocused && (
                    (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) ||
                            (uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty())
                    )

            SearchOverlayWrapper(
                visible = shouldShowOverlay,
                query = uiState.query,
                suggestions = uiState.suggestions,
                recentSearches = uiState.recentSearches,
                onSelect = { selected ->
                    viewModel.onSuggestionSelected(selected)
                    isFocused = false
                }
            ) { isFocused = false }
        }
    }
}

@Composable
private fun SearchOverlayWrapper(
    visible: Boolean,
    query: String,
    suggestions: List<String>,
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .animateEnterExit(
                        enter = expandVertically(expandFrom = Alignment.Top),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top)
                    )
            ) {
                SearchActiveOverlay(
                    query = query,
                    suggestions = suggestions,
                    recentSearches = recentSearches,
                    onSelect = onSelect,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBrowsingContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    queueViewModel: QueueViewModel
) {
    if (uiState.isBrowsingCategory) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Volver", style = MeloType.body, color = MaterialTheme.colorScheme.primary)
                }
            }

            browseResult?.sections?.forEach { section ->
                section.title?.let { item { SectionHeader(title = it) } }
                items(section.items) { item ->
                    when (item) {
                        is SearchResult.Song -> TrackRow(
                            item.track,
                            onClick = { queueViewModel.playTrack(item.track) })

                        is SearchResult.Album -> AlbumCard(
                            item.title,
                            item.author,
                            item.artworkUrl,
                            onClick = { onAlbumClick(item.id) },
                            cardWidth = 140.dp
                        )

                        is SearchResult.Artist -> ArtistCircle(
                            item.name,
                            item.artworkUrl,
                            onClick = { onArtistClick(item.id) },
                            size = 80.dp
                        )

                        is SearchResult.Playlist -> AlbumCard(
                            item.title,
                            item.author,
                            item.artworkUrl,
                            onClick = { onPlaylistClick(item.id) },
                            cardWidth = 140.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    queueViewModel: QueueViewModel
) {
    if (uiState.isSearching) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (uiState.results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                TrackRow(track = track, onClick = { queueViewModel.playTrack(track) })
            }
        }
    }
}

@Composable
private fun ExploreContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onBrowseCategory: (String, String?) -> Unit,
    queueViewModel: QueueViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 32.dp
        )
    ) {
        if (uiState.recentHistory.isNotEmpty()) {
            item {
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
                    val tracks =
                        section.items.filterIsInstance<SearchResult.Song>().map { it.track }
                    item {
                        SectionHeader(title = section.title)
                        SongFourRowCarousel(
                            tracks = tracks,
                            onTrackClick = queueViewModel::playTrack
                        )
                    }
                }

                HomeSectionType.ALBUMS -> {
                    item {
                        SectionHeader(title = section.title)
                        AdaptiveLazyRow(section.items, 140.dp, 12.dp) { item, cardWidth ->
                            when (item) {
                                is SearchResult.Album -> AlbumCard(
                                    item.title,
                                    item.author,
                                    item.artworkUrl,
                                    onClick = { onAlbumClick(item.id) },
                                    cardWidth = cardWidth
                                )

                                is SearchResult.Playlist -> AlbumCard(
                                    item.title,
                                    item.author,
                                    item.artworkUrl,
                                    onClick = { onPlaylistClick(item.id) },
                                    cardWidth = cardWidth
                                )

                                is SearchResult.Artist -> ArtistCircle(
                                    item.name,
                                    item.artworkUrl,
                                    onClick = { onArtistClick(item.id) },
                                    size = cardWidth
                                )

                                else -> {}
                            }
                        }
                    }
                }

                HomeSectionType.PLAYLISTS -> {
                    item {
                        SectionHeader(title = section.title)
                        AdaptiveLazyRow(section.items, 140.dp, 14.dp) { item, cardWidth ->
                            when (item) {
                                is SearchResult.Playlist -> AlbumCard(
                                    item.title,
                                    item.author,
                                    item.artworkUrl,
                                    onClick = { onPlaylistClick(item.id) },
                                    cardWidth = cardWidth
                                )

                                is SearchResult.Album -> AlbumCard(
                                    item.title,
                                    item.author,
                                    item.artworkUrl,
                                    onClick = { onAlbumClick(item.id) },
                                    cardWidth = cardWidth
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
                item {
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { category ->
                                    CategoryCard(
                                        title = category.title,
                                        color = Color(category.stripeColor.toInt()),
                                        onClick = {
                                            onBrowseCategory(
                                                category.browseId,
                                                category.params
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(
                                        modifier = Modifier.weight(
                                            1f
                                        )
                                    )
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

@Composable
private fun SearchActiveOverlay(
    query: String,
    suggestions: List<String>,
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MeloColors.surface1.copy(alpha = 0.98f))
            .border(0.5.dp, MeloColors.glassBorder, RoundedCornerShape(20.dp))
            .clickable(enabled = false) {}
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            item {
                Crossfade(targetState = query.isBlank()) { isHistory ->
                    if (isHistory) {
                        if (recentSearches.isNotEmpty()) {
                            Column {
                                Text(
                                    "Búsquedas recientes",
                                    style = MeloType.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MeloColors.textMuted,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    )
                                )
                                recentSearches.forEach { search ->
                                    SearchItemRow(
                                        search,
                                        Icons.Default.History
                                    ) { onSelect(search) }
                                }
                            }
                        }
                    } else {
                        Column {
                            suggestions.forEach { suggestion ->
                                SearchItemRow(
                                    suggestion,
                                    Icons.Default.Search
                                ) { onSelect(suggestion) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchItemRow(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MeloColors.textMuted, modifier = Modifier.size(20.dp))
        Text(
            text,
            style = MeloType.body,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                    null,
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
                                null,
                                tint = MeloColors.textMuted
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.AccountCircle,
                            null,
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
            modifier = Modifier.weight(1f).onFocusChanged { onFocusChanged(it.isFocused) }
                .blur(if (query.isEmpty()) 0.dp else 0.dp).drawBehind {}
        )
    }
}
