package com.github.adriianh.core.domain.provider

/**
 * Thrown when a stream cannot be resolved because the video is age-restricted and
 * the active extraction session cannot watch it (logged out or not age-verified).
 *
 * Providers should throw this instead of returning null so upstream callers can
 * short-circuit the fallback chain and surface a clear message.
 */
class AgeRestrictedException(val sourceId: String) : Exception("Video $sourceId is age-restricted")