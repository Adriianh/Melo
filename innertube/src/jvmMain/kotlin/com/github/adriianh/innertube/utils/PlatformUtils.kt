package com.github.adriianh.innertube.utils

import java.security.MessageDigest
import java.util.Locale

actual fun sha1(str: String): String =
    MessageDigest.getInstance("SHA-1")
        .digest(str.toByteArray())
        .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

actual fun defaultCountryCode(): String = Locale.getDefault().country

actual fun defaultLanguageTag(): String = Locale.getDefault().toLanguageTag()

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000
