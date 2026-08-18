package com.github.adriianh.melo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.adriianh.melo.theme.MeloTheme
import com.github.adriianh.melo.ui.AdaptiveScaffold
import com.github.adriianh.melo.ui.components.AmbientCanvas
import com.github.adriianh.melo.ui.detail.AlbumDetailScreen
import com.github.adriianh.melo.ui.detail.ArtistDetailScreen
import com.github.adriianh.melo.ui.detail.PlaylistDetailScreen
import com.github.adriianh.melo.ui.home.HomeScreen
import com.github.adriianh.melo.ui.library.LibraryScreen
import com.github.adriianh.melo.ui.login.LoginDialog
import com.github.adriianh.melo.ui.login.LoginViewModel
import com.github.adriianh.melo.ui.player.NowPlayingScreen
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloMotion
import org.koin.compose.viewmodel.koinViewModel

sealed interface ScreenDestination {
    data object Home : ScreenDestination
    data object Library : ScreenDestination
    data class Album(
        val id: String,
        val title: String = "",
        val artwork: String? = null,
        val author: String = ""
    ) : ScreenDestination

    data class Playlist(
        val id: String,
        val title: String = "",
        val artwork: String? = null,
        val author: String = ""
    ) : ScreenDestination

    data class Artist(val id: String, val name: String = "", val artwork: String? = null) :
        ScreenDestination
}

@Composable
expect fun InitImageLoader()

@Composable
fun App() {
    InitImageLoader()

    val playerViewModel: PlayerViewModel = koinViewModel()
    val accentPalette by playerViewModel.accentPalette.collectAsState()

    MeloTheme(accentColor = accentPalette.dominant) {
        var selectedTab by remember { mutableStateOf("Home") }
        var showLoginDialog by remember { mutableStateOf(false) }
        var isNowPlayingExpanded by remember { mutableStateOf(false) }
        var navigationStack by remember { mutableStateOf(listOf<ScreenDestination>(ScreenDestination.Home)) }
        val loginViewModel: LoginViewModel = koinViewModel()

        val currentScreen = navigationStack.lastOrNull() ?: ScreenDestination.Home

        fun navigateTo(destination: ScreenDestination) {
            navigationStack = navigationStack + destination
        }

        fun navigateBack() {
            if (navigationStack.size > 1) {
                navigationStack = navigationStack.dropLast(1)
            }
        }

        AmbientCanvas(
            accentColor = accentPalette.dominant,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdaptiveScaffold(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        navigationStack =
                            listOf(if (tab == "Home") ScreenDestination.Home else ScreenDestination.Library)
                    },
                    onOpenNowPlaying = { isNowPlayingExpanded = true },
                    onPlaylistClick = { id, title, artwork, author ->
                        navigateTo(ScreenDestination.Playlist(id, title, artwork, author))
                    }
                ) { _, paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (currentScreen) {
                            ScreenDestination.Home -> HomeScreen(
                                onAlbumClick = { id -> navigateTo(ScreenDestination.Album(id)) },
                                onPlaylistClick = { id -> navigateTo(ScreenDestination.Playlist(id)) },
                                onArtistClick = { id -> navigateTo(ScreenDestination.Artist(id)) },
                                onLoginClick = { showLoginDialog = true },
                            )

                            ScreenDestination.Library -> LibraryScreen(
                                onLoginClick = { showLoginDialog = true },
                                onAlbumClick = { id -> navigateTo(ScreenDestination.Album(id)) },
                                onPlaylistClick = { id -> navigateTo(ScreenDestination.Playlist(id)) },
                                onArtistClick = { id -> navigateTo(ScreenDestination.Artist(id)) },
                            )

                            is ScreenDestination.Album -> AlbumDetailScreen(
                                albumId = currentScreen.id,
                                initialTitle = currentScreen.title,
                                initialArtwork = currentScreen.artwork,
                                initialAuthor = currentScreen.author,
                                onBack = ::navigateBack
                            )

                            is ScreenDestination.Playlist -> PlaylistDetailScreen(
                                playlistId = currentScreen.id,
                                initialTitle = currentScreen.title,
                                initialArtwork = currentScreen.artwork,
                                initialAuthor = currentScreen.author,
                                onBack = ::navigateBack
                            )

                            is ScreenDestination.Artist -> ArtistDetailScreen(
                                artistId = currentScreen.id,
                                initialName = currentScreen.name,
                                initialArtwork = currentScreen.artwork,
                                onBack = ::navigateBack,
                                onAlbumClick = { id -> navigateTo(ScreenDestination.Album(id)) }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isNowPlayingExpanded,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = MeloMotion.slow()
                    ) + fadeIn(
                        animationSpec = MeloMotion.medium()
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = MeloMotion.slow()
                    ) + fadeOut(
                        animationSpec = MeloMotion.medium()
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    NowPlayingScreen(
                        onCollapse = { isNowPlayingExpanded = false }
                    )
                }

                if (showLoginDialog) {
                    LoginDialog(
                        viewModel = loginViewModel,
                        onDismiss = { showLoginDialog = false }
                    )
                }
            }
        }
    }
}