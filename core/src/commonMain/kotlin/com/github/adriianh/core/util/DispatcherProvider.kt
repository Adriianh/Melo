package com.github.adriianh.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object MeloDispatchers {
    val Main: CoroutineDispatcher get() = Dispatchers.Main
    val Default: CoroutineDispatcher get() = Dispatchers.Default
    val IO: CoroutineDispatcher get() = platformIO()
}

internal expect fun platformIO(): CoroutineDispatcher
