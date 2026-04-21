package com.github.adriianh.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual fun platformIO(): CoroutineDispatcher = Dispatchers.IO
