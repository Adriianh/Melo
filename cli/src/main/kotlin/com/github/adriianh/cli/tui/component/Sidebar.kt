package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.*

import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import dev.tamboui.layout.Constraint
import dev.tamboui.style.Style
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

private const val NAV_ITEMS = 4
private const val UTIL_ITEMS = 3

fun buildSidebar(
    navList: ListElement<*>,
    utilList: ListElement<*>,
    sidebarInUtil: Boolean,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    if (sidebarInUtil) {
        navList.highlightStyle(Style.EMPTY).highlightSymbol("  ")
        utilList.highlightStyle(Style.EMPTY.fg(MeloTheme.PRIMARY_COLOR).bold())
            .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    } else {
        navList.highlightStyle(Style.EMPTY.fg(MeloTheme.PRIMARY_COLOR).bold())
            .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        utilList.highlightStyle(Style.EMPTY).highlightSymbol("  ")
    }

    return panel(
        dock()
            .top(navList.length(NAV_ITEMS), Constraint.length(NAV_ITEMS))
            .bottom(utilList.length(UTIL_ITEMS), Constraint.length(UTIL_ITEMS))
            .center(spacer()).fill()
    ).title("Navigation")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("sidebar-panel")
        .onKeyEvent(onKeyEvent)
}
