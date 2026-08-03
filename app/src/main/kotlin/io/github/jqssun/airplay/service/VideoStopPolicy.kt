package io.github.jqssun.airplay.service

/** Distinguishes an explicit sender stop from the stop iOS emits after a receiver-local pause. */
internal object VideoStopPolicy {
    fun shouldIgnoreRemoteStop(
        localPauseAtMs: Long?,
        nowMs: Long,
        playerIsPaused: Boolean,
        graceMs: Long,
    ): Boolean {
        if (!playerIsPaused || localPauseAtMs == null) return false
        val elapsedMs = nowMs - localPauseAtMs
        return elapsedMs in 0..graceMs
    }
}
