package com.github.adriianh.melo.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.util.MeloVersion
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UpdateSettingsSection(
    autoCheckUpdates: Boolean,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showChangelog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MeloColors.surface2.copy(alpha = 0.6f))
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Actualizaciones",
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textPrimary
                    )
                    Text(
                        text = "Versión instalada: v${MeloVersion.CURRENT}",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary
                    )
                }
            }

            if (state !is UpdateState.Checking && state !is UpdateState.Downloading) {
                OutlinedButton(
                    onClick = { viewModel.checkForUpdates() },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MeloColors.border),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MeloColors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Buscar", style = MeloType.labelMedium, color = MeloColors.textPrimary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Buscar automáticamente",
                    style = MeloType.body,
                    color = MeloColors.textPrimary
                )
                Text(
                    text = "Comprueba si hay nuevas versiones al abrir la app",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary
                )
            }
            Switch(
                checked = autoCheckUpdates,
                onCheckedChange = onToggleAutoCheckUpdates,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }

        when (val s = state) {
            is UpdateState.Idle -> {
                // Keep minimal
            }

            is UpdateState.Checking -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MeloColors.surface1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Comprobando nuevas versiones...",
                            style = MeloType.labelMedium,
                            color = MeloColors.textSecondary
                        )
                    }
                }
            }

            is UpdateState.UpToDate -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MeloColors.surface1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "¡Melo está actualizado! (v${s.currentVersion})",
                            style = MeloType.labelMedium,
                            color = MeloColors.textPrimary
                        )
                    }
                }
            }

            is UpdateState.UpdateAvailable -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "¡Nueva versión disponible: v${s.release.version}!",
                                    style = MeloType.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = s.targetAsset?.name ?: "Instalador de actualización",
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textSecondary
                                )
                            }
                        }

                        if (s.release.releaseNotes.isNotBlank()) {
                            OutlinedButton(
                                onClick = { showChangelog = !showChangelog },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (showChangelog) "Ocultar novedades" else "Ver novedades",
                                    style = MeloType.labelSmall
                                )
                            }

                            AnimatedVisibility(visible = showChangelog) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MeloColors.surface1,
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                ) {
                                    Text(
                                        text = s.release.releaseNotes,
                                        style = MeloType.labelSmall,
                                        color = MeloColors.textSecondary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        val targetAsset = s.targetAsset
                        if (targetAsset != null) {
                            Button(
                                onClick = { viewModel.startDownload(s.release, targetAsset) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Descargar e instalar actualización",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            is UpdateState.Downloading -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MeloColors.surface1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Descargando actualización (v${s.release.version})...",
                                style = MeloType.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MeloColors.textPrimary
                            )
                            Text(
                                text = "${(s.progressFraction * 100).toInt()}%",
                                style = MeloType.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { s.progressFraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MeloColors.surface2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatMegabytes(s.bytesDownloaded)} / ${formatMegabytes(s.totalBytes)}",
                                style = MeloType.labelSmall,
                                color = MeloColors.textSecondary
                            )
                            OutlinedButton(
                                onClick = { viewModel.cancelDownload() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Cancelar",
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textMuted
                                )
                            }
                        }
                    }
                }
            }

            is UpdateState.ReadyToInstall -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "¡Descarga completa!",
                            style = MeloType.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "La nueva versión de Melo está lista para instalarse.",
                            style = MeloType.body,
                            color = MeloColors.textSecondary
                        )
                        Button(
                            onClick = { viewModel.installUpdate(s.installerPath) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reiniciar y actualizar ahora", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is UpdateState.Error -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF453A).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFFFF453A).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error de actualización",
                                style = MeloType.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF453A)
                            )
                            Text(
                                text = s.message,
                                style = MeloType.labelSmall,
                                color = MeloColors.textSecondary
                            )
                        }
                        if (s.canRetry) {
                            OutlinedButton(
                                onClick = { viewModel.checkForUpdates() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reintentar", style = MeloType.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMegabytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return "${(mb * 10).toInt() / 10.0} MB"
}