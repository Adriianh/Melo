@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "LongParameterList",
    "TooManyFunctions",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

private const val DISABLED_ALPHA = 0.4f
private val ACTION_ROW_INLINE_BREAKPOINT = 480.dp

internal enum class StatusTone {
    NEUTRAL,
    PRIMARY,
    SUCCESS,
    ERROR,
}

internal enum class SettingActionEmphasis {
    PRIMARY,
    DESTRUCTIVE,
    SECONDARY,
}

internal interface SettingActionRowScope : RowScope {
    val isStacked: Boolean

    fun Modifier.actionWidth(minInlineWidth: Dp = 160.dp): Modifier
}

private class SettingActionRowScopeImpl(
    private val rowScope: RowScope,
    override val isStacked: Boolean,
) : SettingActionRowScope,
    RowScope by rowScope {
    override fun Modifier.actionWidth(minInlineWidth: Dp): Modifier =
        if (isStacked) {
            with(rowScope) { this@actionWidth.weight(1f) }
        } else {
            this.widthIn(min = minInlineWidth)
        }
}

@Composable
internal fun SettingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: SettingActionEmphasis = SettingActionEmphasis.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val buttonModifier = modifier.heightIn(min = 48.dp)
    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

    when (emphasis) {
        SettingActionEmphasis.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier = buttonModifier,
            ) {
                SettingActionButtonContent(text = text, icon = icon, fontWeight = FontWeight.SemiBold)
            }
        }

        SettingActionEmphasis.DESTRUCTIVE -> {
            val errorColor = MaterialTheme.colorScheme.error
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = errorColor.copy(alpha = 0.12f),
                        contentColor = errorColor,
                    ),
                border = BorderStroke(1.dp, errorColor.copy(alpha = 0.55f)),
                modifier = buttonModifier,
            ) {
                SettingActionButtonContent(
                    text = text,
                    icon = icon ?: Icons.Default.DeleteOutline,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        SettingActionEmphasis.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MeloColors.surface2.copy(alpha = 0.5f),
                        contentColor = MeloColors.textPrimary,
                    ),
                border = BorderStroke(1.dp, MeloColors.borderStrong),
                modifier = buttonModifier,
            ) {
                SettingActionButtonContent(text = text, icon = icon, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SettingActionButtonContent(
    text: String,
    icon: ImageVector?,
    fontWeight: FontWeight,
) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
        text = text,
        style = MeloType.labelMedium,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MeloColors.surface2.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MeloColors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MeloType.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            content()
        }
    }
}

@Composable
internal fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledNote: String? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingRowText(
            title = title,
            subtitle = subtitle,
            disabledNote = if (!enabled) disabledNote else null,
            modifier = Modifier.weight(1f),
        )
        MeloSettingsSwitch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
internal fun SettingChoiceRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledNote: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SettingRowText(
                title = title,
                subtitle = subtitle,
                disabledNote = if (!enabled) disabledNote else null,
                modifier = Modifier.weight(1f),
            )
        }
        content()
    }
}

@Composable
internal fun SettingActionRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledNote: String? = null,
    actions: @Composable SettingActionRowScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        val isStacked = maxWidth < ACTION_ROW_INLINE_BREAKPOINT
        if (!isStacked) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingRowText(
                    title = title,
                    subtitle = subtitle,
                    disabledNote = if (!enabled) disabledNote else null,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingActionRowScopeImpl(this, isStacked = false).actions()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SettingRowText(
                        title = title,
                        subtitle = subtitle,
                        disabledNote = if (!enabled) disabledNote else null,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingActionRowScopeImpl(this, isStacked = true).actions()
                }
            }
        }
    }
}

@Composable
private fun SettingRowText(
    title: String,
    subtitle: String,
    disabledNote: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MeloType.body,
            fontWeight = FontWeight.Medium,
            color = MeloColors.textPrimary,
        )
        Text(
            text = subtitle,
            style = MeloType.labelSmall,
            color = MeloColors.textSecondary,
        )
        if (!disabledNote.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = disabledNote,
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MeloColors.textSecondary,
                )
            }
        }
    }
}

