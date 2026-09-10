package com.github.adriianh.melo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.melo.theme.MeloTheme
import com.github.adriianh.melo.theme.resolveDarkTheme
import com.github.adriianh.melo.theme.toAccentColor
import com.github.adriianh.melo.ui.AdaptiveScaffold
import com.github.adriianh.melo.ui.LocalSelectionMode
import com.github.adriianh.melo.ui.components.AmbientCanvas
import com.github.adriianh.melo.ui.components.LocalMeloSnackbar
import com.github.adriianh.melo.ui.components.MeloSnackbarHost
import com.github.adriianh.melo.ui.components.MeloSnackbarState
import com.github.adriianh.melo.ui.components.MeloSplashScreen
import com.github.adriianh.melo.ui.components.OfflineModeBanner
import com.github.adriianh.melo.ui.detail.AlbumDetailScreen
import com.github.adriianh.melo.ui.detail.ArtistDetailScreen
import com.github.adriianh.melo.ui.detail.PlaylistDetailScreen
import com.github.adriianh.melo.ui.home.HomeScreen
import com.github.adriianh.melo.ui.library.LibraryScreen
import com.github.adriianh.melo.ui.login.LoginDialog
import com.github.adriianh.melo.ui.login.LoginViewModel
import com.github.adriianh.melo.ui.navigation.MainTab
import com.github.adriianh.melo.ui.navigation.ScreenRoute
import com.github.adriianh.melo.ui.player.NowPlayingScreen
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.search.SearchScreen
import com.github.adriianh.melo.ui.settings.SettingsActions
import com.github.adriianh.melo.ui.settings.SettingsDialog
import com.github.adriianh.melo.ui.settings.SettingsSheet
import com.github.adriianh.melo.ui.settings.UpdateViewModel
import com.github.adriianh.melo.util.MeloMotion
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
expect fun InitImageLoader()

