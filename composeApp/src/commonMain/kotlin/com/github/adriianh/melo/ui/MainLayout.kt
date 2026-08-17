package com.github.adriianh.melo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.components.AnimatedEqualizerBars
import com.github.adriianh.melo.ui.player.DesktopNowPlayingDockedPane
import com.github.adriianh.melo.ui.player.PanelSection
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloMotion
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffold(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOpenNowPlaying: () -> Unit = {},
    onPlaylistClick: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    content: @Composable (String, PaddingValues) -> Unit
) {
    val platform = remember { getPlatform() }

    if (platform.type == PlatformType.DESKTOP) {
        DesktopMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onOpenNowPlaying = onOpenNowPlaying,
            onPlaylistClick = onPlaylistClick,
            content = { padding -> content(selectedTab, padding) }
        )
    } else {
        MobileMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onOpenNowPlaying = onOpenNowPlaying,
            content = { padding -> content(selectedTab, padding) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMainLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var isDesktopPaneVisible by remember { mutableStateOf(true) }
    var desktopPaneSection by remember { mutableStateOf(PanelSection.QUEUE) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            YouTubeStyleSidebar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onPlaylistClick = onPlaylistClick,
                collapsed = isSidebarCollapsed,
                onToggleCollapsed = { isSidebarCollapsed = !isSidebarCollapsed }
            )

            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f)) {
                    Scaffold(
                        containerColor = Color.Transparent,
                    ) { paddingValues ->
                        content(paddingValues)
                    }
                }

                DesktopPlayerBar(
                    onOpenNowPlaying = onOpenNowPlaying,
                    onToggleLyrics = {
                        isDesktopPaneVisible = true
                        desktopPaneSection = PanelSection.LYRICS
                    },
                    onToggleQueue = {
                        isDesktopPaneVisible = true
                        desktopPaneSection = PanelSection.QUEUE
                    },
                    onToggleDockedPane = {
                        isDesktopPaneVisible = !isDesktopPaneVisible
                    }
                )
            }

            AnimatedVisibility(
                visible = isDesktopPaneVisible,
                enter = expandHorizontally(
                    expandFrom = Alignment.End,
                    animationSpec = MeloMotion.medium()
                ) + fadeIn(animationSpec = MeloMotion.fast()),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.End,
                    animationSpec = MeloMotion.medium()
                ) + fadeOut(animationSpec = MeloMotion.fast())
            ) {
                DesktopNowPlayingDockedPane(
                    onExpandToFullscreen = onOpenNowPlaying,
                    onClose = { isDesktopPaneVisible = false },
                    selectedSection = desktopPaneSection,
                    onSectionChange = { desktopPaneSection = it }
                )
            }
        }
    }
}

