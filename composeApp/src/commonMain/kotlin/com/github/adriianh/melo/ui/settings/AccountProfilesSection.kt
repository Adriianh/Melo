package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MeloColors.surface2.copy(alpha = 0.35f)
                    },
                ).clickable(enabled = enabled && !isSelected, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeloAsyncImage(
            url = profile.avatarUrl,
            contentDescription = profile.name,
            size = 32.dp,
            shape = CircleShape,
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

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Canal activo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
