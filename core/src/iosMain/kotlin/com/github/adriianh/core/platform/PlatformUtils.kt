package com.github.adriianh.core.platform

import platform.Foundation.*
import platform.posix.*
import kotlinx.cinterop.*
import platform.CoreCrypto.*

@OptIn(ExperimentalForeignApi::class)
actual fun sha1(str: String): String {
    val data = str.encodeToByteArray()
    val hash = ByteArray(CC_SHA1_DIGEST_LENGTH)
    data.usePinned { dataPinned ->
        hash.usePinned { hashPinned ->
            CC_SHA1(dataPinned.addressOf(0), data.size.toUInt(), hashPinned.addressOf(0).reinterpret())
        }
    }
    return hash.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

actual fun defaultCountryCode(): String =
    NSLocale.currentLocale.countryCode ?: "US"

actual fun defaultLanguageTag(): String =
    NSLocale.currentLocale.localeIdentifier.replace("_", "-")

actual fun currentTimeSeconds(): Long =
    NSDate().timeIntervalSince1970.toLong()

@OptIn(ExperimentalForeignApi::class)
actual fun md5(str: String): String {
    val data = str.encodeToByteArray()
    val hash = ByteArray(CC_MD5_DIGEST_LENGTH)
    data.usePinned { dataPinned ->
        hash.usePinned { hashPinned ->
            CC_MD5(dataPinned.addressOf(0), data.size.toUInt(), hashPinned.addressOf(0).reinterpret())
        }
    }
    return hash.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
