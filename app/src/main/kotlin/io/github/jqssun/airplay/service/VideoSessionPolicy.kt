package io.github.jqssun.airplay.service

/** Playback owns transport lifetime, not the other way around. HOLD only marks
 * a paused session whose transport closed; it must never change play intent. */
internal object VideoSessionPolicy {
    enum class Reason {
        SENDER_STOP, PLAY_CONNECTION_CLOSED, ALL_CLIENTS_GONE, PLAYER_ENDED,
        PLAYER_ERROR_UNRECOVERABLE, LOCAL_EXIT, NEW_PLAY, SERVER_STOP,
        NATIVE_HLS_SHUTDOWN, NATIVE_HLS_EOS,
    }
    enum class Decision { END, HOLD, KEEP }

    fun decide(
        reason: Reason,
        paused: Boolean,
        localPauseAtMs: Long? = null,
        nowMs: Long = 0,
        graceMs: Long = 2_000L,
    ): Decision = when (reason) {
        Reason.SENDER_STOP -> if (VideoStopPolicy.shouldIgnoreRemoteStop(
                localPauseAtMs, nowMs, paused, graceMs,
            )) Decision.KEEP else Decision.END
        Reason.PLAY_CONNECTION_CLOSED, Reason.ALL_CLIENTS_GONE ->
            if (paused) Decision.HOLD else Decision.KEEP
        else -> Decision.END
    }
}
