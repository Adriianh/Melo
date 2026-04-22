package com.github.adriianh.melo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import com.github.adriianh.melo.di.initKoin

fun main() {
    initKoin()
    application {
        Window(onCloseRequest = ::exitApplication, title = "Melo") {
            App()
        }
    }
}
