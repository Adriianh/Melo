package com.github.adriianh.melo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.navigation.MainTab
import com.github.adriianh.melo.ui.player.DesktopNowPlayingDockedPane
import com.github.adriianh.melo.ui.player.PanelSection
import com.github.adriianh.melo.util.LocalMeloColors
import com.github.adriianh.melo.util.MeloMotion
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform

val LocalSelectionMode = compositionLocalOf { mutableStateOf(false) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffold(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onOpenNowPlaying: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onPlaylistClick: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    isDarkTheme: Boolean = true,
    isSelectionMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    content: @Composable (MainTab, PaddingValues) -> Unit
) {
    val platform = remember { getPlatform() }

    if (platform.type == PlatformType.DESKTOP) {
        DesktopMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onOpenNowPlaying = onOpenNowPlaying,
            onOpenSettings = onOpenSettings,
            onPlaylistClick = onPlaylistClick,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            content = { padding -> content(selectedTab, padding) }
        )
    } else {
        MobileMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onOpenNowPlaying = onOpenNowPlaying,
            isSelectionMode = isSelectionMode,
            content = { padding -> content(selectedTab, padding) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMainLayout(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var isDesktopPaneVisible by remember { mutableStateOf(true) }
    var desktopPaneSection by remember { mutableStateOf(PanelSection.QUEUE) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            MainSidebar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onPlaylistClick = onPlaylistClick,
                collapsed = isSidebarCollapsed,
                onToggleCollapsed = { isSidebarCollapsed = !isSidebarCollapsed },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onOpenSettings = onOpenSettings
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                val currentColors = LocalMeloColors.current
                val glassTint =
                    if (isDarkTheme) currentColors.glassSurface else Color.Black.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    glassTint.copy(alpha = if (isDarkTheme) 0.18f else 0.12f),
                                    glassTint.copy(alpha = if (isDarkTheme) 0.06f else 0.04f)
                                )
                            )
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isDarkTheme) currentColors.border.copy(alpha = 0.2f)
                            else Color.Black.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
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
                    },
                    modifier = Modifier.fillMaxWidth()
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
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    selectedSection = desktopPaneSection,
                    onSectionChange = { desktopPaneSection = it }
                )
            }
        }
    }
}

@Composable
private fun MobileMainLayout(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onOpenNowPlaying: () -> Unit,
    isSelectionMode: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content(PaddingValues(bottom = if (isSelectionMode) 80.dp else 160.dp))

        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = MeloMotion.medium()
            ) + fadeIn(animationSpec = MeloMotion.fast()),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = MeloMotion.medium()
            ) + fadeOut(animationSpec = MeloMotion.fast()),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp)
            ) {
                MobilePlayerBar(onOpenNowPlaying = onOpenNowPlaying)
            }
        }

        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = MeloMotion.medium()
            ) + fadeIn(animationSpec = MeloMotion.fast()),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = MeloMotion.medium()
            ) + fadeOut(animationSpec = MeloMotion.fast()),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                FloatingNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    }
}