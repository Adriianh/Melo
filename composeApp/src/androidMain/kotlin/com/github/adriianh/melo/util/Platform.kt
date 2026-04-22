package com.github.adriianh.melo.util

import android.os.Build

class AndroidPlatform : Platform {
    override val type: PlatformType = PlatformType.ANDROID
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
