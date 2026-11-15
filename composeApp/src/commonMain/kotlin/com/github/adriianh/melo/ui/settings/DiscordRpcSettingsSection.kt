package com.github.adriianh.melo.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
internal fun DiscordRpcAccountEntry(
    token: String?,
    onTokenChanged: (String?) -> Unit,
) {
    var showDiscordLoginDialog by remember { mutableStateOf(false) }

    if (showDiscordLoginDialog) {
        DiscordLoginDialog(
            onDismiss = { showDiscordLoginDialog = false },
            onTokenSaved = { savedToken ->
                onTokenChanged(savedToken)
                showDiscordLoginDialog = false
            },
        )
    }

    DiscordRpcAccountRow(
        token = token,
        onUnlink = { onTokenChanged(null) },
        onOpenLogin = { showDiscordLoginDialog = true },
    )
}

@Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")
@Composable
private fun DiscordRpcAccountRow(
    token: String?,
    onUnlink: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    val isLinked = !token.isNullOrBlank()
    var showUnlinkConfirm by rememberSaveable { mutableStateOf(false) }

    if (showUnlinkConfirm) {
        SettingsConfirmDialog(
            title = "Desvincular cuenta de Discord",
            message =
                "¿Estás seguro de que deseas desvincular tu cuenta de Discord? " +
                    "Se eliminará el token de sesión guardado y dejará de mostrarse tu actividad.",
            confirmText = "Desvincular",
            icon = Icons.Default.LinkOff,
            onConfirm = {
                showUnlinkConfirm = false
                onUnlink()
            },
            onDismiss = { showUnlinkConfirm = false },
        )
    }

    SettingActionRow(
        title = "Estado de la cuenta",
        subtitle =
            if (isLinked) {
                "Cuenta vinculada con token de sesión activo."
            } else {
                "Requiere inicio de sesión para conectar con el Gateway de Discord."
            },
    ) {
        if (isLinked) {
            SettingActionButton(
                text = "Cambiar",
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.SECONDARY,
                icon = Icons.Default.SwapHoriz,
                modifier = Modifier.actionWidth(),
            )
            SettingActionButton(
                text = "Desvincular",
                onClick = { showUnlinkConfirm = true },
                emphasis = SettingActionEmphasis.DESTRUCTIVE,
                icon = Icons.Default.LinkOff,
                modifier = Modifier.actionWidth(),
            )
        } else {
            SettingActionButton(
                text = "Vincular",
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.PRIMARY,
                icon = Icons.Default.Link,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}