@Composable
fun App(
    titleBar: @Composable () -> Unit = {}
) {
    InitImageLoader()

    val playerViewModel: PlayerViewModel = koinViewModel()
    val loginViewModel: LoginViewModel = koinViewModel()
    val updateViewModel: UpdateViewModel = koinViewModel()
    val getSettingsUseCase: GetSettingsUseCase = koinInject()
    val updateSettingsUseCase: UpdateSettingsUseCase = koinInject()
    val offlineRepository: OfflineRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val accentPalette by playerViewModel.accentPalette.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val settings by getSettingsUseCase().collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()

    LaunchedEffect(settings.autoCheckUpdates) {
        if (settings.autoCheckUpdates) {
            kotlinx.coroutines.delay(4000)
            updateViewModel.checkForUpdates()
        }
    }
    val isSystemDark = isSystemInDarkTheme()
    val platform = remember { getPlatform() }
    val isDarkTheme = settings.themeMode.resolveDarkTheme(isSystemDark)
    val themeAccent = remember(settings.theme) { settings.theme.toAccentColor() }
    val savedAccent: Color? = remember(settings.lastAccentColor) {
        settings.lastAccentColor?.let { Color(it.toULong()) }
    }
    val hasTrack = playbackState.currentTrack != null
    val finalAccent = when {
        settings.dynamicColor && hasTrack -> accentPalette.dominant
        settings.dynamicColor && savedAccent != null -> savedAccent
        else -> themeAccent
    }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val snackbarState = remember { MeloSnackbarState() }

    MeloTheme(
        accentColor = finalAccent,
        isDarkTheme = isDarkTheme
    ) {
        var selectedTab by remember { mutableStateOf(MainTab.HOME) }
        var showLoginDialog by remember { mutableStateOf(false) }
        var isNowPlayingExpanded by remember { mutableStateOf(false) }
        val isSelectionMode = remember { mutableStateOf(false) }
        var navigationStack by remember { mutableStateOf(listOf<ScreenRoute>(ScreenRoute.Home)) }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(isNowPlayingExpanded) {
            if (isNowPlayingExpanded) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        }

        val currentScreen = navigationStack.lastOrNull() ?: ScreenRoute.Home

        fun navigateTo(destination: ScreenRoute) {
            navigationStack = navigationStack + destination
        }

        fun navigateBack() {
            if (navigationStack.size > 1) {
                navigationStack = navigationStack.dropLast(1)
            }
        }

        val isDetailScreen = currentScreen is ScreenRoute.Album ||
                currentScreen is ScreenRoute.Playlist ||
                currentScreen is ScreenRoute.Artist
        val isAmbientCanvasEnabled = !isDetailScreen && !isNowPlayingExpanded

        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(750.milliseconds)
            showSplash = false
        }

        if (!showSplash) {

            CompositionLocalProvider(
                LocalMeloSnackbar provides snackbarState,
                LocalSelectionMode provides isSelectionMode
            ) {
                AmbientCanvas(
                    accentColor = finalAccent,
                    modifier = Modifier.fillMaxSize(),
                    enabled = isAmbientCanvasEnabled
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            titleBar()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                AdaptiveScaffold(
                                    selectedTab = selectedTab,
                                    isSelectionMode = isSelectionMode.value,
                                    onTabSelected = { tab ->
                                        selectedTab = tab
                                        navigationStack = when (tab) {
                                            MainTab.HOME -> listOf(ScreenRoute.Home)
                                            MainTab.SEARCH -> listOf(ScreenRoute.Search)
                                            MainTab.LIBRARY -> listOf(ScreenRoute.Library)
                                        }
                                    },
                                    onOpenNowPlaying = { isNowPlayingExpanded = true },
                                    onOpenSettings = { showSettingsSheet = true },
                                    onPlaylistClick = { id, title, artwork, author ->
                                        navigateTo(ScreenRoute.Playlist(id, title, artwork, author))
                                    },
                                    onAlbumClick = { id ->
                                        navigateTo(ScreenRoute.Album(id))
                                    },
                                    onArtistClick = { id ->
                                        navigateTo(ScreenRoute.Artist(id))
                                    },
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = {
                                        coroutineScope.launch {
                                            updateSettingsUseCase { current ->
                                                val nextMode = when (current.themeMode) {
                                                    ThemeMode.SYSTEM -> if (isSystemDark) ThemeMode.LIGHT else ThemeMode.DARK
                                                    ThemeMode.LIGHT -> ThemeMode.DARK
                                                    ThemeMode.DARK -> ThemeMode.LIGHT
                                                }
                                                current.copy(themeMode = nextMode)
                                            }
                                        }
                                    }
                                ) { _, paddingValues ->
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        OfflineModeBanner(
                                            visible = settings.offlineMode,
                                            onReconnect = {
                                                coroutineScope.launch {
                                                    updateSettingsUseCase { current ->
                                                        current.copy(offlineMode = false)
                                                    }
                                                    snackbarState.show("Modo sin conexión desactivado")
                                                }
                                            }
                                        )

                                        val currentUpdate = updateState
                                        AnimatedVisibility(
                                            visible = currentUpdate is UpdateState.UpdateAvailable,
                                            enter = slideInVertically() + fadeIn(),
                                            exit = slideOutVertically() + fadeOut()
                                        ) {
                                            if (currentUpdate is UpdateState.UpdateAvailable) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = 16.dp,
                                                            vertical = 8.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Nueva versión ${currentUpdate.release.version} disponible",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        TextButton(
                                                            onClick = { showSettingsSheet = true }
                                                        ) {
                                                            Text("Actualizar")
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        ) {
                                            when (currentScreen) {
                                                ScreenRoute.Home -> HomeScreen(
                                                    onAlbumClick = { id, title, artwork, author ->
                                                        navigateTo(
                                                            ScreenRoute.Album(
                                                                id,
                                                                title,
                                                                artwork,
                                                                author
                                                            )
                                                        )
                                                    },
                                                    onPlaylistClick = { id, title, artwork, author ->
                                                        navigateTo(
                                                            ScreenRoute.Playlist(
                                                                id,
                                                                title,
                                                                artwork,
                                                                author
                                                            )
                                                        )
                                                    },
                                                    onArtistClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Artist(
                                                                id
                                                            )
                                                        )
                                                    },
                                                    onOpenSettings = { showSettingsSheet = true },
                                                    paddingValues = paddingValues
                                                )

                                                ScreenRoute.Search -> SearchScreen(
                                                    onAlbumClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Album(
                                                                id
                                                            )
                                                        )
                                                    },
                                                    onPlaylistClick = { id, title, artwork, author ->
                                                        navigateTo(
                                                            ScreenRoute.Playlist(
                                                                id,
                                                                title,
                                                                artwork,
                                                                author
                                                            )
                                                        )
                                                    },
                                                    onArtistClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Artist(
                                                                id
                                                            )
                                                        )
                                                    },
                                                    onOpenSettings = { showSettingsSheet = true },
                                                    paddingValues = paddingValues
                                                )

                                                ScreenRoute.Library -> LibraryScreen(
                                                    onOpenSettings = { showSettingsSheet = true },
                                                    onAlbumClick = { id, title, artwork, author ->
                                                        navigateTo(
                                                            ScreenRoute.Album(
                                                                id,
                                                                title,
                                                                artwork,
                                                                author
                                                            )
                                                        )
                                                    },
                                                    onPlaylistClick = { id, title, artwork, author ->
                                                        navigateTo(
                                                            ScreenRoute.Playlist(
                                                                id,
                                                                title,
                                                                artwork,
                                                                author
                                                            )
                                                        )
                                                    },
                                                    onArtistClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Artist(
                                                                id
                                                            )
                                                        )
                                                    },
                                                    paddingValues = paddingValues
                                                )

                                                is ScreenRoute.Album -> AlbumDetailScreen(
                                                    albumId = currentScreen.id,
                                                    initialTitle = currentScreen.title,
                                                    initialArtwork = currentScreen.artwork,
                                                    initialAuthor = currentScreen.author,
                                                    bottomPadding = paddingValues,
                                                    onBack = ::navigateBack,
                                                    onArtistClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Artist(
                                                                id
                                                            )
                                                        )
                                                    },
                                                    onAlbumClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Album(
                                                                id
                                                            )
                                                        )
                                                    }
                                                )

                                                is ScreenRoute.Playlist -> PlaylistDetailScreen(
                                                    playlistId = currentScreen.id,
                                                    initialTitle = currentScreen.title,
                                                    initialArtwork = currentScreen.artwork,
                                                    initialAuthor = currentScreen.author,
                                                    bottomPadding = paddingValues,
                                                    onBack = ::navigateBack,
                                                    onArtistClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Artist(
                                                                id
                                                            )
                                                        )
                                                    }
                                                )

                                                is ScreenRoute.Artist -> ArtistDetailScreen(
                                                    artistId = currentScreen.id,
                                                    initialName = currentScreen.name,
                                                    initialArtwork = currentScreen.artwork,
                                                    bottomPadding = paddingValues,
                                                    onBack = ::navigateBack,
                                                    onAlbumClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Album(
                                                                id
                                                            )
                                                        )
                                                    },
                                                    onArtistClick = { id ->
                                                        navigateTo(
                                                            ScreenRoute.Artist(
                                                                id
                                                            )
                                                        )
                                                    },
                                                    onPlaylistClick = { id, title, artwork, author ->
                                                        navigateTo(
                                                            ScreenRoute.Playlist(
                                                                id,
                                                                title,
                                                                artwork,
                                                                author
                                                            )
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
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
                                titleBar = titleBar,
                                onCollapse = { isNowPlayingExpanded = false },
                                onArtistClick = { id ->
                                    isNowPlayingExpanded = false
                                    navigateTo(ScreenRoute.Artist(id))
                                },
                                onAlbumClick = { id ->
                                    isNowPlayingExpanded = false
                                    navigateTo(ScreenRoute.Album(id))
                                },
                                onPlaylistClick = { id ->
                                    isNowPlayingExpanded = false
                                    navigateTo(ScreenRoute.Playlist(id))
                                }
                            )
                        }

                        if (showLoginDialog) {
                            LoginDialog(
                                viewModel = loginViewModel,
                                onDismiss = { showLoginDialog = false }
                            )
                        }

                        if (showSettingsSheet) {
                            val settingsActions = SettingsActions(
                                onThemeModeSelected = { themeMode ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(themeMode = themeMode)
                                        }
                                    }
                                },
                                onThemePresetSelected = { preset ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(theme = preset)
                                        }
                                    }
                                },
                                onDynamicColorToggle = { enabled ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(dynamicColor = enabled)
                                        }
                                    }
                                },
                                onDataSaverToggle = { enabled ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(dataSaver = enabled)
                                        }
                                    }
                                },
                                onAudioQualitySelected = { quality ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(audioQuality = quality)
                                        }
                                    }
                                },
                                onDownloadFormatSelected = { format ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(downloadFormat = format)
                                        }
                                    }
                                },
                                onDownloadQualitySelected = { quality ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(downloadQuality = quality)
                                        }
                                    }
                                },
                                onCacheSizeLimitSelected = { limit ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(maxOfflineSizeMb = limit.sizeMb)
                                        }
                                    }
                                },
                                onOfflineModeToggle = { enabled ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(offlineMode = enabled)
                                        }
                                    }
                                },
                                onClearCache = {
                                    coroutineScope.launch {
                                        offlineRepository.cleanupCache(0)
                                        snackbarState.show("Caché de audio liberada correctamente")
                                    }
                                },
                                onAutoplayToggle = { autoplay ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(autoplay = autoplay)
                                        }
                                    }
                                },
                                onDiscordRpcToggle = { rpc ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(discordRpcEnabled = rpc)
                                        }
                                    }
                                },
                                onAddLocalPath = { path ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            if (path !in current.localLibraryPaths) {
                                                current.copy(localLibraryPaths = current.localLibraryPaths + path)
                                            } else current
                                        }
                                    }
                                },
                                onRemoveLocalPath = { path ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(localLibraryPaths = current.localLibraryPaths.filter { it != path })
                                        }
                                    }
                                },
                                onSyncHistoryToYouTubeChanged = { enabled ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(syncHistoryToYouTube = enabled)
                                        }
                                    }
                                },
                                onToggleAutoCheckUpdates = { enabled ->
                                    coroutineScope.launch {
                                        updateSettingsUseCase { current ->
                                            current.copy(autoCheckUpdates = enabled)
                                        }
                                    }
                                },
                                onOpenLogin = {
                                    showSettingsSheet = false
                                    showLoginDialog = true
                                },
                                onLogout = {
                                    showSettingsSheet = false
                                    loginViewModel.logout()
                                }
                            )

                            if (platform.type == PlatformType.DESKTOP) {
                                SettingsDialog(
                                    settings = settings,
                                    isLoggedIn = !settings.sessionCookies.isNullOrBlank(),
                                    actions = settingsActions,
                                    onDismiss = { showSettingsSheet = false }
                                )
                            } else {
                                SettingsSheet(
                                    settings = settings,
                                    isLoggedIn = !settings.sessionCookies.isNullOrBlank(),
                                    actions = settingsActions,
                                    onDismiss = { showSettingsSheet = false }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            MeloSnackbarHost(state = snackbarState)
                        }

                    }
                }
            }
        } else {
            MeloSplashScreen(accentColor = finalAccent, modifier = Modifier.fillMaxSize())
        }
    }
}