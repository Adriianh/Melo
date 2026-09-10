package com.github.adriianh.cli.command.player.util

import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.widgets.progress.MultiProgressBarWidgetMaker
import com.github.ajalt.mordant.widgets.progress.ProgressBarMakerRow
import com.github.ajalt.mordant.widgets.progress.ProgressBarWidgetMaker

object VerticalProgressBarMaker : ProgressBarWidgetMaker {
    override fun build(rows: List<ProgressBarMakerRow<*>>): Widget {
        return grid {
            val widgets = MultiProgressBarWidgetMaker.buildCells(rows)
            for ((term, desc) in widgets.flatten().windowed(2, 2)) {
                row(term, desc)
            }
        }
    }
}