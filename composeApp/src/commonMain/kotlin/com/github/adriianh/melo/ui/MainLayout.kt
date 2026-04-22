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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.surfaceColorAtElevation
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
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffold(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    content: @Composable (String, PaddingValues) -> Unit
) {
    val platform = remember { getPlatform() }

    if (platform.type == PlatformType.DESKTOP) {
        DesktopMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            content = { padding -> content(selectedTab, padding) }
        )
    } else {
        MobileMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            content = { padding -> content(selectedTab, padding) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMainLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            // Sidebar (NavigationRail) - Spotify Style
            NavigationRail(
                modifier = Modifier.fillMaxHeight().width(240.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Text(
                        "Melo",
                        style = MaterialTheme.typography.headlineMedium,
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

            // Main Content area
            Box(modifier = Modifier.weight(1f)) {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(selectedTab) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                scrolledContainerColor = Color.Unspecified,
                                navigationIconContentColor = Color.Unspecified,
                                titleContentColor = Color.Unspecified,
                                actionIconContentColor = Color.Unspecified
                            )
                        )
                    }
                ) { paddingValues ->
                    content(paddingValues)
                }
            }
        }

        // Player Bar (Persistent at bottom)
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
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun AdaptivePlayerBar(
    viewModel: PlayerViewModel = koinViewModel()
) {
    val platform = remember { getPlatform() }
    val state by viewModel.playbackState.collectAsState()

    if (state.currentTrack == null) return

    if (platform.type == PlatformType.DESKTOP) {
        DesktopPlayerBar(state, viewModel::togglePlayPause)
    } else {
        MobileMiniPlayer(state, viewModel::togglePlayPause)
    }
}

@Composable
private fun DesktopPlayerBar(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    // Artwork would go here
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        state.currentTrack?.title ?: "No track playing",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        state.currentTrack?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowLeft, "Previous")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, "Next")
                }
            }

            // Volume / Extra
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, "Volume", modifier = Modifier.size(20.dp))
                Slider(
                    value = 0.5f,
                    onValueChange = {},
                    modifier = Modifier.width(120.dp).padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MobileMiniPlayer(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Info
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.currentTrack?.title ?: "No track playing",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    state.currentTrack?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Minimal Controls
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MobileMainLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            Column {
                AdaptivePlayerBar()
                NavigationBar {
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
