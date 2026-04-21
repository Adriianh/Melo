package com.github.adriianh.innertube.utils

/**
 * Platform-agnostic cryptographic and locale utilities.
 */

/**
 * Computes the SHA-1 hash of the given string and returns it as a hex string.
 */
expect fun sha1(str: String): String

/**
 * Returns the default country code for the current platform (e.g. "US").
 */
expect fun defaultCountryCode(): String

/**
 * Returns the default language tag for the current platform (e.g. "en-US").
 */
expect fun defaultLanguageTag(): String

/**
 * Returns the current time in seconds since epoch.
 */
expect fun currentTimeSeconds(): Long
