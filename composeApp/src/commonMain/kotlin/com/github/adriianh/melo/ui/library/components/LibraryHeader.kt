package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.melo.ui.library.LibraryTab
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.desktopScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHeader(
    profile: AccountProfile?,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = profile?.name?.take(1)?.uppercase() ?: "Y",
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
                        color = MeloColors.textPrimary
                    )
                    if (profile?.channelHandle != null) {
                        Text(
                            text = profile.channelHandle.orEmpty(),
                            style = MeloType.labelSmall,
                            color = MeloColors.textSecondary
                        )
                    }
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
            }
        }

        val libraryChipState = rememberLazyListState()
        LazyRow(
            state = libraryChipState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().desktopScroll(libraryChipState)
        ) {
            items(LibraryTab.entries.toTypedArray()) { tab ->
                val selected = selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    label = { Text(tab.label, style = MeloType.labelMedium) },
                    leadingIcon = {
                        val icon = when (tab) {
                            LibraryTab.PLAYLISTS -> Icons.AutoMirrored.Filled.PlaylistPlay
                            LibraryTab.LIKED -> Icons.Default.Favorite
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
