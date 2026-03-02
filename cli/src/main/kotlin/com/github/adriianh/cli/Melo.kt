package com.github.adriianh.cli

import com.github.adriianh.cli.command.MeloCommand
import com.github.ajalt.clikt.core.main

fun main(args: Array<String>) {

    MeloCommand().main(args)
}