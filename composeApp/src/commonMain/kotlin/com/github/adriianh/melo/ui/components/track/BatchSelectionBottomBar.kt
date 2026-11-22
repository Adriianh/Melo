package com.github.adriianh.melo.ui.components.track

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_clear_selection
import com.github.adriianh.melo.resources.a11y_delete_selected
import com.github.adriianh.melo.resources.a11y_deselect_all
import com.github.adriianh.melo.resources.a11y_download_selected
import com.github.adriianh.melo.resources.a11y_select_all
import com.github.adriianh.melo.resources.common_action_add_to_playlist
import com.github.adriianh.melo.resources.common_action_add_to_queue
import com.github.adriianh.melo.resources.selection_count
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BatchSelectionBottomBar(
    isVisible: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteSelected: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val isAllSelected = selectedCount > 0 && selectedCount == totalCount

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MeloColors.surface1,
                border = BorderStroke(1.dp, MeloColors.borderStrong),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(Res.string.a11y_clear_selection),
                                tint = MeloColors.textMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Column {
                            Text(
                                text = pluralStringResource(Res.plurals.selection_count, selectedCount, selectedCount),
                                style = MeloType.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                            )
                        }

                        IconButton(
                            onClick = onSelectAllToggle,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription =
                                    if (isAllSelected) {
                                        stringResource(Res.string.a11y_deselect_all)
                                    } else {
                                        stringResource(Res.string.a11y_select_all)
                                    },
                                tint = if (isAllSelected) accentColor else MeloColors.textMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BatchActionButton(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = stringResource(Res.string.common_action_add_to_playlist),
                            onClick = onAddToPlaylist,
                            tint = accentColor,
                        )

                        BatchActionButton(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = stringResource(Res.string.common_action_add_to_queue),
                            onClick = onAddToQueue,
                            tint = MeloColors.brandAccent,
                        )

                        BatchActionButton(
                            icon = Icons.Default.Download,
                            contentDescription = stringResource(Res.string.a11y_download_selected),
                            onClick = onDownload,
                            tint = MeloColors.textPrimary,
                        )

                        if (onDeleteSelected != null) {
                            BatchActionButton(
                                icon = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(Res.string.a11y_delete_selected),
                                onClick = onDeleteSelected,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MeloColors.surface2)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
