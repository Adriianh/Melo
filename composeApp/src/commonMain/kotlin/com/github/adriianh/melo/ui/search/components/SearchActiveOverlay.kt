package com.github.adriianh.melo.ui.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun SearchOverlayWrapper(
    visible: Boolean,
    query: String,
    suggestions: List<String>,
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    onDeleteRecentSearch: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            SearchActiveOverlay(
                query = query,
                suggestions = suggestions,
                recentSearches = recentSearches,
                onSelect = onSelect,
                onDeleteRecentSearch = onDeleteRecentSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            )
        }
    }
}

@Composable
fun SearchActiveOverlay(
    query: String,
    suggestions: List<String>,
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    onDeleteRecentSearch: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MeloColors.surface1.copy(alpha = 0.98f))
            .border(0.5.dp, MeloColors.glassBorder, RoundedCornerShape(20.dp))
    ) {
        val showHistory = query.isBlank()
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            if (showHistory) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Text(
                            "Búsquedas recientes",
                            style = MeloType.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textMuted,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                        )
                    }
                    items(recentSearches, key = { it }) { search ->
                        SearchItemRow(
                            text = search,
                            icon = Icons.Default.History,
                            onClick = { onSelect(search) },
                            onDelete = onDeleteRecentSearch?.let { del -> { del(search) } }
                        )
                    }
                }
            } else {
                items(suggestions, key = { it }) { suggestion ->
                    SearchItemRow(
                        text = suggestion,
                        icon = Icons.Default.Search,
                        onClick = { onSelect(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchItemRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = MeloColors.textMuted, modifier = Modifier.size(20.dp))
        Text(
            text = text,
            style = MeloType.body,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar de búsquedas",
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