@Composable
internal fun <T> SettingOptionGrid(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    title: (T) -> String,
    modifier: Modifier = Modifier,
    hint: (T) -> String? = { null },
    minCellWidth: Dp = 104.dp,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return
    val gap = 8.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().selectableGroup(),
    ) {
        val maxCols = ((maxWidth + gap) / (minCellWidth + gap)).toInt().coerceIn(1, options.size)
        val cols =
            if (maxCols <= 1) {
                1
            } else {
                (maxCols downTo 2).firstOrNull { options.size % it == 0 } ?: maxCols
            }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            options.chunked(cols).forEach { rowOptions ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    rowOptions.forEach { option ->
                        val selected = isSelected(option)
                        SettingOptionCell(
                            title = title(option),
                            hint = hint(option),
                            selected = selected,
                            enabled = enabled,
                            onClick = { onSelect(option) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    repeat(cols - rowOptions.size) {
                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingOptionCell(
    title: String,
    hint: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    val cellModifier =
        modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)

    Surface(
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.2f) else MeloColors.surface2,
        border = BorderStroke(1.dp, if (selected) accent else MeloColors.border),
        modifier = cellModifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().heightIn(min = 56.dp),
            contentAlignment = Alignment.Center,
        ) {
            SettingOptionCellText(
                title = title,
                hint = hint,
                selected = selected,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingOptionCellText(
    title: String,
    hint: String?,
    selected: Boolean,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = title,
            style = MeloType.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) accent else MeloColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!hint.isNullOrBlank()) {
            Text(
                text = hint,
                style = MeloType.labelSmall,
                color = if (selected) accent.copy(alpha = 0.85f) else MeloColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class StatusBannerPalette(
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
    val iconTint: Color,
)

@Composable
private fun StatusTone.resolvePalette(): StatusBannerPalette {
    val primary = MaterialTheme.colorScheme.primary
    val successColor = Color(0xFF34C759)
    val errorColor = Color(0xFFFF453A)
    return when (this) {
        StatusTone.NEUTRAL ->
            StatusBannerPalette(
                containerColor = MeloColors.surface1,
                borderColor = MeloColors.border,
                titleColor = MeloColors.textPrimary,
                iconTint = primary,
            )
        StatusTone.PRIMARY ->
            StatusBannerPalette(
                containerColor = primary.copy(alpha = 0.1f),
                borderColor = primary.copy(alpha = 0.35f),
                titleColor = primary,
                iconTint = primary,
            )
        StatusTone.SUCCESS ->
            StatusBannerPalette(
                containerColor = MeloColors.surface1,
                borderColor = successColor.copy(alpha = 0.35f),
                titleColor = MeloColors.textPrimary,
                iconTint = successColor,
            )
        StatusTone.ERROR ->
            StatusBannerPalette(
                containerColor = errorColor.copy(alpha = 0.1f),
                borderColor = errorColor.copy(alpha = 0.35f),
                titleColor = errorColor,
                iconTint = errorColor,
            )
    }
}

@Composable
internal fun SettingsStatusBanner(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    body: String? = null,
    tone: StatusTone = StatusTone.NEUTRAL,
    titleBadge: (@Composable () -> Unit)? = null,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val palette = tone.resolvePalette()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.containerColor,
        border = BorderStroke(1.dp, palette.borderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsStatusBannerHeader(
                title = title,
                body = body,
                icon = icon,
                isLoading = isLoading,
                palette = palette,
                titleBadge = titleBadge,
            )
            extraContent?.invoke(this)
            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
private fun SettingsStatusBannerHeader(
    title: String,
    body: String?,
    icon: ImageVector?,
    isLoading: Boolean,
    palette: StatusBannerPalette,
    titleBadge: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = palette.iconTint,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MeloType.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.titleColor,
                    modifier = Modifier.weight(1f, fill = false),
                )
                titleBadge?.invoke()
            }
            if (!body.isNullOrBlank()) {
                Text(
                    text = body,
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                )
            }
        }
    }
}

@Composable
internal fun MeloSettingsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            ),
    )
}

@Composable
internal fun SettingsConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Default.DeleteOutline,
    dismissText: String = "Cancelar",
) {
    val errorColor = MaterialTheme.colorScheme.error
    val dialogShape = RoundedCornerShape(24.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        containerColor = MeloColors.surface1,
        titleContentColor = MeloColors.textPrimary,
        textContentColor = MeloColors.textSecondary,
        modifier =
            Modifier.border(
                BorderStroke(1.dp, MeloColors.borderStrong),
                dialogShape,
            ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = errorColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, errorColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = errorColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                )
            }
        },
        text = {
            Text(
                text = message,
                style = MeloType.body,
                color = MeloColors.textSecondary,
            )
        },
        confirmButton = {
            SettingActionButton(
                text = confirmText,
                onClick = onConfirm,
                emphasis = SettingActionEmphasis.DESTRUCTIVE,
                icon = icon,
            )
        },
        dismissButton = {
            SettingActionButton(
                text = dismissText,
                onClick = onDismiss,
                emphasis = SettingActionEmphasis.SECONDARY,
            )
        },
    )
}
