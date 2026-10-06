@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.login.InAppDiscordLogin
import com.github.adriianh.melo.ui.login.isDiscordInAppLoginSupported
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

private const val DISCLAIMER_TEXT =
    "Aviso importante: En Android, Discord no admite Rich Presence local. " +
        "Esta integración conecta con el Gateway usando tu token de sesión (KizzyRPC). " +
        "El uso de clientes no oficiales va contra los Términos de Servicio de Discord. " +
        "Se ofrece exclusivamente con fines educativos y bajo tu propia responsabilidad."

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

@Composable
private fun DiscordLoginDialogBody(
    isShowingInAppBrowser: Boolean,
    inAppBrowserAvailable: Boolean,
    showManualInput: Boolean,
    manualToken: String,
    onTokenChange: (String) -> Unit,
    onTokenCaptured: (String) -> Unit,
    onShowInAppBrowser: () -> Unit,
    onShowManualInput: () -> Unit,
    onBackToAuto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isShowingInAppBrowser) {
            DiscordInAppBrowserContent(onTokenCaptured = onTokenCaptured)
        } else {
            DiscordDisclaimerBanner()
            if (inAppBrowserAvailable && !showManualInput) {
                DiscordAutoLoginOptions(
                    onShowInAppBrowser = onShowInAppBrowser,
                    onShowManualInput = onShowManualInput,
                )
            } else {
                DiscordManualInputContent(
                    manualToken = manualToken,
                    onTokenChange = onTokenChange,
                    inAppBrowserAvailable = inAppBrowserAvailable,
                    onBackToAuto = onBackToAuto,
                )
            }
        }
    }
}

@Composable
private fun DiscordLoginDialogTitle(
    isShowingInAppBrowser: Boolean,
    onBackClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isShowingInAppBrowser) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = MeloColors.textPrimary,
                )
            }
        } else {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = if (isShowingInAppBrowser) "Acceso a Discord" else "Vincular Discord en Android",
            style = MeloType.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DiscordDisclaimerBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = DISCLAIMER_TEXT,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
            )
        }
    }
}

@Composable
private fun DiscordInAppBrowserContent(onTokenCaptured: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Inicia sesión con tu cuenta de Discord. Tu token de sesión será capturado automáticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(12.dp)),
            color = MeloColors.surface1,
        ) {
            InAppDiscordLogin(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                onTokenCaptured = onTokenCaptured,
            )
        }
    }
}

@Composable
private fun DiscordAutoLoginOptions(
    onShowInAppBrowser: () -> Unit,
    onShowManualInput: () -> Unit,
) {
    Button(
        onClick = onShowInAppBrowser,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(
            Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("Iniciar sesión en Discord")
    }

    TextButton(
        onClick = onShowManualInput,
    ) {
        Text("O ingresar token manualmente")
    }
}

@Composable
private fun DiscordManualInputContent(
    manualToken: String,
    onTokenChange: (String) -> Unit,
    inAppBrowserAvailable: Boolean,
    onBackToAuto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Pega tu token de usuario de Discord:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = manualToken,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Token de Discord") },
            placeholder = { Text("OTQ... o mfa....") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        if (inAppBrowserAvailable) {
            TextButton(
                onClick = onBackToAuto,
            ) {
                Text("Volver a inicio de sesión automático")
            }
        }
    }
}

@Composable
private fun DiscordLoginConfirmButtons(
    isShowingInAppBrowser: Boolean,
    showManualInput: Boolean,
    inAppBrowserAvailable: Boolean,
    manualToken: String,
    onDismiss: () -> Unit,
    onTokenSaved: (String) -> Unit,
    onCancelInAppBrowser: () -> Unit,
) {
    if (isShowingInAppBrowser) {
        TextButton(onClick = onCancelInAppBrowser) {
            Text("Cancelar")
        }
    } else if (showManualInput || !inAppBrowserAvailable) {
        Row {
            Button(
                enabled = manualToken.isNotBlank(),
                onClick = { onTokenSaved(manualToken.trim()) },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Guardar token")
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    } else {
        TextButton(onClick = onDismiss) {
            Text("Cerrar")
        }
    }
}
