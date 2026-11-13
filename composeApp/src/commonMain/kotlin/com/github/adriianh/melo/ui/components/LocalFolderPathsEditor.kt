@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import com.github.adriianh.melo.util.rememberDirectoryPicker

@Composable
fun LocalFolderPathsEditor(
    configuredPaths: List<String>,
    onAddPath: (String) -> Unit,
    onRemovePath: (String) -> Unit,
    initialManualExpanded: Boolean = false,
    hasNativePicker: Boolean = remember { getPlatform().type != PlatformType.IOS },
) {
    var newPathInput by rememberSaveable { mutableStateOf("") }
    var isManualExpanded by rememberSaveable {
        mutableStateOf(initialManualExpanded || !hasNativePicker)
    }
    val launchDirectoryPicker = rememberDirectoryPicker(onAddPath)

    val submitManualPath = {
        val trimmed = newPathInput.trim()
        if (trimmed.isNotEmpty()) {
            onAddPath(trimmed)
            newPathInput = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (configuredPaths.isEmpty()) {
            LocalFoldersEmptyState()
        } else {
            ConfiguredFoldersList(
                configuredPaths = configuredPaths,
                onRemovePath = onRemovePath,
            )
        }

        if (hasNativePicker) {
            Button(
                onClick = launchDirectoryPicker,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Elegir carpeta",
                    style = MeloType.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            TextButton(
                onClick = { isManualExpanded = !isManualExpanded },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Escribir ruta manualmente",
                    style = MeloType.labelMedium,
                    color = MeloColors.textSecondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isManualExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        AnimatedVisibility(visible = isManualExpanded || !hasNativePicker) {
            ManualFolderPathField(
                value = newPathInput,
                onValueChange = { newPathInput = it },
                onSubmit = submitManualPath,
            )
        }
    }
}

@Composable
private fun LocalFoldersEmptyState() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MeloColors.textMuted,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "No hay carpetas configuradas.",
            style = MeloType.labelSmall,
            color = MeloColors.textMuted,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConfiguredFoldersList(
    configuredPaths: List<String>,
    onRemovePath: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        configuredPaths.forEach { path ->
            Surface(
                color = MeloColors.surface2,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MeloColors.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = path,
                        style = MeloType.labelSmall,
                        color = MeloColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onRemovePath(path) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar carpeta",
                            tint = MeloColors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualFolderPathField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val canAdd = value.isNotBlank()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Ruta de la carpeta", style = MeloType.labelSmall) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (canAdd) onSubmit() }),
        trailingIcon = {
            IconButton(
                onClick = onSubmit,
                enabled = canAdd,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir carpeta",
                    tint = if (canAdd) MaterialTheme.colorScheme.primary else MeloColors.textMuted,
                )
            }
        },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MeloColors.borderStrong,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}
