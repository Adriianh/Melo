package com.github.adriianh.melo.util

import android.content.Intent
import android.net.Uri
import com.github.adriianh.data.local.ContextHolder
import java.io.File

actual class PlatformUpdateInstaller actual constructor() {
    actual fun installAndRestart(installerPath: String) {
        val context = ContextHolder.context ?: return
        try {
            val file = File(installerPath)
            if (file.exists()) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {
        }
    }
}