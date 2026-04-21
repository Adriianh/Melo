package com.github.adriianh.core.platform

import java.security.MessageDigest
import java.util.Locale

actual fun sha1(str: String): String =
    MessageDigest.getInstance("SHA-1")
        .digest(str.toByteArray())
        .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

actual fun defaultCountryCode(): String = Locale.getDefault().country

actual fun defaultLanguageTag(): String = Locale.getDefault().toLanguageTag()

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

actual fun md5(str: String): String =
    MessageDigest.getInstance("MD5")
        .digest(str.toByteArray())
        .joinToString("") { "%02x".format(it) }
