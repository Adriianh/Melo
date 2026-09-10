package com.github.adriianh.cli

import com.github.adriianh.cli.command.MeloCommand
import com.github.ajalt.clikt.core.main
import java.awt.color.ColorSpace

fun main(args: Array<String>) {
    try {
        ColorSpace.getInstance(ColorSpace.CS_sRGB)
    } catch (_: Throwable) {
    }

    MeloCommand().main(args)
}