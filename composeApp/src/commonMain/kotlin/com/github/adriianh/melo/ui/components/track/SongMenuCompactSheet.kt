@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.i18n.resolve
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

private val COMPACT_ROW_HEIGHT = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongMenuCompactSheet(
    track: Track,
    entries: List<SongMenuEntry>,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MeloColors.menuSheetSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MeloColors.textMuted) },
    ) {
        SongMenuCompactSheetContent(
            track = track,
            entries = entries,
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
internal fun SongMenuCompactSheetContent(
    track: Track,
    entries: List<SongMenuEntry>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
    ) {
        SongMenuHeader(track = track)

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            color = MeloColors.menuDivider,
        )

        entries.forEach { entry ->
            when (entry) {
                SongMenuEntry.Divider ->
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                        color = MeloColors.menuDivider,
                    )

                is SongMenuEntry.Action -> {
                    val textColor =
                        if (entry.style == SongMenuEntryStyle.Destructive) {
                            MeloColors.menuDestructive
                        } else {
                            MeloColors.textPrimary
                        }
                    SongMenuCompactRow(
                        icon = entry.icon,
                        label = entry.label.resolve(),
                        iconTint = entry.tint ?: textColor,
                        textColor = textColor,
                        enabled = entry.enabled,
                        onClick = {
                            entry.onClick()
                            onDismissRequest()
                        },
                    )
                }

                is SongMenuEntry.Submenu ->
                    SongMenuCompactRow(
                        icon = entry.icon,
                        label = entry.label.resolve(),
                        iconTint = MeloColors.textPrimary,
                        textColor = MeloColors.textPrimary,
                        enabled = true,
                        onClick = {
                            entry.content.onOpenSheetFallback()
                            onDismissRequest()
                        },
                    )
            }
        }
    }
}

@Composable
private fun SongMenuHeader(track: Track) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MeloAsyncImage(
            url = track.artworkUrl,
            contentDescription = track.title,
            size = 56.dp,
            shape = RoundedCornerShape(8.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MeloType.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SongMenuCompactRow(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = COMPACT_ROW_HEIGHT)
                .alpha(if (enabled) 1f else 0.5f)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MeloType.body,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
