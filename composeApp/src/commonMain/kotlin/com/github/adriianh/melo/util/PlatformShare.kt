@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable

/**
 * Returns a platform-specific handler that shares [text] (native share sheet on mobile,
 * clipboard copy + [onCopiedToClipboard] callback on desktop).
 */
@Composable
expect fun rememberPlatformShareHandler(onCopiedToClipboard: () -> Unit = {}): (String) -> Unit
