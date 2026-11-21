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
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.settings_discord_account_title
import com.github.adriianh.melo.resources.settings_discord_change_action
import com.github.adriianh.melo.resources.settings_discord_link_action
import com.github.adriianh.melo.resources.settings_discord_linked_desc
import com.github.adriianh.melo.resources.settings_discord_unlink_action
import com.github.adriianh.melo.resources.settings_discord_unlink_message
import com.github.adriianh.melo.resources.settings_discord_unlink_title
import com.github.adriianh.melo.resources.settings_discord_unlinked_desc
import org.jetbrains.compose.resources.stringResource

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
            title = stringResource(Res.string.settings_discord_unlink_title),
            message = stringResource(Res.string.settings_discord_unlink_message),
            confirmText = stringResource(Res.string.settings_discord_unlink_action),
            icon = Icons.Default.LinkOff,
            onConfirm = {
                showUnlinkConfirm = false
                onUnlink()
            },
            onDismiss = { showUnlinkConfirm = false },
        )
    }

    SettingActionRow(
        title = stringResource(Res.string.settings_discord_account_title),
        subtitle =
            stringResource(
                if (isLinked) {
                    Res.string.settings_discord_linked_desc
                } else {
                    Res.string.settings_discord_unlinked_desc
                },
            ),
    ) {
        if (isLinked) {
            SettingActionButton(
                text = stringResource(Res.string.settings_discord_change_action),
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.SECONDARY,
                icon = Icons.Default.SwapHoriz,
                modifier = Modifier.actionWidth(),
            )
            SettingActionButton(
                text = stringResource(Res.string.settings_discord_unlink_action),
                onClick = { showUnlinkConfirm = true },
                emphasis = SettingActionEmphasis.DESTRUCTIVE,
                icon = Icons.Default.LinkOff,
                modifier = Modifier.actionWidth(),
            )
        } else {
            SettingActionButton(
                text = stringResource(Res.string.settings_discord_link_action),
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.PRIMARY,
                icon = Icons.Default.Link,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}
