@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.login.isDiscordInAppLoginSupported
import com.github.adriianh.melo.util.MeloColors

@Composable
fun DiscordLoginDialog(
    onDismiss: () -> Unit,
    onTokenSaved: (String) -> Unit,
) {
    var manualToken by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var isShowingInAppBrowser by remember { mutableStateOf(false) }
    val inAppBrowserAvailable = remember { isDiscordInAppLoginSupported() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MeloColors.surface1.copy(alpha = 0.95f),
        textContentColor = MeloColors.textPrimary,
        titleContentColor = MeloColors.textPrimary,
        tonalElevation = 10.dp,
        modifier =
            Modifier.border(
                BorderStroke(1.dp, MeloColors.borderStrong),
                RoundedCornerShape(24.dp),
            ),
        title = {
            DiscordLoginDialogTitle(
                isShowingInAppBrowser = isShowingInAppBrowser,
                onBackClick = { isShowingInAppBrowser = false },
            )
        },
        text = {
            DiscordLoginDialogBody(
                isShowingInAppBrowser = isShowingInAppBrowser,
                inAppBrowserAvailable = inAppBrowserAvailable,
                showManualInput = showManualInput,
                manualToken = manualToken,
                onTokenChange = { manualToken = it },
                onTokenCaptured = { token ->
                    isShowingInAppBrowser = false
                    onTokenSaved(token)
                },
                onShowInAppBrowser = { isShowingInAppBrowser = true },
                onShowManualInput = { showManualInput = true },
                onBackToAuto = { showManualInput = false },
            )
        },
        confirmButton = {
            DiscordLoginConfirmButtons(
                isShowingInAppBrowser = isShowingInAppBrowser,
                showManualInput = showManualInput,
                inAppBrowserAvailable = inAppBrowserAvailable,
                manualToken = manualToken,
                onDismiss = onDismiss,
                onTokenSaved = onTokenSaved,
                onCancelInAppBrowser = { isShowingInAppBrowser = false },
            )
        },
    )
}

@Preview
@Composable
fun PreviewDiscordLoginDialog() {
    MaterialTheme {
        DiscordLoginDialog(
            onDismiss = {},
            onTokenSaved = {},
        )
    }
}
