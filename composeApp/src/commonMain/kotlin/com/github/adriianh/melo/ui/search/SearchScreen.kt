package com.github.adriianh.melo.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import com.github.adriianh.melo.ui.components.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.components.VideoCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
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
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SearchTopBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = { q ->
                isFocused = false
                focusManager.clearFocus()
                viewModel.executeSearch(q)
            },
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

        AnimatedVisibility(
            visible = uiState.query.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SearchFilterChipsRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { filter ->
                    isFocused = false
                    focusManager.clearFocus()
                    viewModel.onFilterSelected(filter)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState = when {
                    uiState.isBrowsingCategory || uiState.browseCategoryResult != null -> "category"
                    uiState.query.isNotBlank() -> "results"
                    else -> "explore"
                }
            ) { state ->
                when (state) {
                    "category" -> CategoryBrowsingContent(
                        uiState = uiState,
                        paddingValues = paddingValues,
                        onBack = viewModel::exitBrowsing,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                        queueViewModel = queueViewModel
                    )

                    "results" -> SearchResultsContent(
                        uiState = uiState,
                        paddingValues = paddingValues,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                        queueViewModel = queueViewModel
                    )

                    "explore" -> ExploreContent(
                        uiState = uiState,
                        paddingValues = paddingValues,
                        onAlbumClick = onAlbumClick,
                        onPlaylistClick = onPlaylistClick,
                        onBrowseCategory = viewModel::browseCategory,
                        queueViewModel = queueViewModel
                    )
                }
            }

            val shouldShowOverlay = isFocused && (
                    (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) ||
                            (uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty())
                    )

            if (shouldShowOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isFocused = false
                            focusManager.clearFocus()
                        }
                )
            }

            SearchOverlayWrapper(
                visible = shouldShowOverlay,
                query = uiState.query,
                suggestions = uiState.suggestions,
                recentSearches = uiState.recentSearches,
                onSelect = { selectedQuery ->
                    isFocused = false
                    focusManager.clearFocus()
                    viewModel.onSuggestionSelected(selectedQuery)
                }
            )
        }
    }
}

@Composable
private fun SearchFilterChipsRow(
    selectedFilter: SearchFilterType,
    onFilterSelected: (SearchFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchFilterType.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val activeColor = MaterialTheme.colorScheme.primary
            val bgAlpha by animateColorAsState(
                targetValue = if (isSelected) activeColor else MeloColors.surface1,
                animationSpec = tween(250),
                label = "chip_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MeloColors.textSecondary,
                animationSpec = tween(250),
                label = "chip_text"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgAlpha)
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    style = MeloType.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun SearchOverlayWrapper(
    visible: Boolean,
    query: String,
    suggestions: List<String>,
    recentSearches: List<String>,
    onSelect: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            SearchActiveOverlay(
                query = query,
                suggestions = suggestions,
                recentSearches = recentSearches,
                onSelect = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            )
        }
    }
}

