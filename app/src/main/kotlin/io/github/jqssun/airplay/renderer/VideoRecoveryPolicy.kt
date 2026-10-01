package io.github.jqssun.airplay.renderer

import androidx.media3.common.PlaybackException

/** The budget includes live-window repositioning; a broken live feed must not loop forever. */
internal object VideoRecoveryPolicy {
    const val MAX_ATTEMPTS = 3
    // TYPE_SOURCE also includes permanent manifest, permission and DRM failures.
    fun isRecoverable(errorCode: Int, httpStatus: Int?): Boolean = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> true
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            httpStatus == 408 || httpStatus == 429 || httpStatus in 500..599
        else -> false
    }
    enum class Decision { REPOSITION, RETRY, GIVE_UP }
    fun decide(behindLiveWindow: Boolean, recoverable: Boolean, attempts: Int): Decision = when {
        attempts >= MAX_ATTEMPTS -> Decision.GIVE_UP
        behindLiveWindow -> Decision.REPOSITION
        recoverable -> Decision.RETRY
        else -> Decision.GIVE_UP
    }
    fun delayMs(attempt: Int): Long = 1_000L shl attempt.coerceIn(0, MAX_ATTEMPTS - 1)
}
