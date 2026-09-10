package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

@Composable
actual fun rememberDirectoryPicker(onDirectorySelected: (String) -> Unit): () -> Unit {
    return remember(onDirectorySelected) {
        {
            try {
                val os = System.getProperty("os.name")?.lowercase() ?: ""
                if (os.contains("mac")) {
                    System.setProperty("apple.awt.fileDialogForDirectories", "true")
                    val dialog = FileDialog(null as Frame?, "Seleccionar carpeta", FileDialog.LOAD)
                    dialog.isVisible = true
                    val dir = dialog.directory
                    val file = dialog.file
                    System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    if (dir != null && file != null) {
                        onDirectorySelected(File(dir, file).absolutePath)
                    } else if (dir != null) {
                        onDirectorySelected(dir)
                    }
                } else {
                    val chooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = "Seleccionar carpeta de música"
                        isAcceptAllFileFilterUsed = false
                    }
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        chooser.selectedFile?.absolutePath?.let(onDirectorySelected)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}