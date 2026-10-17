package com.github.adriianh.core.platform

import android.content.Context

/**
 * Holds the application [Context] for core-level platform code that has no DI access.
 * Initialised once from the Application's `onCreate`.
 */
object AndroidAppContext {
    @Volatile
    var application: Context? = null
}
