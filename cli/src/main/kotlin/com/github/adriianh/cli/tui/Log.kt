package com.github.adriianh.cli.tui
import java.io.File
fun logDebug(msg: String) {
    File("debug.log").appendText(msg + "\n")
}
