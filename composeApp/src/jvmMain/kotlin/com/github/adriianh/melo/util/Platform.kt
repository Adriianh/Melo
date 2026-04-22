package com.github.adriianh.melo.util

class JVMPlatform : Platform {
    override val type: PlatformType = PlatformType.DESKTOP
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
