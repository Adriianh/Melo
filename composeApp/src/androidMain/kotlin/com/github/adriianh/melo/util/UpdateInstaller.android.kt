package com.github.adriianh.melo.util

import android.content.Intent
import androidx.core.content.FileProvider
import com.github.adriianh.data.local.ContextHolder
import java.io.File

actual class PlatformUpdateInstaller actual constructor() {
    actual fun supportsAutoRestart(installerPath: String): Boolean = false

    actual fun installAndRestart(installerPath: String): UpdateInstallResult {
        val context =
            ContextHolder.context
                ?: return UpdateInstallResult.Failed(reason = InstallErrorReason.NO_ANDROID_CONTEXT)

        val file = File(installerPath)
        if (!file.exists()) {
            return UpdateInstallResult.Failed(
                reason = InstallErrorReason.PACKAGE_NOT_FOUND,
                detail = installerPath,
            )
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
            UpdateInstallResult.Failed(
                reason = InstallErrorReason.ANDROID_INSTALLER_OPEN_FAILED,
                detail = e.message.orEmpty(),
            )
        }
    }
}
