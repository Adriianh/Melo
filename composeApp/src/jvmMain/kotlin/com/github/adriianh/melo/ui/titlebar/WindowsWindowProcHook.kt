package com.github.adriianh.melo.ui.titlebar

import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Window
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.SwingUtilities

private const val WM_SIZE = 0x0005
private const val WM_GETMINMAXINFO = 0x0024
private const val WM_NCDESTROY = 0x0082
private const val WM_NCCALCSIZE = 0x0083

private const val SIZE_RESTORED = 0
private const val SIZE_MINIMIZED = 1
private const val SIZE_MAXIMIZED = 2

internal data class Win32Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

internal data class MaxTrackBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

internal data class FrameBorderInsets(
    val horizontal: Int,
    val vertical: Int,
)

internal fun computeMaxTrackBounds(
    monitorRect: Win32Rect,
    workRect: Win32Rect,
    frameBorder: FrameBorderInsets,
): MaxTrackBounds =
    MaxTrackBounds(
        x = (workRect.left - monitorRect.left) - frameBorder.horizontal,
        y = (workRect.top - monitorRect.top) - frameBorder.vertical,
        width = workRect.width + frameBorder.horizontal * 2,
        height = workRect.height + frameBorder.vertical * 2,
    )

/**
 * Adjusts the maximized `WM_NCCALCSIZE` proposed window rectangle inward by [frameBorder] so the
 * client area aligns flush with the monitor work area while the `WS_THICKFRAME` border overhangs
 * off-screen like a standard decorated Win32 window.
 */
internal fun computeMaximizedClientRect(
    windowRect: Win32Rect,
    frameBorder: FrameBorderInsets,
): Win32Rect {
    val leftInset = frameBorder.horizontal.coerceAtLeast(0)
    val topInset = frameBorder.vertical.coerceAtLeast(0)
    return Win32Rect(
        left = windowRect.left + leftInset,
        top = windowRect.top + topInset,
        right = (windowRect.right - leftInset).coerceAtLeast(windowRect.left + leftInset),
        bottom = (windowRect.bottom - topInset).coerceAtLeast(windowRect.top + topInset),
    )
}

internal fun isWindowsPlatform(): Boolean = System.getProperty("os.name")?.contains("win", ignoreCase = true) == true

/**
 * Subclasses an AWT [Window]'s Win32 `HWND` on Windows so `SW_MAXIMIZE` / `SW_RESTORE` work
 * natively on an undecorated Compose window with full DWM frame recognition and zero desktop gaps.
 */
