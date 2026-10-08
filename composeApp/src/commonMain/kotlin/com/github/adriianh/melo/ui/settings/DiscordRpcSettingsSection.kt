package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.melo.util.PlatformType

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
internal fun DiscordRpcSettingsSection(
    settings: Settings,
    actions: SettingsActions,
    platformType: PlatformType,
) {
    var showDiscordLoginDialog by remember { mutableStateOf(false) }

    if (showDiscordLoginDialog) {
        DiscordLoginDialog(
            onDismiss = { showDiscordLoginDialog = false },
            onTokenSaved = { token ->
                actions.onDiscordTokenChanged(token)
                showDiscordLoginDialog = false
            },
        )
    }

    SettingsGroupCard(
        title = "Discord Rich Presence",
        icon = Icons.Default.Share,
    ) {
        SettingRow(
            title = "Mostrar actividad en Discord",
            subtitle =
                if (platformType == PlatformType.ANDROID) {
                    "Muestra en tiempo real tu música en reproducción usando la API de Discord."
                } else {
                    "Muestra en tiempo real tu música en reproducción en tu estado de Discord Desktop."
                },
        ) {
            MeloSettingsSwitch(
                checked = settings.discordRpcEnabled,
                onCheckedChange = actions.onDiscordRpcToggle,
            )
        }

        if (platformType == PlatformType.ANDROID && settings.discordRpcEnabled) {
            DiscordRpcAccountRow(
                token = settings.discordRpcToken,
                onUnlink = { actions.onDiscordTokenChanged(null) },
                onOpenLogin = { showDiscordLoginDialog = true },
            )
        }
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun DiscordRpcAccountRow(
    token: String?,
    onUnlink: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    val isLinked = !token.isNullOrBlank()
    SettingRow(
        title = "Estado de la cuenta",
        subtitle =
            if (isLinked) {
                "Cuenta vinculada con token de sesión activo."
            } else {
                "Requiere inicio de sesión para conectar con el Gateway de Discord."
            },
    ) {
        if (isLinked) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onOpenLogin) {
                    Text("Cambiar")
                }
                OutlinedButton(
                    onClick = onUnlink,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Desvincular")
                }
            }
        } else {
            Button(
                onClick = onOpenLogin,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Vincular")
            }
        }
    }
}
