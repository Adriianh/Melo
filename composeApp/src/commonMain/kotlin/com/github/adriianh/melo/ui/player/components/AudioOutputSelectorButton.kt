@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.player.AudioOutputDevice
import com.github.adriianh.core.domain.player.AudioOutputKind
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_audio_output
import com.github.adriianh.melo.resources.a11y_refresh_audio_outputs
import com.github.adriianh.melo.resources.player_audio_output_active
import com.github.adriianh.melo.resources.player_audio_output_default
import com.github.adriianh.melo.resources.player_audio_output_default_hint
import com.github.adriianh.melo.resources.player_audio_output_empty
import com.github.adriianh.melo.resources.player_audio_output_kind_bluetooth
import com.github.adriianh.melo.resources.player_audio_output_kind_hdmi
import com.github.adriianh.melo.resources.player_audio_output_kind_headphones
import com.github.adriianh.melo.resources.player_audio_output_kind_other
import com.github.adriianh.melo.resources.player_audio_output_kind_speaker
import com.github.adriianh.melo.resources.player_audio_output_kind_usb
import com.github.adriianh.melo.resources.player_audio_output_title
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AudioOutputSelectorButton(
    devices: List<AudioOutputDevice>,
    selectedDeviceId: String?,
    activeAccent: Color,
    onRefresh: () -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onMenuOpenChanged: (Boolean) -> Unit = {},
    tint: Color = Color.White.copy(alpha = 0.78f),
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 22.dp,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDevice = remember(devices, selectedDeviceId) { devices.firstOrNull { it.id == selectedDeviceId } }
    val icon = selectedDevice?.kind?.let(::audioOutputIcon) ?: Icons.Default.SpeakerGroup

    DisposableEffect(expanded) {
        onMenuOpenChanged(expanded)
        onDispose {
            if (expanded) onMenuOpenChanged(false)
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                onRefresh()
                expanded = true
            },
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(Res.string.a11y_audio_output),
                tint = if (selectedDeviceId != null) activeAccent else tint,
                modifier = Modifier.size(iconSize),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MeloColors.surface1,
            border = BorderStroke(1.dp, MeloColors.borderStrong),
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
        ) {
            AudioOutputMenuHeader(onRefresh = onRefresh)

            HorizontalDivider(
                color = MeloColors.border,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AudioOutputDeviceRow(
                    label = stringResource(Res.string.player_audio_output_default),
                    subtitle =
                        stringResource(
                            if (selectedDeviceId == null) {
                                Res.string.player_audio_output_active
                            } else {
                                Res.string.player_audio_output_default_hint
                            },
                        ),
                    icon = Icons.Default.SpeakerGroup,
                    selected = selectedDeviceId == null,
                    activeAccent = activeAccent,
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )

                if (devices.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.player_audio_output_empty),
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                } else {
                    devices.forEach { device ->
                        val isSelected = selectedDeviceId == device.id
                        AudioOutputDeviceRow(
                            label = device.name,
                            subtitle =
                                if (isSelected) {
                                    stringResource(Res.string.player_audio_output_active)
                                } else {
                                    stringResource(device.kind.labelRes())
                                },
                            icon = audioOutputIcon(device.kind),
                            selected = isSelected,
                            activeAccent = activeAccent,
                            onClick = {
                                onSelect(device.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioOutputMenuHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.player_audio_output_title),
            style = MeloType.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MeloColors.textPrimary,
        )
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(30.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(Res.string.a11y_refresh_audio_outputs),
                tint = MeloColors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AudioOutputDeviceRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    activeAccent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (selected) activeAccent.copy(alpha = 0.14f) else Color.Transparent
    val borderColor = if (selected) activeAccent.copy(alpha = 0.35f) else Color.Transparent

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bgColor)
                .border(1.dp, borderColor, shape)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = if (selected) activeAccent.copy(alpha = 0.22f) else MeloColors.surface2,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) activeAccent else MeloColors.textSecondary,
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MeloType.body,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MeloColors.textPrimary else MeloColors.textPrimary.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MeloType.labelSmall,
                color = if (selected) activeAccent else MeloColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (selected) {
            Box(
                modifier =
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(activeAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

internal fun AudioOutputKind.labelRes(): StringResource =
    when (this) {
        AudioOutputKind.SPEAKER -> Res.string.player_audio_output_kind_speaker
        AudioOutputKind.HEADPHONES -> Res.string.player_audio_output_kind_headphones
        AudioOutputKind.BLUETOOTH -> Res.string.player_audio_output_kind_bluetooth
        AudioOutputKind.USB -> Res.string.player_audio_output_kind_usb
        AudioOutputKind.HDMI -> Res.string.player_audio_output_kind_hdmi
        AudioOutputKind.OTHER -> Res.string.player_audio_output_kind_other
    }

internal fun audioOutputIcon(kind: AudioOutputKind): ImageVector =
    when (kind) {
        AudioOutputKind.SPEAKER -> Icons.Default.Speaker
        AudioOutputKind.HEADPHONES -> Icons.Default.Headphones
        AudioOutputKind.BLUETOOTH -> Icons.Default.BluetoothAudio
        AudioOutputKind.USB -> Icons.Default.Usb
        AudioOutputKind.HDMI -> Icons.Default.Tv
        AudioOutputKind.OTHER -> Icons.Default.SpeakerGroup
    }
