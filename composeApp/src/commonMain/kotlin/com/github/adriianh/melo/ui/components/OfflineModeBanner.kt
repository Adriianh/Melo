package com.github.adriianh.melo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
        title = if (isManualOffline) "Modo sin conexión" else "Sin conexión a internet",
        subtitle = if (isManualOffline) {
            "Reproduciendo únicamente pistas descargadas y locales"
        } else {
            "Comprueba tu conexión para acceder a todo el catálogo"
        },
        icon = Icons.Default.CloudOff,
        actionLabel = if (isManualOffline) "Desactivar" else "Reintentar",
        onAction = if (isManualOffline) onDisableManual else onRetry,
    )
}
