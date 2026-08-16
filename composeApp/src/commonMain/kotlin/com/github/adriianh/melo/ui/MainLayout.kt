package com.github.adriianh.melo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffold(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onLoginClick: () -> Unit,
    content: @Composable (String, PaddingValues) -> Unit
) {
    val platform = remember { getPlatform() }

    if (platform.type == PlatformType.DESKTOP) {
        DesktopMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onLoginClick = onLoginClick,
            content = { padding -> content(selectedTab, padding) }
        )
    } else {
        MobileMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onLoginClick = onLoginClick,
            content = { padding -> content(selectedTab, padding) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMainLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onLoginClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MeloColors.surface0)) {
        Row(modifier = Modifier.weight(1f)) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight().width(240.dp),
                containerColor = MeloColors.surface1,
                header = {
                    Text(
                        "Melo",
                        style = MeloType.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                NavigationItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    selected = selectedTab == "Home",
                    onClick = { onTabSelected("Home") }
                )
                NavigationItem(
                    label = "Search",
                    icon = Icons.Default.Search,
                    selected = selectedTab == "Search",
                    onClick = { onTabSelected("Search") }
                )
                NavigationItem(
                    label = "Library",
                    icon = Icons.Default.LibraryMusic,
                    selected = selectedTab == "Library",
                    onClick = { onTabSelected("Library") }
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Box(modifier = Modifier.weight(1f)) {
                Scaffold(
                    containerColor = MeloColors.surface0,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(selectedTab, style = MeloType.titleLarge) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MeloColors.surface0,
                                scrolledContainerColor = Color.Unspecified,
                                navigationIconContentColor = Color.Unspecified,
                                titleContentColor = MeloColors.textPrimary,
                                actionIconContentColor = Color.Unspecified
                            ),
                            actions = {
                                IconButton(onClick = onLoginClick) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "Sign in to YouTube Music",
                                        tint = MeloColors.textPrimary
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    content(paddingValues)
                }
            }
        }

        AdaptivePlayerBar()
    }
}

@Composable
private fun NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        alwaysShowLabel = true,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MeloColors.textMuted
        )
    )
}

@Composable
private fun AdaptivePlayerBar(
    viewModel: PlayerViewModel = koinViewModel()
) {
    val platform = remember { getPlatform() }
    val state by viewModel.uiState.collectAsState()

    if (!state.hasTrack) return

    if (platform.type == PlatformType.DESKTOP) {
        DesktopPlayerBar(
            state = state,
            onTogglePlayPause = viewModel::togglePlayPause,
            onPlayPrevious = viewModel::playPrevious,
            onPlayNext = viewModel::playNext,
            onSeekTo = viewModel::seekTo,
            onToggleShuffle = viewModel::toggleShuffle,
            onToggleRepeat = viewModel::toggleRepeat,
        )
    } else {
        MobileMiniPlayer(
            state = state,
            onTogglePlayPause = viewModel::togglePlayPause,
            onPlayNext = viewModel::playNext,
        )
    }
}

@Composable
private fun DesktopPlayerBar(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        color = MeloColors.surface2,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.width(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    size = 44.dp,
                )
                Column {
                    Text(
                        state.title,
                        style = MeloType.labelMedium,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.artist,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Outlined.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MeloColors.textMuted,
                        )
                    }
                    IconButton(onClick = onPlayPrevious) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = MeloColors.textPrimary
                        )
                    }
                    FilledIconButton(onClick = onTogglePlayPause) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                        )
                    }
                    IconButton(onClick = onPlayNext) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = MeloColors.textPrimary
                        )
                    }
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            when (state.repeatMode) {
                                RepeatMode.ONE -> Icons.Outlined.Repeat
                                RepeatMode.ALL -> Icons.Outlined.Repeat
                                RepeatMode.NONE -> Icons.Outlined.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = when (state.repeatMode) {
                                RepeatMode.NONE -> MeloColors.textMuted
                                else -> MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier.widthIn(max = 320.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        state.elapsedLabel,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted,
                    )
                    Slider(
                        value = state.progressFraction,
                        onValueChange = { onSeekTo((it * 1000f).toLong()) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        state.remainingLabel,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { /* lyrics */ }) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Lyrics",
                        tint = MeloColors.textMuted
                    )
                }
                IconButton(onClick = { /* queue */ }) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = MeloColors.textMuted
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    modifier = Modifier.size(20.dp),
                    tint = MeloColors.textMuted
                )
                Slider(
                    value = 0.5f,
                    onValueChange = {},
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

@Composable
private fun MobileMiniPlayer(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium),
            color = MeloColors.surface2,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    size = 40.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.title,
                        style = MeloType.labelMedium,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.artist,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onPlayNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MeloColors.textPrimary,
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
        )
    }
}

@Composable
private fun MobileMainLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onLoginClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MeloColors.surface0,
        bottomBar = {
            Column {
                AdaptivePlayerBar()
                NavigationBar(containerColor = MeloColors.surface2) {
                    NavigationBarItem(
                        selected = selectedTab == "Home",
                        onClick = { onTabSelected("Home") },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "Search",
                        onClick = { onTabSelected("Search") },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "Library",
                        onClick = { onTabSelected("Library") },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}