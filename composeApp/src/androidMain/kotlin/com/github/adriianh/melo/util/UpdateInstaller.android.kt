package com.github.adriianh.melo.util

import android.content.Intent
import androidx.core.content.FileProvider
import com.github.adriianh.data.local.ContextHolder
import java.io.File

actual class PlatformUpdateInstaller actual constructor() {
    actual fun installAndRestart(installerPath: String): UpdateInstallResult {
        val context =
            ContextHolder.context
                ?: return UpdateInstallResult.Failed("No se pudo acceder al contexto de la aplicación")

        val file = File(installerPath)
        if (!file.exists()) {
            return UpdateInstallResult.Failed("No se encontró el paquete descargado en $installerPath")
        }

        return try {
            val apkUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            UpdateInstallResult.Started
        } catch (e: Exception) {
            UpdateInstallResult.Failed("No se pudo abrir el instalador de Android: ${e.message}")
        }
    }
}
