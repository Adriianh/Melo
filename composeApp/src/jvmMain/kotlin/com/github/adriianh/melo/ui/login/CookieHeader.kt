package com.github.adriianh.melo.ui.login

/**
 * Shared helpers for building/parsing raw `"name=value; name2=value2"` cookie headers.
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

    private val ESSENTIAL_AUTH_COOKIES = listOf(
        "SAPISID",
        "__Secure-3PAPISID",
        "__Secure-1PAPISID",
        "APISID",
        "LOGIN_INFO",
        "__Secure-3PSID",
        "__Secure-1PSID",
        "SID",
        "HSID",
        "SSID",
        "__Secure-3PSIDTS",
        "__Secure-1PSIDTS",
        "__Secure-3PSIDCC",
        "__Secure-1PSIDCC",
        "SIDCC",
        "VISITOR_INFO1_LIVE",
        "VISITOR_PRIVACY_METADATA",
        "PREF",
        "YSC",
        "CONSISTENCY",
        "__Secure-ROLLOUT_TOKEN",
        "__Secure-YNID",
        "SOCS",
        "GPS",
    )

    /** Renders a cookie map back into `"name=value; name2=value2"` form, or `null` if empty. */
    fun Map<String, String>.toHeaderStringOrNull(): String? {
        if (isEmpty()) return null

        val selected = LinkedHashMap<String, String>()
        for (key in ESSENTIAL_AUTH_COOKIES) {
            val value = get(key)
            if (!value.isNullOrBlank()) {
                selected[key] = value
            }
        }

        for ((key, value) in this) {
            if (selected.containsKey(key)) continue
            if (key.startsWith("ST-", ignoreCase = true) || key.startsWith("_ga") || key.startsWith("_gid")) continue
            if (value.isNotBlank()) {
                selected[key] = value
            }
        }

        val result = StringBuilder()
        for ((name, value) in selected) {
            val entry = "$name=$value"
            if (result.length + entry.length + 2 > 3800 && selected.containsKey("SAPISID")) {
                break
            }
            if (result.isNotEmpty()) result.append("; ")
            result.append(entry)
        }

        return result.toString().takeIf { it.isNotEmpty() }
    }

    /** Whether this cookie map carries a complete, authenticated YouTube session. */
    fun Map<String, String>.hasValidSession(): Boolean {
        val hasAuthToken = containsKey("SAPISID") ||
                containsKey("__Secure-3PAPISID") ||
                containsKey("__Secure-1PAPISID") ||
                containsKey("APISID")
        val hasSessionId = containsKey("LOGIN_INFO") ||
                containsKey("__Secure-3PSID") ||
                containsKey("__Secure-1PSID") ||
                containsKey("SID")
        return hasAuthToken && hasSessionId
    }

    /** Whether this cookie map carries the YouTube/Google session-identity cookie. */
    fun Map<String, String>.hasSapisid(): Boolean = hasValidSession()
}