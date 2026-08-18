package com.github.adriianh.melo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.github.adriianh.melo.theme.LocalMeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffold(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOpenNowPlaying: () -> Unit = {},
    onPlaylistClick: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    content: @Composable (String, PaddingValues) -> Unit
) {
    val platform = remember { getPlatform() }

    if (platform.type == PlatformType.DESKTOP) {
        DesktopMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onOpenNowPlaying = onOpenNowPlaying,
            onPlaylistClick = onPlaylistClick,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            content = { padding -> content(selectedTab, padding) }
        )
    } else {
        MobileMainLayout(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onOpenNowPlaying = onOpenNowPlaying,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
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
                onToggleTheme = onToggleTheme
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 12.dp)
                    .graphicsLayer { clip = true; shape = RoundedCornerShape(20.dp) }
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(MeloColors.surface0.copy(alpha = 0.6f))
                        .border(0.5.dp, MeloColors.border, RoundedCornerShape(20.dp))
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
                    selectedSection = desktopPaneSection,
                    onSectionChange = { desktopPaneSection = it }
                )
            }
        }
    }
}

@Composable
private fun MainSidebar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SidebarViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val meloColors = LocalMeloColors.current
    val sidebarWidthState = animateDpAsState(
        targetValue = if (collapsed) 72.dp else 240.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "sidebarWidth"
    )

    Column(
        modifier = modifier
            .layout { measurable, constraints ->
                val width = sidebarWidthState.value.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(minWidth = width, maxWidth = width)
                )
                layout(width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .fillMaxHeight()
            .padding(top = 12.dp, bottom = 12.dp, start = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(meloColors.playerBarFill)
            .border(0.5.dp, meloColors.playerBarBorder, RoundedCornerShape(20.dp)),
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
                            color = MaterialTheme.colorScheme.primary,
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
                                label = "Biblioteca",
                                icon = Icons.Default.LibraryMusic,
                                isSelected = selectedTab == "Library",
                                onClick = { onTabSelected("Library") }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "MI COLECCIÓN",
                                style = MeloType.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
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

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "PLAYLISTS",
                                    style = MeloType.labelSmall,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MeloColors.textMuted
                                )
                                IconButton(
                                    onClick = { onTabSelected("Library") },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Nueva lista",
                                        tint = MeloColors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (state.playlists.isNotEmpty()) {
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SidebarThemeToggle(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                collapsed = collapsed
            )
        }
    }
}

@Composable
private fun SidebarThemeToggle(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    collapsed: Boolean
) {
    if (collapsed) {
        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Cambiar tema",
                tint = MeloColors.textMuted
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleTheme)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = null,
                tint = MeloColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (isDarkTheme) "Tema Claro" else "Tema Oscuro",
                style = MeloType.body,
                color = MeloColors.textSecondary
            )
        }
    }
}

