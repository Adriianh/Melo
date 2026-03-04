package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.tui.event.KeyEvent

fun buildSidebar(
    sidebarList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> dev.tamboui.toolkit.event.EventResult,
): Element = panel(
    column(sidebarList.fill())
).title("Navigation")
    .rounded()
    .borderColor(BORDER_DEFAULT)
    .focusedBorderColor(BORDER_FOCUSED)
    .focusable()
    .id("sidebar-panel")
    .onKeyEvent(onKeyEvent)

