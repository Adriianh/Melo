package com.github.adriianh.melo.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.util.LocalMeloColors
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MainSidebar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
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

        SidebarUtils(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onOpenSettings = onOpenSettings,
            collapsed = collapsed
        )
    }
}

@Composable
private fun SidebarUtils(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    collapsed: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (collapsed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UtilIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings,
                    contentDescription = "Ajustes"
                )
                UtilIconButton(
                    icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    onClick = onToggleTheme,
                    contentDescription = "Cambiar tema"
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UtilIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings,
                    contentDescription = "Ajustes"
                )
                UtilIconButton(
                    icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    onClick = onToggleTheme,
                    contentDescription = "Cambiar tema"
                )
            }
        }
    }
}

@Composable
private fun UtilIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
    size: Dp = 36.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val meloColors = LocalMeloColors.current

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHovered) meloColors.glassSurface.copy(alpha = 0.35f)
                else meloColors.glassSurface.copy(alpha = 0.15f)
            )
            .border(
                width = 0.5.dp,
                color = if (isHovered) meloColors.glassBorder.copy(alpha = 0.5f)
                else meloColors.glassBorder.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isHovered) meloColors.textPrimary else meloColors.textSecondary,
            modifier = Modifier.size(19.dp)
        )
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
            color = if (isSelected) MeloColors.textPrimary else MeloColors.textSecondary,
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