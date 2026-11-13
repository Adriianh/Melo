@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "LongParameterList",
    "TooManyFunctions",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
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
                currentVersion = currentVersion,
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
                UpdateChannelChip(isNightly = isCurrentNightly)
            }

            Text(
                text = "Versión instalada: v$currentVersion",
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
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
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun UpdateChannelChip(isNightly: Boolean) {
    val tagColor =
        if (isNightly) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tagColor.copy(alpha = 0.15f),
    ) {
        Text(
            text = if (isNightly) "Nightly" else "Estable",
            style = MeloType.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tagColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun UpdateStatusContent(
    state: UpdateState,
    currentVersion: String,
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
                title = "Melo está actualizado",
                body = "v${state.currentVersion} · ${state.channel.displayName}",
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
            ManualUpdateBanner(
                state = state,
                currentVersion = currentVersion,
            )
        }

        is UpdateState.Downloading -> {
            DownloadingUpdateBanner(
                state = state,
                onCancelDownload = onCancelDownload,
            )
        }

        is UpdateState.ReadyToInstall -> {
            SettingsStatusBanner(
                icon = Icons.Default.CheckCircle,
                title = "Descarga completa",
                body = "v${state.release.version} · La nueva versión de Melo está lista para instalarse.",
                tone = StatusTone.PRIMARY,
                extraContent = {
                    SettingActionButton(
                        text = "Reiniciar y actualizar ahora",
                        onClick = { onInstallUpdate(state.installerPath) },
                        emphasis = SettingActionEmphasis.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }

        is UpdateState.Error -> {
            SettingsStatusBanner(
                icon = Icons.Default.Info,
                title = "Error de actualización",
                body = state.message,
                tone = StatusTone.ERROR,
                extraContent =
                    if (state.canRetry) {
                        {
                            SettingActionButton(
                                text = "Reintentar",
                                onClick = { onCheckForUpdates(updateChannel) },
                                emphasis = SettingActionEmphasis.SECONDARY,
                                icon = Icons.Default.Refresh,
                                modifier = Modifier.fillMaxWidth(),
                            )
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
    val isNightly = state.release.channel == UpdateChannel.NIGHTLY
    val hasNotes = state.release.releaseNotes.isNotBlank()
    val assetLine =
        state.targetAsset?.let { asset ->
            val sizePart = if (asset.sizeBytes > 0) " · ${formatMegabytes(asset.sizeBytes)}" else ""
            "${asset.name}$sizePart"
        } ?: "Instalador de actualización"

    SettingsStatusBanner(
        icon = Icons.Default.SystemUpdate,
        title = "Nueva versión disponible",
        tone = StatusTone.PRIMARY,
        titleBadge =
            if (isNightly) {
                { UpdateChannelChip(isNightly = true) }
            } else {
                null
            },
        extraContent = {
            UpdateVersionComparisonBlock(
                installedVersion = state.currentVersion,
                newVersion = state.release.version,
            )

            Text(
                text = assetLine,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (hasNotes) {
                ChangelogExpanderSection(
                    releaseNotes = state.release.releaseNotes,
                    expanded = showChangelog,
                    onToggle = onToggleChangelog,
                )
            }

            val targetAsset = state.targetAsset
            if (targetAsset != null) {
                SettingActionButton(
                    text = "Descargar e instalar",
                    onClick = { onStartDownload(state.release, targetAsset) },
                    emphasis = SettingActionEmphasis.PRIMARY,
                    icon = Icons.Default.CloudDownload,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun ManualUpdateBanner(
    state: UpdateState.ManualUpdate,
    currentVersion: String,
) {
    val uriHandler = LocalUriHandler.current
    val isNightly = state.release.channel == UpdateChannel.NIGHTLY
    val hint =
        if (state.method == UpdateInstallMethod.PACKAGE_MANAGER) {
            "Melo se instaló desde un paquete del sistema: actualízalo con tu gestor."
        } else {
            "Esta instalación no admite actualizaciones automáticas. Usa el enlace para descargarla."
        }

    SettingsStatusBanner(
        icon = Icons.Default.CloudDownload,
        title = "Nueva versión disponible",
        body = hint,
        tone = StatusTone.PRIMARY,
        titleBadge =
            if (isNightly) {
                { UpdateChannelChip(isNightly = true) }
            } else {
                null
            },
        extraContent = {
            UpdateVersionComparisonBlock(
                installedVersion = currentVersion,
                newVersion = state.release.version,
            )
            SettingActionButton(
                text = "Abrir página de descarga",
                onClick = { uriHandler.openUri(state.release.htmlUrl) },
                emphasis = SettingActionEmphasis.PRIMARY,
                icon = Icons.Default.CloudDownload,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun DownloadingUpdateBanner(
    state: UpdateState.Downloading,
    onCancelDownload: () -> Unit,
) {
    val percent = (state.progressFraction * 100).toInt()
    SettingsStatusBanner(
        isLoading = true,
        title = "Descargando actualización",
        body = "v${state.release.version}",
        tone = StatusTone.NEUTRAL,
        titleBadge = {
            Text(
                text = "$percent%",
                style = MeloType.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatMegabytes(state.bytesDownloaded)} / ${formatMegabytes(state.totalBytes)}",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                SettingActionButton(
                    text = "Cancelar",
                    onClick = onCancelDownload,
                    emphasis = SettingActionEmphasis.SECONDARY,
                )
            }
        },
    )
}

@Composable
private fun UpdateVersionComparisonBlock(
    installedVersion: String,
    newVersion: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Instalada: v$installedVersion",
            style = MeloType.labelSmall,
            color = MeloColors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Nueva: v$newVersion",
            style = MeloType.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MeloColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChangelogExpanderSection(
    releaseNotes: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = shape,
            color = MeloColors.surface1.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MeloColors.border),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onToggle),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Novedades",
                    style = MeloType.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MeloColors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Text(
                text = releaseNotes,
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
