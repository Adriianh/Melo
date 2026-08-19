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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var cookies by remember { mutableStateOf("") }
    var isWaitingBrowserAuth by remember { mutableStateOf(false) }
    var browserAuthStatus by remember { mutableStateOf<String?>(null) }
    var showManualInput by remember { mutableStateOf(false) }
    var isShowingInAppBrowser by remember { mutableStateOf(false) }
    val autoBrowserAvailable = remember { isAutomatedBrowserLoginAvailable() }
    val inAppBrowserAvailable = remember { isInAppSignInAvailable() }

    AlertDialog(
        onDismissRequest = {
            if (!isWaitingBrowserAuth && !state.isVerifying) {
                onDismiss()
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MeloColors.surface1.copy(alpha = 0.90f),
        textContentColor = MeloColors.textPrimary,
        titleContentColor = MeloColors.textPrimary,
        tonalElevation = 10.dp,
        modifier = Modifier
            .border(
                BorderStroke(1.dp, MeloColors.borderStrong),
                RoundedCornerShape(24.dp)
            ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isShowingInAppBrowser) {
                    IconButton(
                        onClick = { isShowingInAppBrowser = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MeloColors.textPrimary
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    if (state.isLoggedIn) "Cuenta de YouTube Music"
                    else if (isShowingInAppBrowser) "Acceso con Google"
                    else "Iniciar sesión en YouTube Music",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.isVerifying -> VerifyingContent()

                    isWaitingBrowserAuth -> WaitingBrowserContent(status = browserAuthStatus)

                    isShowingInAppBrowser -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Inicia sesión con tu cuenta de Google. Melo detectará tu sesión automáticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MeloColors.surface1
                            ) {
                                InAppSignInBrowser(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    onCookiesCaptured = { capturedCookies ->
                                        isShowingInAppBrowser = false
                                        viewModel.saveSessionCookies(capturedCookies)
                                    }
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Completar inicio de sesión")
                            }
                        }
                    }

                    state.isLoggedIn -> LoggedInContent(accountName = state.accountName)

                    else -> NotLoggedInContent(
                        error = state.error,
                        cookies = cookies,
                        onCookiesChange = { cookies = it },
                        showManualInput = showManualInput,
                        onToggleManualInput = { showManualInput = !showManualInput },
                        autoBrowserAvailable = autoBrowserAvailable,
                        inAppBrowserAvailable = inAppBrowserAvailable,
                        onInAppLogin = { isShowingInAppBrowser = true },
                        onAutomatedLogin = {
                            isWaitingBrowserAuth = true
                            browserAuthStatus = "Abriendo ventana de inicio de sesión seguro..."
                            coroutineScope.launch {
                                val captured = launchAutomatedBrowserLogin()
                                isWaitingBrowserAuth = false
                                browserAuthStatus = null
                                if (!captured.isNullOrBlank()) {
                                    viewModel.saveSessionCookies(captured)
                                }
                            }
                        },
                        onQuickImport = {
                            isWaitingBrowserAuth = true
                            browserAuthStatus =
                                "Buscando sesión en navegadores locales (Chrome, Edge, Brave, Firefox)..."
                            coroutineScope.launch {
                                val captured = importExistingBrowserCookies()
                                isWaitingBrowserAuth = false
                                browserAuthStatus = null
                                if (!captured.isNullOrBlank()) {
                                    viewModel.saveSessionCookies(captured)
                                } else {
                                    isWaitingBrowserAuth = true
                                    browserAuthStatus = "Iniciando ventana de navegador..."
                                    val autoCaptured = launchAutomatedBrowserLogin()
                                    isWaitingBrowserAuth = false
                                    browserAuthStatus = null
                                    if (!autoCaptured.isNullOrBlank()) {
                                        viewModel.saveSessionCookies(autoCaptured)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            when {
                state.isLoggedIn ->
                    TextButton(onClick = viewModel::logout) {
                        Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                    }

                state.isVerifying || isWaitingBrowserAuth ->
                    TextButton(onClick = { isWaitingBrowserAuth = false }) {
                        Text("Cancelar")
                    }

                isShowingInAppBrowser ->
                    TextButton(onClick = { isShowingInAppBrowser = false }) {
                        Text("Cancelar")
                    }

                showManualInput -> Row {
                    TextButton(
                        enabled = cookies.isNotBlank(),
                        onClick = { viewModel.saveSessionCookies(cookies) },
                    ) {
                        Text("Conectar")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }

                else ->
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
            }
        },
    )
}

@Composable
private fun WaitingBrowserContent(status: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = status ?: "Inicia sesión en la ventana de tu navegador.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Melo capturará tu sesión automáticamente en cuanto completes el acceso.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.primary
        )
        Text("Verificando tu cuenta de YouTube Music...")
    }
}

@Composable
private fun LoggedInContent(accountName: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (accountName != null) {
            Text(
                text = "¡Conectado como $accountName!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Tu feed de inicio, playlists y biblioteca están sincronizados con tu cuenta.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NotLoggedInContent(
    error: String?,
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
                text = "Inicia sesión con tu cuenta de Google de forma automática y 100% segura:",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = onAutomatedLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Iniciar sesión con Google (Navegador)")
            }

            FilledTonalButton(
                onClick = onQuickImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Importar sesión de navegador local")
            }

            if (inAppBrowserAvailable) {
                TextButton(
                    onClick = onInAppLogin,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("O iniciar sesión en la ventana integrada")
                }
            }

            HorizontalDivider(modifier = Modifier.height(8.dp))
        } else if (inAppBrowserAvailable) {
            Text(
                text = "Inicia sesión con tu cuenta de Google para acceder a tus playlists y biblioteca:",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = onInAppLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Iniciar sesión con Google")
            }

            HorizontalDivider(modifier = Modifier.height(8.dp))
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        TextButton(
            onClick = onToggleManualInput,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(if (showManualInput) "Ocultar ingreso manual" else "Opciones avanzadas (ingreso manual)")
        }

        if (showManualInput) {
            Text(
                text = "Pega aquí tus cookies de sesión si prefieres ingresarlas manualmente:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = cookies,
                onValueChange = onCookiesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cookies de sesión") },
                placeholder = { Text("SAPISID=...; __Secure-3PSID=...") },
                minLines = 2,
            )

            TextButton(
                onClick = { uriHandler.openUri("https://music.youtube.com") },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Abrir music.youtube.com")
            }
        }
    }
}