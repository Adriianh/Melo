@file:Suppress("FunctionNaming", "LongMethod", "LongParameterList", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdateInstallMethod
import com.github.adriianh.core.domain.model.update.UpdatePlatform
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.util.MeloVersion
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UpdateSettingsSection(
    modifier: Modifier = Modifier,
    autoCheckUpdates: Boolean,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    updateChannel: UpdateChannel =
        if (MeloVersion.CURRENT.contains("nightly", ignoreCase = true)) {
            UpdateChannel.NIGHTLY
        } else {
            UpdateChannel.STABLE
        },
    onUpdateChannelSelected: (UpdateChannel) -> Unit = {},
    viewModel: UpdateViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    UpdateSettingsSectionContent(
        modifier = modifier,
        state = state,
        autoCheckUpdates = autoCheckUpdates,
        updateChannel = updateChannel,
        onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
        onUpdateChannelSelected = { channel ->
            onUpdateChannelSelected(channel)
            viewModel.checkForUpdates(channel)
        },
        onCheckForUpdates = { viewModel.checkForUpdates(it) },
        onStartDownload = { release, asset -> viewModel.startDownload(release, asset) },
        onCancelDownload = { viewModel.cancelDownload() },
        onInstallUpdate = { path -> viewModel.installUpdate(path) },
        currentVersion = MeloVersion.CURRENT,
    )
}

@Composable
fun UpdateSettingsSectionContent(
    modifier: Modifier = Modifier,
    state: UpdateState,
    autoCheckUpdates: Boolean,
    updateChannel: UpdateChannel,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onUpdateChannelSelected: (UpdateChannel) -> Unit,
    onCheckForUpdates: (UpdateChannel) -> Unit,
    onStartDownload: (AppRelease, ReleaseAsset) -> Unit,
    onCancelDownload: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    currentVersion: String = MeloVersion.CURRENT,
) {
    Surface(
        color = MeloColors.surface2.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MeloColors.border),
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            UpdateVersionHeaderRow(
                currentVersion = currentVersion,
                canCheck = state !is UpdateState.Checking && state !is UpdateState.Downloading,
                onCheckForUpdates = { onCheckForUpdates(updateChannel) },
            )

            SettingToggleRow(
                title = "Buscar automáticamente",
                subtitle = "Comprueba si hay nuevas versiones al abrir la app.",
                checked = autoCheckUpdates,
                onCheckedChange = onToggleAutoCheckUpdates,
            )

            SettingChoiceRow(
                title = "Canal de actualización",
                subtitle =
                    if (updateChannel == UpdateChannel.NIGHTLY) {
                        "Novedades continuas y correcciones automáticas de desarrollo."
                    } else {
                        "Versiones oficiales recomendadas y probadas."
                    },
            ) {
                SegmentedControl(
                    options = UpdateChannel.entries,
                    selected = updateChannel,
                    onSelect = { channel ->
                        if (channel != updateChannel) {
                            onUpdateChannelSelected(channel)
                        }
                    },
                    label = { it.displayName },
                )
            }

            UpdateStatusContent(
                state = state,
                updateChannel = updateChannel,
                onCheckForUpdates = onCheckForUpdates,
                onStartDownload = onStartDownload,
                onCancelDownload = onCancelDownload,
                onInstallUpdate = onInstallUpdate,
            )
        }
    }
}

