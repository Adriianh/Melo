package com.github.adriianh.cli.tui.util

/**
 * Transient confirmation messages ("toasts") shown as a floating overlay above
 * the player bar. Pure models + logic so the queue can be unit-tested without
 * a terminal; rendering lives in the SDK-independent component/ToastOverlay.
 */

/** Acknowledged kind: drives the icon + color used by the overlay. */
enum class ToastKind { INFO, SUCCESS, ERROR }

/** How far along a toast's lifecycle it is at a given instant. */
enum class ToastPhase { ENTERING, ACTIVE, LEAVING }

/** A single transient message. Timestamps allow animation to be derived from the clock. */
data class ToastMessage(
    val id: Long,
    val message: String,
    val kind: ToastKind,
    val shownAtMs: Long,
    val ttlMs: Long = TOAST_TTL_MS,
)

/** Full active lifetime of a toast, matching the Compose `MeloSnackbarHost` (3.5s). */
const val TOAST_TTL_MS = 3500L

/** Entrance animation window (fade + slide). */
const val TOAST_FADE_IN_MS = 250L

/** Exit animation window, rendered *after* the TTL and before removal. */
const val TOAST_FADE_OUT_MS = 300L

/** Heartbeat interval driving the fade/slide animation and expired-toast pruning. */
const val TOAST_TICK_MS = 200L

/** Maximum toasts rendered at once; oldest is dropped when exceeded. */
const val MAX_VISIBLE_TOASTS = 3

/**
 * Appends a new toast as the newest (last) entry, dropping the oldest when the
 * stack exceeds [MAX_VISIBLE_TOASTS]. Oldest-first ordering means the oldest
 * toast sits at the top of the stack and the newest anchored above the player bar.
 */
fun pushToast(
    toasts: List<ToastMessage>,
    message: String,
    kind: ToastKind,
    nowMs: Long,
    id: Long,
): List<ToastMessage> {
    val updated = toasts + ToastMessage(id = id, message = message, kind = kind, shownAtMs = nowMs)
    return if (updated.size > MAX_VISIBLE_TOASTS) {
        updated.drop(updated.size - MAX_VISIBLE_TOASTS)
    } else {
        updated
    }
}

/**
 * Drops toasts whose lifetime (TTL + fade-out window) has fully elapsed.
 * Keeping the fade window means the LEAVING phase is actually rendered.
 */
fun pruneExpiredToasts(toasts: List<ToastMessage>, nowMs: Long): List<ToastMessage> =
    toasts.filter { nowMs < it.shownAtMs + it.ttlMs + TOAST_FADE_OUT_MS }

/**
 * Lifecycle phase of a toast at [nowMs]. Only the newest toast animates its
 * entrance; every toast animates its exit so disappearances stay visible.
 */
fun toastPhase(toast: ToastMessage, nowMs: Long, isNewest: Boolean): ToastPhase {
    val ageMs = nowMs - toast.shownAtMs
    return when {
        ageMs >= toast.ttlMs -> ToastPhase.LEAVING
        isNewest && ageMs < TOAST_FADE_IN_MS -> ToastPhase.ENTERING
        else -> ToastPhase.ACTIVE
    }
}