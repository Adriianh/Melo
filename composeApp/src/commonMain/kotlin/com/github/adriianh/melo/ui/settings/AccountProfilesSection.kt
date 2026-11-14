package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
        SettingActionRow(
            title = "Canales de la cuenta",
            subtitle = "Cambia el canal activo para sincronizar su biblioteca, likes y playlists.",
        ) {
            if (isLoading || isSwitching) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                SettingActionButton(
                    text = "Actualizar",
                    onClick = onRefresh,
                    emphasis = SettingActionEmphasis.SECONDARY,
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.actionWidth(),
                )
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
            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
    val rowShape = RoundedCornerShape(12.dp)
    val accent = MaterialTheme.colorScheme.primary
    val fill = if (isSelected) accent.copy(alpha = 0.2f) else MeloColors.surface2.copy(alpha = 0.35f)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(rowShape)
                .background(fill)
                .border(width = 1.dp, color = if (isSelected) accent else MeloColors.border, shape = rowShape)
                .selectable(
                    selected = isSelected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { if (!isSelected) onClick() },
                ).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AccountProfileAvatar(profile = profile, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                style = MeloType.body,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) accent else MeloColors.textPrimary,
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
