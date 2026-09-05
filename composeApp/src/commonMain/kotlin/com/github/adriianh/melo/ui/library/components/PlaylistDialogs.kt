package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

data class PlaylistCoverPreset(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

val PLAYLIST_COVER_PRESETS = listOf(
    PlaylistCoverPreset("music", "Música", Icons.Default.MusicNote),
    PlaylistCoverPreset("headphones", "Audífonos", Icons.Default.Headphones),
    PlaylistCoverPreset("favorite", "Favorito", Icons.Default.Favorite),
    PlaylistCoverPreset("fire", "Energía", Icons.Default.LocalFireDepartment),
    PlaylistCoverPreset("night", "Noche", Icons.Default.Bedtime),
    PlaylistCoverPreset("bolt", "Entreno", Icons.Default.Bolt),
    PlaylistCoverPreset("coffee", "Chill", Icons.Default.Coffee),
    PlaylistCoverPreset("trip", "Viaje", Icons.Default.DirectionsCar),
    PlaylistCoverPreset("radio", "Radio", Icons.Default.Radio),
    PlaylistCoverPreset("star", "Especial", Icons.Default.Star),
)

@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    placeholder: String,
    initialValue: String = "",
    existingPlaylistNames: List<String> = emptyList(),
    userArtists: List<String> = emptyList(),
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    var selectedIcon by remember { mutableStateOf(PLAYLIST_COVER_PRESETS.first()) }
    val isEditing = initialValue.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MeloColors.surface1,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else selectedIcon.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary
                    )
                    Text(
                        text = if (isEditing) "Cambia el nombre visible de tu lista" else "Personaliza el nombre y estilo de tu lista",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= 80) value = it },
                    placeholder = {
                        Text(
                            placeholder,
                            style = MeloType.body,
                            color = MeloColors.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = selectedIcon.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (!isEditing) {
                                IconButton(
                                    onClick = {
                                        value = PlaylistNameGenerator.generateIdea(
                                            selectedIconId = selectedIcon.id,
                                            userArtists = userArtists,
                                            existingPlaylistNames = existingPlaylistNames,
                                            currentValue = value
                                        )
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Inspiración dinámica",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (value.isNotEmpty()) {
                                IconButton(
                                    onClick = { value = "" },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar texto",
                                        tint = MeloColors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MeloColors.border,
                        focusedContainerColor = MeloColors.surface2,
                        unfocusedContainerColor = MeloColors.surface2,
                        focusedTextColor = MeloColors.textPrimary,
                        unfocusedTextColor = MeloColors.textPrimary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Estilo / Icono temático",
                            style = MeloType.labelSmall,
                            color = MeloColors.textMuted
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PLAYLIST_COVER_PRESETS.forEach { preset ->
                                val isSelected = selectedIcon.id == preset.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.2f
                                    ) else MeloColors.surface2,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedIcon = preset }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 7.dp
                                        )
                                    ) {
                                        Icon(
                                            imageVector = preset.icon,
                                            contentDescription = preset.label,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MeloColors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = preset.label,
                                            style = MeloType.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MeloColors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        onConfirm(value.trim())
                    }
                },
                enabled = value.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(confirmLabel, style = MeloType.labelMedium, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", style = MeloType.labelMedium, color = MeloColors.textSecondary)
            }
        }
    )
}

@Composable
fun PlaylistDeleteDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MeloColors.surface1,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = "Eliminar playlist",
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary
                    )
                    Text(
                        text = "Acción permanente",
                        style = MeloType.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "¿Estás seguro de que deseas eliminar \"$playlistName\"?",
                    style = MeloType.body,
                    color = MeloColors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Esta lista se borrará de tu biblioteca. Las canciones guardadas o descargadas en tu dispositivo no se verán afectadas.",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Eliminar",
                    style = MeloType.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", style = MeloType.labelMedium, color = MeloColors.textSecondary)
            }
        }
    )
}