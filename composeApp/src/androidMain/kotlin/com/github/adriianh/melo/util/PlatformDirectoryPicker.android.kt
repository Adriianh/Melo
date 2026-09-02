package com.github.adriianh.melo.util

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberDirectoryPicker(onDirectorySelected: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = getPathFromTreeUri(uri)
            if (path != null) {
                onDirectorySelected(path)
            }
        }
    }

    return {
        launcher.launch(null)
    }
}

private fun getPathFromTreeUri(uri: Uri): String? {
    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        val relativePath = if (split.size > 1) split[1] else ""

        if ("primary".equals(type, ignoreCase = true)) {
            val root = Environment.getExternalStorageDirectory().absolutePath
            if (relativePath.isNotBlank()) "$root/$relativePath" else root
        } else {
            "/storage/$type/$relativePath"
        }
    } catch (_: Exception) {
        Uri.decode(uri.toString())
    }
}