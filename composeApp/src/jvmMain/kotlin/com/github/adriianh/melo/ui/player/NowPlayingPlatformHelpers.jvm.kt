@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

private val blankPointerIcon: PointerIcon? by lazy {
    if (GraphicsEnvironment.isHeadless()) {
        null
    } else {
        runCatching {
            val blankImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            val awtCursor =
                Toolkit.getDefaultToolkit().createCustomCursor(
                    blankImage,
                    Point(0, 0),
                    "melo_blank_cursor",
                )
            PointerIcon(awtCursor)
        }.getOrNull()
    }
}

@Composable
internal actual fun PlayerPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float?) -> Unit,
    onBack: () -> Unit,
) = Unit

@Composable
internal actual fun rememberPrefersReducedMotion(): Boolean = false

internal actual fun Modifier.idleCursorHidden(hidden: Boolean): Modifier {
    val icon = if (hidden) blankPointerIcon else null
    return if (icon != null) {
        this.pointerHoverIcon(icon, overrideDescendants = true)
    } else {
        this
    }
}
