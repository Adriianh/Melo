package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderEntityDetailScreen(
    state: MeloState,
    entityTracksList: ListElement<*>,
    artistDashboardList: ListElement<*>,
    entityDescriptionArea: MarkupTextAreaElement,
    marqueeText: (String, Int, Int) -> String,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val detail = state.screen as? ScreenState.EntityDetail
        ?: return panel(text("Entity details not active").centered()).rounded()

    return if (detail.entity is SearchResult.Artist) {
        renderArtistDetail(detail, state, artistDashboardList, onEntityDetailKeyEvent, marqueeText)
    } else {
        renderTracksDetail(
            detail,
            state,
            entityTracksList,
            entityDescriptionArea,
            marqueeText,
            onEntityDetailKeyEvent
        )
    }
}