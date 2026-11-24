@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberPlatformShareHandler(onCopiedToClipboard: () -> Unit): (String) -> Unit =
    remember(onCopiedToClipboard) {
        { text ->
            val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootController != null) {
                val activityController =
                    UIActivityViewController(
                        activityItems = listOf(text),
                        applicationActivities = null,
                    )
                rootController.presentViewController(activityController, animated = true, completion = null)
            } else {
                UIPasteboard.generalPasteboard.string = text
                onCopiedToClipboard()
            }
        }
    }
