package com.github.adriianh.melo.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
        SettingToggleRow(
            title = "Mostrar actividad en Discord",
            subtitle =
                if (platformType == PlatformType.ANDROID) {
                    "Muestra en tiempo real tu música en reproducción usando la API de Discord."
                } else {
                    "Muestra en tiempo real tu música en reproducción en tu estado de Discord Desktop."
                },
            checked = settings.discordRpcEnabled,
            onCheckedChange = actions.onDiscordRpcToggle,
        )

        if (platformType == PlatformType.ANDROID) {
            DiscordRpcAccountRow(
                token = settings.discordRpcToken,
                enabled = settings.discordRpcEnabled,
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
    enabled: Boolean,
    onUnlink: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    val isLinked = !token.isNullOrBlank()
    SettingActionRow(
        title = "Estado de la cuenta",
        subtitle =
            if (isLinked) {
                "Cuenta vinculada con token de sesión activo."
            } else {
                "Requiere inicio de sesión para conectar con el Gateway de Discord."
            },
        enabled = enabled,
        disabledNote = "Activa Discord Rich Presence para vincular",
    ) {
        if (isLinked) {
            SettingActionButton(
                text = "Cambiar",
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.SECONDARY,
                icon = Icons.Default.SwapHoriz,
                enabled = enabled,
                modifier = Modifier.actionWidth(minInlineWidth = 128.dp),
            )
            SettingActionButton(
                text = "Desvincular",
                onClick = onUnlink,
                emphasis = SettingActionEmphasis.DESTRUCTIVE,
                icon = Icons.Default.LinkOff,
                enabled = enabled,
                modifier = Modifier.actionWidth(minInlineWidth = 140.dp),
            )
        } else {
            SettingActionButton(
                text = "Vincular",
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.PRIMARY,
                icon = Icons.Default.Link,
                enabled = enabled,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}
