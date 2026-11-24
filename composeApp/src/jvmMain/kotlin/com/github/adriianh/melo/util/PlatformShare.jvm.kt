@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberPlatformShareHandler(onCopiedToClipboard: () -> Unit): (String) -> Unit =
    remember(onCopiedToClipboard) {
        { text ->
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            }
            onCopiedToClipboard()
        }
    }
