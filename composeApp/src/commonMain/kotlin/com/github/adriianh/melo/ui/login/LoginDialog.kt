package com.github.adriianh.melo.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_back
import com.github.adriianh.melo.resources.common_action_cancel
import com.github.adriianh.melo.resources.common_action_close
import com.github.adriianh.melo.resources.login_browser_hint
import com.github.adriianh.melo.resources.login_browser_status_capture
import com.github.adriianh.melo.resources.login_browser_status_default
import com.github.adriianh.melo.resources.login_complete_action
import com.github.adriianh.melo.resources.login_connect_action
import com.github.adriianh.melo.resources.login_connected_as
import com.github.adriianh.melo.resources.login_cookies_label
import com.github.adriianh.melo.resources.login_cookies_placeholder
import com.github.adriianh.melo.resources.login_desktop_prompt
import com.github.adriianh.melo.resources.login_embedded_window
import com.github.adriianh.melo.resources.login_google_action
import com.github.adriianh.melo.resources.login_google_browser
import com.github.adriianh.melo.resources.login_import_browser
import com.github.adriianh.melo.resources.login_logout_action
import com.github.adriianh.melo.resources.login_manual_hide
import com.github.adriianh.melo.resources.login_manual_hint
import com.github.adriianh.melo.resources.login_manual_show
import com.github.adriianh.melo.resources.login_mobile_prompt
import com.github.adriianh.melo.resources.login_open_ytm
import com.github.adriianh.melo.resources.login_synced_subtitle
import com.github.adriianh.melo.resources.login_title_browser
import com.github.adriianh.melo.resources.login_title_default
import com.github.adriianh.melo.resources.login_title_logged_in
import com.github.adriianh.melo.resources.login_verifying
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.i18n.resolve
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var cookies by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var isShowingInAppBrowser by remember { mutableStateOf(false) }
    val autoBrowserAvailable = remember { isAutomatedBrowserLoginAvailable() }
    val inAppBrowserAvailable = remember { isInAppSignInAvailable() }

    AlertDialog(
        onDismissRequest = {
            if (!state.isWaitingBrowserAuth && !state.isVerifying) {
                onDismiss()
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MeloColors.surface1.copy(alpha = 0.90f),
        textContentColor = MeloColors.textPrimary,
        titleContentColor = MeloColors.textPrimary,
        tonalElevation = 10.dp,
        modifier =
            Modifier
                .border(
                    BorderStroke(1.dp, MeloColors.borderStrong),
                    RoundedCornerShape(24.dp),
                ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isShowingInAppBrowser) {
                    IconButton(
                        onClick = { isShowingInAppBrowser = false },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_back),
                            tint = MeloColors.textPrimary,
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    stringResource(
                        when {
                            state.isLoggedIn -> Res.string.login_title_logged_in
                            isShowingInAppBrowser -> Res.string.login_title_browser
                            else -> Res.string.login_title_default
                        },
                    ),
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.isVerifying -> VerifyingContent()
                    state.isWaitingBrowserAuth -> WaitingBrowserContent(status = state.browserAuthStatus)

                    isShowingInAppBrowser -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.login_browser_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(400.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                color = MeloColors.surface1,
                            ) {
                                InAppSignInBrowser(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    onCookiesCaptured = { capturedCookies ->
                                        isShowingInAppBrowser = false
                                        viewModel.saveSessionCookies(capturedCookies)
                                    },
                                )
                            }

                            Button(
                                onClick = {
                                    val captured = captureSessionCookies()
                                    if (!captured.isNullOrBlank()) {
                                        isShowingInAppBrowser = false
                                        viewModel.saveSessionCookies(captured)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Text(stringResource(Res.string.login_complete_action))
                            }
                        }
                    }

                    state.isLoggedIn -> LoggedInContent(accountName = state.accountName)

                    else ->
                        NotLoggedInContent(
                            error = state.error,
                            cookies = cookies,
                            onCookiesChange = { cookies = it },
                            showManualInput = showManualInput,
                            onToggleManualInput = { showManualInput = !showManualInput },
                            autoBrowserAvailable = autoBrowserAvailable,
                            inAppBrowserAvailable = inAppBrowserAvailable,
                            onInAppLogin = { isShowingInAppBrowser = true },
                            onAutomatedLogin = viewModel::startAutomatedBrowserLogin,
                            onQuickImport = viewModel::startQuickImport,
                        )
                }
            }
        },
        confirmButton = {
            when {
                state.isLoggedIn ->
                    TextButton(onClick = viewModel::logout) {
                        Text(stringResource(Res.string.login_logout_action), color = MaterialTheme.colorScheme.error)
                    }

                state.isVerifying || state.isWaitingBrowserAuth ->
                    TextButton(onClick = viewModel::cancelWaitingBrowserAuth) {
                        Text(stringResource(Res.string.common_action_cancel))
                    }

                isShowingInAppBrowser ->
                    TextButton(onClick = { isShowingInAppBrowser = false }) {
                        Text(stringResource(Res.string.common_action_cancel))
                    }

                showManualInput ->
                    Row {
                        TextButton(
                            enabled = cookies.isNotBlank(),
                            onClick = { viewModel.saveSessionCookies(cookies) },
                        ) {
                            Text(stringResource(Res.string.login_connect_action))
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.common_action_close))
                        }
                    }

                else ->
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.common_action_close))
                    }
            }
        },
    )
}

@Composable
private fun WaitingBrowserContent(status: UiText?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = status?.resolve() ?: stringResource(Res.string.login_browser_status_default),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(Res.string.login_browser_status_capture),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VerifyingContent() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(Res.string.login_verifying))
    }
}

@Composable
private fun LoggedInContent(accountName: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (accountName != null) {
            Text(
                text = stringResource(Res.string.login_connected_as, accountName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(Res.string.login_synced_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NotLoggedInContent(
    error: UiText?,
    cookies: String,
    onCookiesChange: (String) -> Unit,
    showManualInput: Boolean,
    onToggleManualInput: () -> Unit,
    autoBrowserAvailable: Boolean,
    inAppBrowserAvailable: Boolean,
    onInAppLogin: () -> Unit,
    onAutomatedLogin: () -> Unit,
    onQuickImport: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (autoBrowserAvailable) {
            Text(
                text = stringResource(Res.string.login_desktop_prompt),
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = onAutomatedLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.login_google_browser))
            }

            FilledTonalButton(
                onClick = onQuickImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.login_import_browser))
            }

            if (inAppBrowserAvailable) {
                TextButton(
                    onClick = onInAppLogin,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(Res.string.login_embedded_window))
                }
            }

            HorizontalDivider(modifier = Modifier.height(8.dp))
        } else if (inAppBrowserAvailable) {
            Text(
                text = stringResource(Res.string.login_mobile_prompt),
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = onInAppLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.login_google_action))
            }

            HorizontalDivider(modifier = Modifier.height(8.dp))
        }

        if (error != null) {
            Text(
                text = error.resolve(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        TextButton(
            onClick = onToggleManualInput,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                stringResource(
                    if (showManualInput) Res.string.login_manual_hide else Res.string.login_manual_show,
                ),
            )
        }

        if (showManualInput) {
            Text(
                text = stringResource(Res.string.login_manual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = cookies,
                onValueChange = onCookiesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.login_cookies_label)) },
                placeholder = { Text(stringResource(Res.string.login_cookies_placeholder)) },
                minLines = 2,
            )

            TextButton(
                onClick = { uriHandler.openUri("https://music.youtube.com") },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.login_open_ytm))
            }
        }
    }
}