internal class WindowsWindowProcHook(
    private val window: Window,
    private val onMaximizedChanged: (Boolean) -> Unit,
    private val onMinimizedChanged: (Boolean) -> Unit = {},
) {
    private var hwnd: WinDef.HWND? = null
    private var originalWndProc: Pointer? = null
    private var subclassProc: WinUser.WindowProc? = null
    private var hierarchyListener: HierarchyListener? = null
    private var isAttached = false

    fun attach() {
        if (!isWindowsPlatform() || isAttached) return

        if (window.isDisplayable) {
            installHook()
        } else if (hierarchyListener == null) {
            val listener =
                HierarchyListener { event ->
                    val displayabilityChanged =
                        (event.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L
                    if (displayabilityChanged && window.isDisplayable) {
                        removeHierarchyListener()
                        installHook()
                    }
                }
            hierarchyListener = listener
            window.addHierarchyListener(listener)
        }
    }

    fun detach() {
        removeHierarchyListener()
        restoreOriginalWndProc()
        isAttached = false
        hwnd = null
        subclassProc = null
    }

    fun showWindow(nCmdShow: Int): Boolean {
        val target = activeHwnd() ?: return false
        User32.INSTANCE.ShowWindow(target, nCmdShow)
        return true
    }

    fun isZoomed(): Boolean? = activeHwnd()?.let { Win32User32Ext.INSTANCE.IsZoomed(it) }

    private fun activeHwnd(): WinDef.HWND? = hwnd?.takeIf { isAttached && User32.INSTANCE.IsWindow(it) }

    private fun installHook() {
        if (isAttached || !window.isDisplayable) return

        val pointer = runCatching { Native.getWindowPointer(window) }.getOrNull() ?: return
        val target = WinDef.HWND(pointer)
        if (User32.INSTANCE.IsWindow(target)) {
            val proc =
                WinUser.WindowProc { hWnd, uMsg, wParam, lParam ->
                    handleWindowMessage(hWnd, uMsg, wParam, lParam)
                }
            val procPtr = CallbackReference.getFunctionPointer(proc)
            val prevProc = User32.INSTANCE.SetWindowLongPtr(target, WinUser.GWL_WNDPROC, procPtr)
            if (prevProc != null) {
                hwnd = target
                subclassProc = proc
                originalWndProc = prevProc
                isAttached = true

                ensureDecoratedFrameStyle(target)
                extendDwmFrameIntoClientArea(target)
                User32.INSTANCE.SetWindowPos(
                    target,
                    null,
                    0,
                    0,
                    0,
                    0,
                    WinUser.SWP_NOMOVE or
                        WinUser.SWP_NOSIZE or
                        WinUser.SWP_NOZORDER or
                        WinUser.SWP_NOACTIVATE or
                        WinUser.SWP_FRAMECHANGED,
                )
                notifyMaximized(Win32User32Ext.INSTANCE.IsZoomed(target))
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleWindowMessage(
        hWnd: WinDef.HWND,
        uMsg: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM,
    ): WinDef.LRESULT {
        val prevProc = originalWndProc ?: return User32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam)

        return try {
            when (uMsg) {
                WM_GETMINMAXINFO -> handleGetMinMaxInfo(prevProc, hWnd, uMsg, wParam, lParam)
                WM_NCCALCSIZE -> handleNcCalcSize(prevProc, hWnd, uMsg, wParam, lParam)
                WM_SIZE -> {
                    val result = User32.INSTANCE.CallWindowProc(prevProc, hWnd, uMsg, wParam, lParam)
                    when (wParam.toInt()) {
                        SIZE_MINIMIZED -> notifyMinimized(true)
                        SIZE_MAXIMIZED -> {
                            notifyMinimized(false)
                            notifyMaximized(true)
                        }
                        SIZE_RESTORED -> {
                            notifyMinimized(false)
                            notifyMaximized(false)
                        }
                    }
                    result
                }
                WM_NCDESTROY -> {
                    restoreOriginalWndProc()
                    isAttached = false
                    hwnd = null
                    subclassProc = null
                    User32.INSTANCE.CallWindowProc(prevProc, hWnd, uMsg, wParam, lParam)
                }
                else -> User32.INSTANCE.CallWindowProc(prevProc, hWnd, uMsg, wParam, lParam)
            }
        } catch (_: Throwable) {
            User32.INSTANCE.CallWindowProc(prevProc, hWnd, uMsg, wParam, lParam)
        }
    }

    private fun notifyMaximized(maximized: Boolean) {
        if (SwingUtilities.isEventDispatchThread()) {
            onMaximizedChanged(maximized)
        } else {
            SwingUtilities.invokeLater {
                onMaximizedChanged(maximized)
            }
        }
    }

    private fun notifyMinimized(minimized: Boolean) {
        if (SwingUtilities.isEventDispatchThread()) {
            onMinimizedChanged(minimized)
        } else {
            SwingUtilities.invokeLater {
                onMinimizedChanged(minimized)
            }
        }
    }

    private fun restoreOriginalWndProc() {
        val target = hwnd ?: return
        val prevProc = originalWndProc ?: return
        originalWndProc = null
        if (User32.INSTANCE.IsWindow(target)) {
            User32.INSTANCE.SetWindowLongPtr(target, WinUser.GWL_WNDPROC, prevProc)
        }
    }

    private fun removeHierarchyListener() {
        val listener = hierarchyListener ?: return
        hierarchyListener = null
        window.removeHierarchyListener(listener)
    }
}

private fun ensureDecoratedFrameStyle(target: WinDef.HWND) {
    val currentStyle = User32.INSTANCE.GetWindowLong(target, WinUser.GWL_STYLE)
    val desiredStyle =
        currentStyle or
            WinUser.WS_THICKFRAME or
            WinUser.WS_CAPTION or
            WinUser.WS_MAXIMIZEBOX or
            WinUser.WS_MINIMIZEBOX
    if (desiredStyle != currentStyle) {
        User32.INSTANCE.SetWindowLong(target, WinUser.GWL_STYLE, desiredStyle)
    }
}

private fun extendDwmFrameIntoClientArea(target: WinDef.HWND) {
    runCatching {
        val margins =
            Win32Margins().apply {
                cxLeftWidth = 0
                cxRightWidth = 0
                cyTopHeight = 1
                cyBottomHeight = 0
            }
        Win32DwmApi.INSTANCE.DwmExtendFrameIntoClientArea(target, margins)
    }
}

private fun queryFrameBorderInsets(hWnd: WinDef.HWND): FrameBorderInsets {
    val dpi = runCatching { Win32User32Ext.INSTANCE.GetDpiForWindow(hWnd) }.getOrDefault(0)
    val paddedBorder = querySystemMetric(WinUser.SM_CXPADDEDBORDER, dpi)
    val cxSizeFrame = querySystemMetric(WinUser.SM_CXSIZEFRAME, dpi)
    val cySizeFrame = querySystemMetric(WinUser.SM_CYSIZEFRAME, dpi)
    return FrameBorderInsets(
        horizontal = cxSizeFrame + paddedBorder,
        vertical = cySizeFrame + paddedBorder,
    )
}

private fun querySystemMetric(
    index: Int,
    dpi: Int,
): Int =
    if (dpi > 0) {
        runCatching { Win32User32Ext.INSTANCE.GetSystemMetricsForDpi(index, dpi) }
            .getOrElse { User32.INSTANCE.GetSystemMetrics(index) }
    } else {
        User32.INSTANCE.GetSystemMetrics(index)
    }

private fun handleGetMinMaxInfo(
    prevProc: Pointer,
    hWnd: WinDef.HWND,
    uMsg: Int,
    wParam: WinDef.WPARAM,
    lParam: WinDef.LPARAM,
): WinDef.LRESULT {
    User32.INSTANCE.CallWindowProc(prevProc, hWnd, uMsg, wParam, lParam)
    val monitorInfo = queryMonitorInfo(hWnd) ?: return WinDef.LRESULT(0)
    val frameBorder = queryFrameBorderInsets(hWnd)
    val maxBounds =
        computeMaxTrackBounds(
            monitorRect = monitorInfo.rcMonitor.toWin32Rect(),
            workRect = monitorInfo.rcWork.toWin32Rect(),
            frameBorder = frameBorder,
        )
    val info = Win32MinMaxInfo(Pointer(lParam.toLong())).apply { read() }
    info.ptMaxPosition.x = maxBounds.x
    info.ptMaxPosition.y = maxBounds.y
    info.ptMaxSize.x = maxBounds.width
    info.ptMaxSize.y = maxBounds.height
    info.write()
    return WinDef.LRESULT(0)
}

private fun handleNcCalcSize(
    prevProc: Pointer,
    hWnd: WinDef.HWND,
    uMsg: Int,
    wParam: WinDef.WPARAM,
    lParam: WinDef.LPARAM,
): WinDef.LRESULT {
    if (wParam.toLong() == 0L) {
        return User32.INSTANCE.CallWindowProc(prevProc, hWnd, uMsg, wParam, lParam)
    }

    if (Win32User32Ext.INSTANCE.IsZoomed(hWnd)) {
        val params = Win32NcCalcSizeParams(Pointer(lParam.toLong())).apply { read() }
        val clientRect =
            computeMaximizedClientRect(
                windowRect = params.rgrc0.toWin32Rect(),
                frameBorder = queryFrameBorderInsets(hWnd),
            )
        params.rgrc0.left = clientRect.left
        params.rgrc0.top = clientRect.top
        params.rgrc0.right = clientRect.right
        params.rgrc0.bottom = clientRect.bottom
        params.write()
    }
    return WinDef.LRESULT(0)
}

private fun queryMonitorInfo(hwnd: WinDef.HWND): WinUser.MONITORINFO? {
    val monitor =
        User32.INSTANCE.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST)
            ?: return null
    val monitorInfo = WinUser.MONITORINFO()
    return if (User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo).booleanValue()) {
        monitorInfo
    } else {
        null
    }
}

private fun WinDef.RECT.toWin32Rect(): Win32Rect =
    Win32Rect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )

