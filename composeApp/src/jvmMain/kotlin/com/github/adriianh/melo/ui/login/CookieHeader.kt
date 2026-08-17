package com.github.adriianh.melo.ui.login

/**
 * Shared helpers for building/parsing raw `"name=value; name2=value2"` cookie headers.
 *
 * Consolidates logic that used to be duplicated across [SessionCookieStore],
 * [BrowserAuthManager]'s Firefox reader and its Chromium reader.
 */
object CookieHeader {

    private const val SAPISID_COOKIE_NAME = "SAPISID"

    /** Parses a raw `document.cookie`-style header into an ordered name -> value map. */
    fun parse(raw: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        raw.split(";").forEach { segment ->
            val separatorIndex = segment.indexOf('=')
            if (separatorIndex <= 0) return@forEach
            val name = segment.substring(0, separatorIndex).trim()
            val value = segment.substring(separatorIndex + 1).trim()
            if (name.isNotEmpty()) result[name] = value
        }
        return result
    }

    /** Renders a cookie map back into `"name=value; name2=value2"` form, or `null` if empty. */
    fun Map<String, String>.toHeaderStringOrNull(): String? =
        takeIf { it.isNotEmpty() }?.entries?.joinToString("; ") { (name, value) -> "$name=$value" }

    /** Whether this cookie map carries the YouTube/Google session-identity cookie. */
    fun Map<String, String>.hasSapisid(): Boolean = containsKey(SAPISID_COOKIE_NAME)
}