@Composable
private fun TopResultHeroCard(
    item: SearchResult,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    queueViewModel: QueueViewModel,
    modifier: Modifier = Modifier
) {
    val isArtist = item is SearchResult.Artist
    val title = when (item) {
        is SearchResult.Artist -> item.name
        is SearchResult.Song -> item.track.title
        is SearchResult.Album -> item.title
        is SearchResult.Playlist -> item.title
    }
    val subtitle = when (item) {
        is SearchResult.Artist -> "Artista"
        is SearchResult.Song -> item.track.artist
        is SearchResult.Album -> item.author + (item.year?.let { " • $it" } ?: "")
        is SearchResult.Playlist -> item.author
    }
    val artworkUrl = when (item) {
        is SearchResult.Artist -> item.artworkUrl
        is SearchResult.Song -> item.track.artworkUrl
        is SearchResult.Album -> item.artworkUrl
        is SearchResult.Playlist -> item.artworkUrl
    }
    val badgeText = when (item) {
        is SearchResult.Artist -> "ARTISTA"
        is SearchResult.Song -> "CANCIÓN"
        is SearchResult.Album -> "ÁLBUM"
        is SearchResult.Playlist -> "PLAYLIST"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MeloColors.surface2.copy(alpha = 0.85f),
                        MeloColors.surface1.copy(alpha = 0.65f)
                    )
                )
            )
            .border(0.75.dp, MeloColors.glassBorder, RoundedCornerShape(20.dp))
            .clickable {
                when (item) {
                    is SearchResult.Artist -> onArtistClick(item.id)
                    is SearchResult.Song -> queueViewModel.playTrack(item.track)
                    is SearchResult.Album -> onAlbumClick(item.id)
                    is SearchResult.Playlist -> onPlaylistClick(item.id)
                }
            }
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isArtist) {
                    MeloAsyncImage(
                        url = artworkUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        shape = CircleShape
                    )
                } else {
                    MeloAsyncImage(
                        url = artworkUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shadow(8.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MeloType.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MeloType.body,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        when (item) {
                            is SearchResult.Song -> queueViewModel.playTrack(item.track)
                            is SearchResult.Artist -> onArtistClick(item.id)
                            is SearchResult.Album -> onAlbumClick(item.id)
                            is SearchResult.Playlist -> onPlaylistClick(item.id)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(8.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    queueViewModel: QueueViewModel
) {
    if (uiState.isSearching) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(6) {
                SuggestionSkeletonCard()
            }
        }
    } else if (!uiState.hasResults) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "No se encontraron resultados",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary
                )
                Text(
                    "Intenta buscar con otras palabras o cambiar de filtro",
                    style = MeloType.body,
                    color = MeloColors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        when (uiState.selectedFilter) {
            SearchFilterType.ALL -> {
                val allSongs = uiState.summarySections
                    .flatMap { it.items }
                    .filterIsInstance<SearchResult.Song>()
                    .map { it.track }
                    .distinctBy { it.id }

                val topResult = uiState.summarySections.firstOrNull()?.items?.firstOrNull()
                val otherSections = uiState.summarySections.filter { section ->
                    section.type != HomeSectionType.SONGS && section.type != HomeSectionType.VIDEOS
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWide = maxWidth >= 760.dp

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            if (topResult != null) {
                                if (isWide) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1.1f)) {
                                            SectionHeader(title = "Mejor resultado")
                                            Spacer(modifier = Modifier.height(10.dp))
                                            TopResultHeroCard(
                                                item = topResult,
                                                onAlbumClick = onAlbumClick,
                                                onArtistClick = onArtistClick,
                                                onPlaylistClick = onPlaylistClick,
                                                queueViewModel = queueViewModel,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (allSongs.isNotEmpty()) {
                                            Column(modifier = Modifier.weight(1.5f)) {
                                                SectionHeader(title = "Canciones principales")
                                                Spacer(modifier = Modifier.height(10.dp))
                                                allSongs.take(4).forEach { track ->
                                                    TrackRow(
                                                        track = track,
                                                        onClick = { queueViewModel.playTrack(track) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            SectionHeader(title = "Mejor resultado")
                                            Spacer(modifier = Modifier.height(10.dp))
                                            TopResultHeroCard(
                                                item = topResult,
                                                onAlbumClick = onAlbumClick,
                                                onArtistClick = onArtistClick,
                                                onPlaylistClick = onPlaylistClick,
                                                queueViewModel = queueViewModel,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (allSongs.isNotEmpty()) {
                                            Column {
                                                SectionHeader(title = "Canciones")
                                                Spacer(modifier = Modifier.height(8.dp))
                                                allSongs.take(4).forEach { track ->
                                                    TrackRow(
                                                        track = track,
                                                        onClick = { queueViewModel.playTrack(track) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (allSongs.isNotEmpty()) {
                                Column {
                                    SectionHeader(title = "Canciones")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    allSongs.take(5).forEach { track ->
                                        TrackRow(
                                            track = track,
                                            onClick = { queueViewModel.playTrack(track) }
                                        )
                                    }
                                }
                            }
                        }

                        if (allSongs.size > 4) {
                            item {
                                SectionHeader(title = "Más canciones")
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    allSongs.drop(4).take(6).forEach { track ->
                                        TrackRow(
                                            track = track,
                                            onClick = { queueViewModel.playTrack(track) }
                                        )
                                    }
                                }
                            }
                        }

                        otherSections.forEach { section ->
                            when (section.type) {
                                HomeSectionType.ALBUMS -> {
                                    item {
                                        val albums =
                                            section.items.filterIsInstance<SearchResult.Album>()
                                        if (albums.isNotEmpty()) {
                                            SectionHeader(title = section.title.ifBlank { "Álbumes" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AdaptiveLazyRow(
                                                items = albums,
                                                minCardWidth = 140.dp,
                                                spacing = 12.dp,
                                                horizontalPadding = 0.dp
                                            ) { album, width ->
                                                AlbumCard(
                                                    title = album.title,
                                                    subtitle = album.author,
                                                    artworkUrl = album.artworkUrl,
                                                    cardWidth = width,
                                                    onClick = { onAlbumClick(album.id) }
                                                )
                                            }
                                        }
                                    }
                                }

                                HomeSectionType.ARTISTS -> {
                                    item {
                                        val artists =
                                            section.items.filterIsInstance<SearchResult.Artist>()
                                        if (artists.isNotEmpty()) {
                                            SectionHeader(title = section.title.ifBlank { "Artistas" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AdaptiveLazyRow(
                                                items = artists,
                                                minCardWidth = 110.dp,
                                                spacing = 12.dp,
                                                horizontalPadding = 0.dp
                                            ) { artist, width ->
                                                ArtistCircle(
                                                    name = artist.name,
                                                    artworkUrl = artist.artworkUrl,
                                                    onClick = { onArtistClick(artist.id) },
                                                    size = width
                                                )
                                            }
                                        }
                                    }
                                }

                                HomeSectionType.PLAYLISTS -> {
                                    item {
                                        val playlists =
                                            section.items.filterIsInstance<SearchResult.Playlist>()
                                        if (playlists.isNotEmpty()) {
                                            SectionHeader(title = section.title.ifBlank { "Playlists" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AdaptiveLazyRow(
                                                items = playlists,
                                                minCardWidth = 140.dp,
                                                spacing = 12.dp,
                                                horizontalPadding = 0.dp
                                            ) { playlist, width ->
                                                AlbumCard(
                                                    title = playlist.title,
                                                    subtitle = playlist.author,
                                                    artworkUrl = playlist.artworkUrl,
                                                    cardWidth = width,
                                                    onClick = { onPlaylistClick(playlist.id) }
                                                )
                                            }
                                        }
                                    }
                                }

                                HomeSectionType.MIXED -> {
                                    item {
                                        SectionHeader(title = section.title)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            section.items.forEach { item ->
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

                                else -> {}
                            }
                        }
                    }
                }
            }

            SearchFilterType.SONGS -> {
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
                    items(uiState.songResults) { track ->
                        TrackRow(track = track, onClick = { queueViewModel.playTrack(track) })
                    }
                }
            }

            SearchFilterType.ALBUMS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        uiState.albumResults,
                        key = { index, item -> "album_${index}_${item.id}" }) { _, album ->
                        AlbumCard(
                            title = album.title,
                            subtitle = album.author,
                            artworkUrl = album.artworkUrl,
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                }
            }

            SearchFilterType.ARTISTS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        uiState.artistResults,
                        key = { index, item -> "artist_${index}_${item.id}" }) { _, artist ->
                        ArtistCircle(
                            name = artist.name,
                            artworkUrl = artist.artworkUrl,
                            onClick = { onArtistClick(artist.id) },
                            size = 120.dp
                        )
                    }
                }
            }

            SearchFilterType.PLAYLISTS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        uiState.playlistResults,
                        key = { index, item -> "pl_${index}_${item.id}" }) { _, playlist ->
                        AlbumCard(
                            title = playlist.title,
                            subtitle = playlist.author,
                            artworkUrl = playlist.artworkUrl,
                            onClick = { onPlaylistClick(playlist.id) }
                        )
                    }
                }
            }

            SearchFilterType.VIDEOS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(260.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        uiState.videoResults,
                        key = { index, track -> "video_${index}_${track.id}" }
                    ) { _, track ->
                        VideoCard(
                            title = track.title,
                            subtitle = track.artist,
                            artworkUrl = track.artworkUrl,
                            onClick = { queueViewModel.playTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onBrowseCategory: (String, String?) -> Unit,
    queueViewModel: QueueViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (uiState.recentHistory.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Búsquedas recientes",
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                SongFourRowCarousel(
                    tracks = uiState.recentHistory,
                    onTrackClick = { track -> queueViewModel.playTrack(track) },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        uiState.exploreSections.forEach { section ->
            when (section.type) {
                HomeSectionType.SONGS -> {
                    val songs =
                        section.items.filterIsInstance<SearchResult.Song>().map { it.track }
                    if (songs.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            SongFourRowCarousel(
                                tracks = songs,
                                onTrackClick = { track -> queueViewModel.playTrack(track) },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                HomeSectionType.MIXED -> {
                    item {
                        SectionHeader(
                            title = section.title,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        AdaptiveLazyRow(
                            items = section.items,
                            minCardWidth = 140.dp,
                            spacing = 12.dp,
                            horizontalPadding = 24.dp
                        ) { item, cardWidth ->
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
            .shadow(16.dp, RoundedCornerShape(20.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
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
    onSearch: (String) -> Unit,
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
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
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyUp) {
                        onSearch(query)
                        true
                    } else {
                        false
                    }
                }
                .blur(if (query.isEmpty()) 0.dp else 0.dp)
                .drawBehind {}
        )
    }
}
