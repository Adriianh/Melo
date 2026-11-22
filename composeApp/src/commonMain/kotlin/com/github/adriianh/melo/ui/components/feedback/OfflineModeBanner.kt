package com.github.adriianh.melo.ui.components.feedback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_retry
import com.github.adriianh.melo.resources.offline_disable
import com.github.adriianh.melo.resources.offline_manual_subtitle
import com.github.adriianh.melo.resources.offline_manual_title
import com.github.adriianh.melo.resources.offline_subtitle
import com.github.adriianh.melo.resources.offline_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun OfflineModeBanner(
    modifier: Modifier = Modifier,
    visible: Boolean,
    isManualOffline: Boolean = false,
    onDisableManual: () -> Unit,
    onRetry: () -> Unit,
) {
    MeloBanner(
        visible = visible,
        modifier = modifier,
        title =
            if (isManualOffline) {
                stringResource(Res.string.offline_manual_title)
            } else {
                stringResource(Res.string.offline_title)
            },
        subtitle =
            if (isManualOffline) {
                stringResource(Res.string.offline_manual_subtitle)
            } else {
                stringResource(Res.string.offline_subtitle)
            },
        icon = Icons.Default.CloudOff,
        actionLabel =
            if (isManualOffline) {
                stringResource(Res.string.offline_disable)
            } else {
                stringResource(Res.string.common_action_retry)
            },
        onAction = if (isManualOffline) onDisableManual else onRetry,
    )
}
