package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
internal fun AccountProfilesList(
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoading: Boolean,
    isSwitching: Boolean,
    error: String?,
    onSelect: (String?) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Canales de la cuenta",
                    style = MeloType.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MeloColors.textPrimary,
                )
                Text(
                    text = "Cambia el canal activo para sincronizar su biblioteca, likes y playlists.",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                )
            }

            if (isLoading || isSwitching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                TextButton(
                    onClick = onRefresh,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar", style = MeloType.labelSmall)
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = MeloType.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (profiles.size > 1) {
            profiles.forEach { profile ->
                AccountProfileRow(
                    profile = profile,
                    isSelected = profile.pageId == activePageId,
                    enabled = !isSwitching,
                    onClick = { onSelect(profile.pageId) },
                )
            }
        }
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun AccountProfileRow(
    profile: AccountProfile,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val subtitle =
        profile.channelHandle?.takeIf { it.isNotBlank() }
            ?: profile.email?.takeIf { it.isNotBlank() }
            ?: profile.byline?.takeIf { it.isNotBlank() }

    val rowShape = RoundedCornerShape(10.dp)
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(rowShape)
                .background(
                    if (isSelected) {
                        accent.copy(alpha = 0.12f)
                    } else {
                        MeloColors.surface2.copy(alpha = 0.35f)
                    },
                ).border(
                    width = 1.5.dp,
                    color = if (isSelected) accent else Color.Transparent,
                    shape = rowShape,
                ).clickable(enabled = enabled && !isSelected, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AccountProfileAvatar(
            profile = profile,
            size = 32.dp,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                style = MeloType.body,
                fontWeight = FontWeight.Medium,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Selection marker sits on the opposite side of the avatar and name.
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Canal activo",
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Profile picture for an account row, falling back to a person glyph when the
 * channel has no avatar URL (or the thumbnail failed to resolve).
 */
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun AccountProfileAvatar(
    profile: AccountProfile,
    size: Dp,
) {
    val avatarUrl = profile.avatarUrl
    if (!avatarUrl.isNullOrBlank()) {
        MeloAsyncImage(
            url = avatarUrl,
            contentDescription = profile.name,
            size = size,
            shape = CircleShape,
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MeloColors.surface1),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = profile.name,
                tint = MeloColors.textSecondary,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}
