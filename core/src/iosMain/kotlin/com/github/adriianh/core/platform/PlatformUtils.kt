package com.github.adriianh.core.platform

import com.github.adriianh.core.domain.model.update.UpdatePlatform
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA1
import platform.CoreCrypto.CC_SHA1_DIGEST_LENGTH
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.timeIntervalSince1970

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

actual fun currentUpdatePlatform(): UpdatePlatform =
    UpdatePlatform.UNKNOWN