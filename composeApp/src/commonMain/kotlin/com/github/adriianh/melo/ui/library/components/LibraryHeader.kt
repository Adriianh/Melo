package com.github.adriianh.melo.ui.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.melo.ui.library.LibraryCategory
import com.github.adriianh.melo.ui.library.LibrarySortOrder
import com.github.adriianh.melo.ui.library.LibraryTab
import com.github.adriianh.melo.ui.library.LibraryViewMode
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.desktopScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHeader(
    profile: AccountProfile?,
    selectedTab: LibraryTab,
    summaryText: String,
    searchQuery: String,
    isSearchActive: Boolean,
    sortOrder: LibrarySortOrder,
    viewMode: LibraryViewMode,
    showViewModeToggle: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategory = LibraryCategory.fromTab(selectedTab)
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MeloColors.surface2,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onOpenSettings)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = profile?.name?.take(1)?.uppercase() ?: "M",
                            style = MeloType.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary
                        )
                    }
                }
                Column {
                    Text(
                        text = profile?.name ?: "Mi Biblioteca",
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summaryText.ifBlank { profile?.channelHandle.orEmpty() },
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = { onToggleSearch(!isSearchActive) }) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = if (isSearchActive) "Cerrar búsqueda" else "Buscar en biblioteca",
                        tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MeloColors.textSecondary
                    )
                }

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Ordenar",
                            tint = if (sortOrder != LibrarySortOrder.RECENTLY_ADDED) MaterialTheme.colorScheme.primary else MeloColors.textSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(MeloColors.surface2)
                    ) {
                        LibrarySortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        order.label,
                                        style = MeloType.body,
                                        color = if (sortOrder == order) MaterialTheme.colorScheme.primary else MeloColors.textPrimary,
                                        fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortOrderChange(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                if (showViewModeToggle) {
                    IconButton(
                        onClick = {
                            val nextMode =
                                if (viewMode == LibraryViewMode.GRID) LibraryViewMode.COMPACT_LIST else LibraryViewMode.GRID
                            onViewModeChange(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == LibraryViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Cambiar vista",
                            tint = MeloColors.textSecondary
                        )
                    }
                }

                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = MeloColors.textSecondary
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Buscar en ${selectedTab.label}...",
                        style = MeloType.body,
                        color = MeloColors.textSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Borrar texto",
                                tint = MeloColors.textSecondary
                            )
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MeloColors.border,
                    focusedContainerColor = MeloColors.surface1,
                    unfocusedContainerColor = MeloColors.surface1,
                    focusedTextColor = MeloColors.textPrimary,
                    unfocusedTextColor = MeloColors.textPrimary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MeloColors.surface1)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LibraryCategory.entries.forEach { category ->
                val isSelected = activeCategory == category
                val background = if (isSelected) MeloColors.surface2 else Color.Transparent
                val textColor =
                    if (isSelected) MaterialTheme.colorScheme.primary else MeloColors.textSecondary
                val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isSelected) {
                                onTabSelected(category.tabs.first())
                            }
                        },
                    color = background,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categoryIcon = when (category) {
                            LibraryCategory.COLLECTION -> Icons.Default.LibraryMusic
                            LibraryCategory.DEVICE -> Icons.Default.Devices
                            LibraryCategory.HISTORY -> Icons.Default.History
                        }
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = category.label,
                            style = MeloType.labelMedium,
                            fontWeight = fontWeight,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (activeCategory.tabs.size > 1) {
            val chipState = rememberLazyListState()
            LazyRow(
                state = chipState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().desktopScroll(chipState)
            ) {
                items(activeCategory.tabs) { tab ->
                    val selected = selectedTab == tab
                    FilterChip(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        label = { Text(tab.label, style = MeloType.labelMedium) },
                        leadingIcon = {
                            val icon = when (tab) {
                                LibraryTab.PLAYLISTS -> Icons.AutoMirrored.Filled.PlaylistPlay
                                LibraryTab.LIKED -> Icons.Default.Favorite
                                LibraryTab.DOWNLOADS -> Icons.Default.DownloadDone
                                LibraryTab.LOCAL -> Icons.Default.Folder
                                LibraryTab.ARTISTS -> Icons.Default.Person
                                LibraryTab.ALBUMS -> Icons.Default.Album
                                LibraryTab.HISTORY -> Icons.Default.History
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MeloColors.surface1,
                            labelColor = MeloColors.textPrimary,
                            iconColor = MeloColors.textSecondary,
                            selectedContainerColor = MeloColors.surface2,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MeloColors.borderStrong,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}