@Composable
private fun HoverableProgressBar(
    progressFraction: Float,
    accentColor: Color,
    elapsedLabel: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    lineCenterY: Dp = 22.dp,
    containerHeight: Dp = 44.dp,
    showHoverControls: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val activeHover = isHovered && showHoverControls
    val barHeight by animateDpAsState(
        targetValue = if (activeHover) 6.dp else 2.5.dp,
        animationSpec = tween(durationMillis = 150),
        label = "barHeight"
    )
    val markerSize = 12.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val topY = with(density) { lineCenterY.toPx() }
        val rPx = with(density) { cornerRadius.toPx() }

        val fullPath = remember(widthPx, topY, rPx) {
            Path().apply {
                moveTo(0f, topY + rPx)
                arcTo(
                    rect = Rect(0f, topY, 2f * rPx, topY + 2f * rPx),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo((widthPx - rPx).coerceAtLeast(rPx), topY)
                arcTo(
                    rect = Rect(widthPx - 2f * rPx, topY, widthPx, topY + 2f * rPx),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            }
        }

        val pathMeasure = remember { PathMeasure() }
        pathMeasure.setPath(fullPath, false)
        val totalLength = pathMeasure.length
        val progressDistance =
            (totalLength * progressFraction.coerceIn(0f, 1f)).coerceIn(0f, totalLength)

        val activePath = remember(fullPath, progressDistance) {
            Path().apply {
                if (progressDistance > 0f) {
                    pathMeasure.getSegment(0f, progressDistance, this, true)
                }
            }
        }

        val playHeadPos = remember(pathMeasure, progressDistance) {
            pathMeasure.getPosition(progressDistance)
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(containerHeight)) {
            drawPath(
                path = fullPath,
                color = MeloColors.borderStrong,
                style = Stroke(width = barHeight.toPx(), cap = StrokeCap.Round)
            )

            if (progressDistance > 0f) {
                drawPath(
                    path = activePath,
                    color = accentColor,
                    style = Stroke(width = barHeight.toPx(), cap = StrokeCap.Round)
                )
            }

            if (activeHover) {
                drawCircle(
                    color = Color.White,
                    radius = markerSize.toPx() / 2f + 1.5.dp.toPx(),
                    center = playHeadPos
                )
                drawCircle(
                    color = accentColor,
                    radius = markerSize.toPx() / 2f,
                    center = playHeadPos
                )
            }
        }

        if (activeHover) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MeloColors.surface2,
                border = BorderStroke(1.dp, MeloColors.borderStrong),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val xPx = (playHeadPos.x - placeable.width / 2f)
                            .coerceIn(
                                8.dp.toPx(),
                                (maxWidth.toPx() - placeable.width - 8.dp.toPx()).coerceAtLeast(0f)
                            )
                        val yPx =
                            (playHeadPos.y - markerSize.toPx() / 2f - placeable.height - 4.dp.toPx())
                                .coerceAtLeast(0f)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(xPx.roundToInt(), yPx.roundToInt())
                        }
                    }
            ) {
                Text(
                    text = elapsedLabel,
                    style = MeloType.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
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
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MeloColors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MeloType.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MeloColors.textMuted,
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
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MeloColors.textSecondary,
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
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val targetAccent =
        if (state.accentColor != MeloColors.textMuted) state.accentColor else MaterialTheme.colorScheme.primary
    val activeAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "DesktopPlayerBarAccent"
    )

    if (!state.hasTrack) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
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

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
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

        HoverableProgressBar(
            progressFraction = state.progressFraction,
            accentColor = activeAccent,
            elapsedLabel = state.elapsedLabel,
            onSeek = { fraction ->
                if (state.durationMs > 0) {
                    viewModel.seekTo((fraction * state.durationMs).toLong())
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            lineCenterY = 10.5.dp,
            containerHeight = 21.dp
        )
    }
}

@Composable
private fun MobileMainLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content(PaddingValues(bottom = 160.dp))

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            MobilePlayerBar(onOpenNowPlaying = onOpenNowPlaying)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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

@Composable
private fun FloatingNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(200.dp)
            .height(58.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(29.dp)
            )
            .background(MeloColors.glassSurface, RoundedCornerShape(29.dp))
            .border(0.5.dp, MeloColors.glassBorder, RoundedCornerShape(29.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavItem(
                selected = selectedTab == "Home",
                onClick = { onTabSelected("Home") },
                icon = Icons.Default.Home,
                label = "Inicio"
            )
            FloatingNavItem(
                selected = selectedTab == "Library",
                onClick = { onTabSelected("Library") },
                icon = Icons.Default.LibraryMusic,
                label = "Biblioteca"
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MeloColors.textMuted
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MeloType.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun MobilePlayerBar(
    onOpenNowPlaying: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val targetAccent =
        if (state.accentColor != MeloColors.textMuted) state.accentColor else MaterialTheme.colorScheme.primary
    val activeAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "MobilePlayerBarAccent"
    )

    if (!state.hasTrack) return

    Box(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MeloColors.playerBarFill)
                .border(0.5.dp, MeloColors.playerBarBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenNowPlaying)
        ) {
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

        HoverableProgressBar(
            progressFraction = state.progressFraction,
            accentColor = activeAccent,
            elapsedLabel = state.elapsedLabel,
            onSeek = { fraction ->
                if (state.durationMs > 0) {
                    viewModel.seekTo((fraction * state.durationMs).toLong())
                }
            },
            cornerRadius = 12.dp,
            lineCenterY = 12.5.dp,
            containerHeight = 25.dp,
            showHoverControls = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )
    }
}