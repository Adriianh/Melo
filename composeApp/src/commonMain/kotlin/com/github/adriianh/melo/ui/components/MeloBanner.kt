package com.github.adriianh.melo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun MeloBanner(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .shadow(8.dp, shape)
                .clip(shape)
                .background(MeloColors.playerBarFill)
                .border(0.5.dp, MeloColors.playerBarBorder, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MeloBannerText(
                title = title,
                subtitle = subtitle,
                icon = icon,
                accentColor = accentColor,
            )
            if (actionLabel != null && onAction != null) {
                MeloBannerAction(
                    label = actionLabel,
                    accentColor = accentColor,
                    onClick = onAction,
                )
            }
            if (onDismiss != null) {
                MeloBannerDismissButton(onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun RowScope.MeloBannerText(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    accentColor: Color,
) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MeloType.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
            )
        }
    }
}

@Composable
private fun MeloBannerAction(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Spacer(modifier = Modifier.width(8.dp))
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
    ) {
        Text(
            text = label,
            style = MeloType.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MeloBannerDismissButton(onDismiss: () -> Unit) {
    IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Descartar",
            tint = MeloColors.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}