@Composable
private fun UpdateVersionHeaderRow(
    currentVersion: String,
    canCheck: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    val isCurrentNightly = currentVersion.contains("nightly", ignoreCase = true)
    val tagColor =
        if (isCurrentNightly) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = "Actualizaciones",
                    style = MeloType.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = if (isCurrentNightly) "Nightly" else "Estable",
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = tagColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Text(
                text = "Versión instalada: v$currentVersion",
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (canCheck) {
            IconButton(
                onClick = onCheckForUpdates,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Buscar actualizaciones",
                    tint = MeloColors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun UpdateStatusContent(
    state: UpdateState,
    updateChannel: UpdateChannel,
    onCheckForUpdates: (UpdateChannel) -> Unit,
    onStartDownload: (AppRelease, ReleaseAsset) -> Unit,
    onCancelDownload: () -> Unit,
    onInstallUpdate: (String) -> Unit,
) {
    var showChangelog by remember { mutableStateOf(false) }

    when (state) {
        is UpdateState.Idle -> Unit

        is UpdateState.Checking -> {
            SettingsStatusBanner(
                title = "Comprobando nuevas versiones...",
                isLoading = true,
                tone = StatusTone.NEUTRAL,
            )
        }

        is UpdateState.UpToDate -> {
            SettingsStatusBanner(
                icon = Icons.Default.CheckCircle,
                title = "¡Melo está actualizado!",
                body = "v${state.currentVersion} • ${state.channel.displayName}",
                tone = StatusTone.SUCCESS,
            )
        }

        is UpdateState.UpdateAvailable -> {
            UpdateAvailableBanner(
                state = state,
                showChangelog = showChangelog,
                onToggleChangelog = { showChangelog = !showChangelog },
                onStartDownload = onStartDownload,
            )
        }

        is UpdateState.ManualUpdate -> {
            val uriHandler = LocalUriHandler.current
            val hint =
                if (state.method == UpdateInstallMethod.PACKAGE_MANAGER) {
                    "Melo se instaló desde un paquete del sistema: actualízalo con tu gestor."
                } else {
                    "Esta instalación no admite actualizaciones automáticas. Usa el enlace para descargarla."
                }
            SettingsStatusBanner(
                icon = Icons.Default.CloudDownload,
                title = "¡Nueva versión disponible: v${state.release.version}!",
                body = hint,
                tone = StatusTone.PRIMARY,
                actions = {
                    Button(
                        onClick = { uriHandler.openUri(state.release.htmlUrl) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir página de descarga", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        }

        is UpdateState.Downloading -> {
            val percent = (state.progressFraction * 100).toInt()
            SettingsStatusBanner(
                isLoading = true,
                title = "Descargando actualización v${state.release.version} ($percent%)",
                body = "${formatMegabytes(state.bytesDownloaded)} / ${formatMegabytes(state.totalBytes)}",
                tone = StatusTone.NEUTRAL,
                extraContent = {
                    LinearProgressIndicator(
                        progress = { state.progressFraction },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MeloColors.surface2,
                    )
                },
                actions = {
                    OutlinedButton(
                        onClick = onCancelDownload,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MeloType.labelSmall,
                            color = MeloColors.textMuted,
                        )
                    }
                },
            )
        }

        is UpdateState.ReadyToInstall -> {
            SettingsStatusBanner(
                icon = Icons.Default.CheckCircle,
                title = "¡Descarga completa!",
                body = "La nueva versión de Melo está lista para instalarse.",
                tone = StatusTone.PRIMARY,
                actions = {
                    Button(
                        onClick = { onInstallUpdate(state.installerPath) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("Reiniciar y actualizar ahora", fontWeight = FontWeight.Bold)
                    }
                },
            )
        }

        is UpdateState.Error -> {
            SettingsStatusBanner(
                icon = Icons.Default.Info,
                title = "Error de actualización",
                body = state.message,
                tone = StatusTone.ERROR,
                actions =
                    if (state.canRetry) {
                        {
                            OutlinedButton(
                                onClick = { onCheckForUpdates(updateChannel) },
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Reintentar", style = MeloType.labelSmall)
                            }
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun UpdateAvailableBanner(
    state: UpdateState.UpdateAvailable,
    showChangelog: Boolean,
    onToggleChangelog: () -> Unit,
    onStartDownload: (AppRelease, ReleaseAsset) -> Unit,
) {
    val releaseTitle =
        if (state.release.channel == UpdateChannel.NIGHTLY) {
            "¡Nueva versión Nightly disponible: v${state.release.version}!"
        } else {
            "¡Nueva versión disponible: v${state.release.version}!"
        }
    val hasNotes = state.release.releaseNotes.isNotBlank()

    SettingsStatusBanner(
        icon = Icons.Default.SystemUpdate,
        title = releaseTitle,
        body = state.targetAsset?.name ?: "Instalador de actualización",
        tone = StatusTone.PRIMARY,
        extraContent =
            if (hasNotes) {
                {
                    AnimatedVisibility(visible = showChangelog) {
                        Text(
                            text = state.release.releaseNotes,
                            style = MeloType.labelSmall,
                            color = MeloColors.textSecondary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MeloColors.surface1)
                                    .padding(10.dp),
                        )
                    }
                }
            } else {
                null
            },
        actions = {
            if (hasNotes) {
                OutlinedButton(
                    onClick = onToggleChangelog,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (showChangelog) "Ocultar novedades" else "Ver novedades",
                        style = MeloType.labelSmall,
                    )
                }
            }
            val targetAsset = state.targetAsset
            if (targetAsset != null) {
                Button(
                    onClick = { onStartDownload(state.release, targetAsset) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Descargar e instalar",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    )
}

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
fun PreviewUpdateSettings() {
    MaterialTheme {
        UpdateSettingsSectionContent(
            currentVersion = "2.3.0-nightly.20261113.a1b2c3d4e5f6-universal",
            state =
                UpdateState.Downloading(
                    release =
                        AppRelease(
                            version = "2.3.0-nightly.20261114.f6e5d4c3b2a1",
                            tagName = "v2.3.0-nightly",
                            name = "Melo Nightly",
                            channel = UpdateChannel.NIGHTLY,
                            htmlUrl = "https://github.com/adriianh/melo",
                            releaseNotes = "",
                            publishedAt = "2026-11-13",
                            assets = emptyList(),
                        ),
                    targetAsset =
                        ReleaseAsset(
                            name = "melo-2.3.0-nightly.apk",
                            downloadUrl = "https://github.com/adriianh/melo",
                            sizeBytes = 20_000_000,
                            platform = UpdatePlatform.ANDROID,
                        ),
                    bytesDownloaded = 12_000_000,
                    totalBytes = 20_000_000,
                    progressFraction = 0.6f,
                ),
            autoCheckUpdates = true,
            updateChannel = UpdateChannel.NIGHTLY,
            onToggleAutoCheckUpdates = {},
            onUpdateChannelSelected = {},
            onCheckForUpdates = {},
            onStartDownload = { _, _ -> },
            onCancelDownload = {},
            onInstallUpdate = {},
        )
    }
}

private fun formatMegabytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return "${(mb * 10).toInt() / 10.0} MB"
}
