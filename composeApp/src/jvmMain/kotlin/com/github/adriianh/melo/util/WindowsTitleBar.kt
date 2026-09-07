package com.github.adriianh.melo.util

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window

/**
 * Applies immersive dark mode and optional custom caption colors to the native
 * Windows 10 and Windows 11 window title bars using the Desktop Window Manager (DWM) API.
 */
object WindowsTitleBar {

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_BORDER_COLOR = 34
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_TEXT_COLOR = 36

    private interface Dwmapi : StdCallLibrary {
        fun dwmSetWindowAttribute(
            hwnd: HWND,
            dwAttribute: Int,
            pvAttribute: Pointer,
            cbAttribute: Int
        ): Int

        companion object {
            val INSTANCE: Dwmapi? = runCatching {
                Native.load("dwmapi", Dwmapi::class.java)
            }.getOrNull()
        }
    }

    /**
     * Applies dark theme styling to the given native [window].
     *
     * @param window The AWT / Swing window.
     * @param backgroundColorRgb 24-bit RGB hex value (e.g. 0x0D0D0E for Melo dark surface).
     */
    fun applyDarkTheme(window: Window, backgroundColorRgb: Int = 0x0D0D0E) {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        if (!os.contains("win")) return

        val dwmapi = Dwmapi.INSTANCE ?: return

        try {
            val hwnd = HWND(Native.getWindowPointer(window))

            val darkModeMem = Memory(4).apply { setInt(0, 1) }
            val res = dwmapi.dwmSetWindowAttribute(
                hwnd,
                DWMWA_USE_IMMERSIVE_DARK_MODE,
                darkModeMem,
                4
            )
            if (res != 0) {
                dwmapi.dwmSetWindowAttribute(
                    hwnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1,
                    darkModeMem,
                    4
                )
            }

            val r = (backgroundColorRgb shr 16) and 0xFF
            val g = (backgroundColorRgb shr 8) and 0xFF
            val b = backgroundColorRgb and 0xFF
            val bgrColor = (b shl 16) or (g shl 8) or r

            val captionColorMem = Memory(4).apply { setInt(0, bgrColor) }
            dwmapi.dwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, captionColorMem, 4)

            val textColorMem = Memory(4).apply { setInt(0, 0x00FFFFFF) }
            dwmapi.dwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, textColorMem, 4)

            dwmapi.dwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, captionColorMem, 4)
        } catch (_: Throwable) {
        }
    }
}