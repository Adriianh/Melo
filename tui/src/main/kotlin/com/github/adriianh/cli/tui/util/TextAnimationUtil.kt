package com.github.adriianh.cli.tui.util

object TextAnimationUtil {
    fun marqueeText(text: String, offset: Int, maxWidth: Int): String {
        if (text.length <= maxWidth) return text
        val separator = "   •   "
        val full = text + separator
        val loop = full.repeat(2)
        val start = offset % full.length
        return loop.substring(start, start + maxWidth)
    }
}