private interface Win32User32Ext : StdCallLibrary {
    @Suppress("FunctionName")
    fun IsZoomed(hWnd: WinDef.HWND): Boolean

    @Suppress("FunctionName")
    fun GetDpiForWindow(hWnd: WinDef.HWND): Int

    @Suppress("FunctionName")
    fun GetSystemMetricsForDpi(
        nIndex: Int,
        dpi: Int,
    ): Int

    companion object {
        val INSTANCE: Win32User32Ext =
            Native.load("user32", Win32User32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }
}

private interface Win32DwmApi : StdCallLibrary {
    @Suppress("FunctionName")
    fun DwmExtendFrameIntoClientArea(
        hWnd: WinDef.HWND,
        pMarInset: Win32Margins,
    ): WinNT.HRESULT

    companion object {
        val INSTANCE: Win32DwmApi =
            Native.load("dwmapi", Win32DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }
}

@Suppress("PropertyName")
internal class Win32Margins : Structure() {
    @JvmField
    var cxLeftWidth: Int = 0

    @JvmField
    var cxRightWidth: Int = 0

    @JvmField
    var cyTopHeight: Int = 0

    @JvmField
    var cyBottomHeight: Int = 0

    override fun getFieldOrder(): List<String> = listOf("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight")
}

@Suppress("MemberNameEqualsClassName", "PropertyName")
internal class Win32MinMaxInfo(
    pointer: Pointer,
) : Structure(pointer) {
    @JvmField
    var ptReserved: WinDef.POINT = WinDef.POINT()

    @JvmField
    var ptMaxSize: WinDef.POINT = WinDef.POINT()

    @JvmField
    var ptMaxPosition: WinDef.POINT = WinDef.POINT()

    @JvmField
    var ptMinTrackSize: WinDef.POINT = WinDef.POINT()

    @JvmField
    var ptMaxTrackSize: WinDef.POINT = WinDef.POINT()

    override fun getFieldOrder(): List<String> =
        listOf(
            "ptReserved",
            "ptMaxSize",
            "ptMaxPosition",
            "ptMinTrackSize",
            "ptMaxTrackSize",
        )
}

@Suppress("PropertyName")
internal class Win32NcCalcSizeParams(
    pointer: Pointer,
) : Structure(pointer) {
    @JvmField
    var rgrc0: WinDef.RECT = WinDef.RECT()

    @JvmField
    var rgrc1: WinDef.RECT = WinDef.RECT()

    @JvmField
    var rgrc2: WinDef.RECT = WinDef.RECT()

    @JvmField
    var lppos: Pointer? = null

    override fun getFieldOrder(): List<String> = listOf("rgrc0", "rgrc1", "rgrc2", "lppos")
}