@Composable
private fun YouTubeStyleSidebar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SidebarViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sidebarWidth by animateDpAsState(
        targetValue = if (collapsed) 72.dp else 240.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "sidebarWidth"
    )

    Column(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(MeloColors.playerBarFill)
            .drawBehind {
                drawLine(
                    color = MeloColors.playerBarBorder,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(
                    12.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (collapsed) 0.dp else 12.dp, vertical = 14.dp)
            ) {
                IconButton(onClick = onToggleCollapsed, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = if (collapsed) "Expandir barra lateral" else "Colapsar barra lateral",
                        tint = MeloColors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (!collapsed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = { onTabSelected("Home") })
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MeloColors.brandAccent,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = "Melo Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            "Melo",
                            style = MeloType.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Crossfade(
                targetState = collapsed,
                animationSpec = tween(durationMillis = 150),
                label = "sidebarContent"
            ) { isCollapsed ->
                if (isCollapsed) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CollapsedRailItem(
                            label = "Inicio",
                            icon = Icons.Default.Home,
                            isSelected = selectedTab == "Home",
                            onClick = { onTabSelected("Home") }
                        )
                        CollapsedRailItem(
                            label = "Explorar",
                            icon = Icons.Default.Explore,
                            isSelected = selectedTab == "Search",
                            onClick = { onTabSelected("Home") }
                        )
                        CollapsedRailItem(
                            label = "Biblioteca",
                            icon = Icons.Default.LibraryMusic,
                            isSelected = selectedTab == "Library",
                            onClick = { onTabSelected("Library") }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SidebarRow(
                                label = "Inicio",
                                icon = Icons.Default.Home,
                                isSelected = selectedTab == "Home",
                                onClick = { onTabSelected("Home") }
                            )
                            SidebarRow(
                                label = "Explorar",
                                icon = Icons.Default.Explore,
                                isSelected = selectedTab == "Search",
                                onClick = { onTabSelected("Home") }
                            )
                            SidebarRow(
                                label = "Tu Biblioteca",
                                icon = Icons.Default.LibraryMusic,
                                isSelected = selectedTab == "Library",
                                onClick = { onTabSelected("Library") }
                            )
                        }

                        HorizontalDivider(color = MeloColors.border, thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(MeloColors.surface2)
                                .clickable(onClick = { onTabSelected("Library") })
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Nueva lista",
                                tint = MeloColors.brandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Nueva lista de reproducción",
                                style = MeloType.body,
                                fontWeight = FontWeight.SemiBold,
                                color = MeloColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SidebarRow(
                                label = "Canciones Favoritas",
                                icon = Icons.Default.Favorite,
                                isSelected = false,
                                onClick = { onTabSelected("Library") }
                            )
                            SidebarRow(
                                label = "Historial",
                                icon = Icons.Default.History,
                                isSelected = false,
                                onClick = { onTabSelected("Library") }
                            )
                        }

                        if (state.playlists.isNotEmpty()) {
                            Text(
                                "TUS PLAYLISTS",
                                style = MeloType.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            state.playlists.forEach { playlist ->
                                SidebarPlaylistRow(
                                    title = playlist.title,
                                    artworkUrl = playlist.artworkUrl,
                                    onClick = {
                                        onPlaylistClick(
                                            playlist.id,
                                            playlist.title,
                                            playlist.artworkUrl,
                                            playlist.author
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedRailItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MeloColors.surface2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) MeloColors.brandAccent else MeloColors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MeloType.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MeloColors.textPrimary else MeloColors.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun SidebarRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MeloColors.surface2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) MeloColors.brandAccent else MeloColors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            style = MeloType.body,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else MeloColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SidebarPlaylistRow(
    title: String,
    artworkUrl: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            MeloAsyncImage(
                url = artworkUrl,
                contentDescription = title,
                size = 28.dp,
                shape = RoundedCornerShape(4.dp)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MeloColors.surface2,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            title,
            style = MeloType.body,
            color = MeloColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DesktopPlayerBar(
    onOpenNowPlaying: () -> Unit = {},
    onToggleLyrics: () -> Unit = {},
    onToggleQueue: () -> Unit = {},
    onToggleDockedPane: () -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val activeAccent =
        if (state.accentColor != MeloColors.textMuted) state.accentColor else MeloColors.brandAccent

    if (!state.hasTrack) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(MeloColors.playerBarFill)
            .border(0.5.dp, MeloColors.playerBarBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .width(260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleDockedPane)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    MeloAsyncImage(
                        url = state.albumArt,
                        contentDescription = state.title,
                        size = 52.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.title,
                            style = MeloType.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.isPlaying) {
                            AnimatedEqualizerBars(accentColor = activeAccent)
                        }
                    }
                    Text(
                        state.artist,
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::toggleShuffle) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) activeAccent else MeloColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = viewModel::playPrevious, enabled = state.hasPrevious) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (state.hasPrevious) MeloColors.textPrimary else MeloColors.textMuted.copy(
                                alpha = 0.4f
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = viewModel::togglePlayPause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = activeAccent
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = viewModel::playNext, enabled = state.hasNext) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (state.hasNext) MeloColors.textPrimary else MeloColors.textMuted.copy(
                                alpha = 0.4f
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = viewModel::toggleRepeat) {
                        val icon = when (state.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }
                        val tint = when (state.repeatMode) {
                            RepeatMode.NONE -> MeloColors.textMuted
                            else -> activeAccent
                        }
                        Icon(
                            icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        state.elapsedLabel,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted
                    )
                    Slider(
                        value = state.progressFraction,
                        onValueChange = { fraction ->
                            if (state.durationMs > 0) {
                                viewModel.seekTo((fraction * state.durationMs).toLong())
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = activeAccent,
                            activeTrackColor = activeAccent,
                            inactiveTrackColor = MeloColors.borderStrong
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        state.remainingLabel,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted
                    )
                }
            }

            Row(
                modifier = Modifier.width(220.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleLyrics) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Letras en panel",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onToggleQueue) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Cola en panel",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onOpenNowPlaying) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Expandir pantalla completa",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileMainLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                MobilePlayerBar(onOpenNowPlaying = onOpenNowPlaying)
                Spacer(modifier = Modifier.height(4.dp))
                NavigationBar(containerColor = Color.Transparent) {
                    NavigationBarItem(
                        selected = selectedTab == "Home",
                        onClick = { onTabSelected("Home") },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Inicio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MeloColors.brandAccent,
                            selectedTextColor = MeloColors.brandAccent,
                            unselectedIconColor = MeloColors.textMuted,
                            unselectedTextColor = MeloColors.textMuted,
                            indicatorColor = MeloColors.surface1
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "Library",
                        onClick = { onTabSelected("Library") },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Biblioteca") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MeloColors.brandAccent,
                            selectedTextColor = MeloColors.brandAccent,
                            unselectedIconColor = MeloColors.textMuted,
                            unselectedTextColor = MeloColors.textMuted,
                            indicatorColor = MeloColors.surface1
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Composable
private fun MobilePlayerBar(
    onOpenNowPlaying: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val activeAccent =
        if (state.accentColor != MeloColors.textMuted) state.accentColor else MeloColors.brandAccent

    if (!state.hasTrack) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MeloColors.playerBarFill)
            .border(0.5.dp, MeloColors.playerBarBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenNowPlaying)
    ) {
        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = activeAccent,
            trackColor = Color.Transparent,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MeloAsyncImage(
                url = state.albumArt,
                contentDescription = state.title,
                size = 40.dp,
                shape = RoundedCornerShape(6.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.title,
                        style = MeloType.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.isPlaying) {
                        AnimatedEqualizerBars(accentColor = activeAccent)
                    }
                }
                Text(
                    state.artist,
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = viewModel::togglePlayPause) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = activeAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = viewModel::playNext, enabled = state.hasNext) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = if (state.hasNext) MeloColors.textPrimary else MeloColors.textMuted.copy(
                        alpha = 0.4f
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}