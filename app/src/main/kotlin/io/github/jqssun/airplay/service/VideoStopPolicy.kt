package io.github.jqssun.airplay.service

/** Distinguishes an explicit sender stop from the stop iOS emits after a receiver-local pause. */
internal object VideoStopPolicy {
    // A /rate=0 echo may acknowledge a local pause; it must not erase or extend it.
    fun afterSenderRate(localPauseAtMs: Long?, rate: Float): Long? =
        if (rate > 0f) null else localPauseAtMs

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
