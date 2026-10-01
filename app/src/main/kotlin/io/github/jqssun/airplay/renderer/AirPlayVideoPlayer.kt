package io.github.jqssun.airplay.renderer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.datasource.HttpDataSource

// rate is 0 while buffering; speed is the configured rate regardless of pause state
// native reads the effective rate, overlay reads playWhenReady + speed + skipSilence
data class PlaybackSnapshot(
    val position: Float,
    val duration: Float,
    val rate: Float,
    val ready: Boolean,
    val playWhenReady: Boolean,
    val speed: Float = 1f,
    val skipSilence: Boolean = false,
    val buffering: Boolean = false,
)

// exoplayer calls stay on the main thread; native only reads the onPlaybackInfo snapshot
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AirPlayVideoPlayer(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private val callbacks = CallbackGate()
    private var hasBeenReady = false
    private var pendingSurface: Surface? = null

    var onPlaybackInfo: ((PlaybackSnapshot) -> Unit)? = null
    var onVideoSize: ((width: Int, height: Int, aspect: Float) -> Unit)? = null
    var onTitle: ((String?) -> Unit)? = null
    var onEnded: (() -> Unit)? = null
    var onUnrecoverableError: (() -> Unit)? = null
    private var recoveryAttempts = 0
    private var pendingRecovery: Runnable? = null

    // Execute inline on main so a following native /stop sees the just-applied pause.
    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post { action() }
    }

    private val _reportTick = object : Runnable {
        override fun run() {
            _reportPlaybackInfo()
            mainHandler.postDelayed(this, REPORT_INTERVAL_MS)
        }
    }

    private fun listenerFor(p: ExoPlayer, token: Long) = object : Player.Listener {
        private fun current() = callbacks.isCurrent(token) && player === p
        override fun onEvents(player: Player, events: Player.Events) {
            if (current()) _reportPlaybackInfo()
        }
        override fun onPlayerError(error: PlaybackException) {
            if (!current()) return
            val behind = error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
            val status = generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.responseCode
            val decision = VideoRecoveryPolicy.decide(behind, VideoRecoveryPolicy.isRecoverable(error.errorCode, status), recoveryAttempts)
            // Never log the Throwable: source exceptions embed signed URLs.
            Log.i("AirPlaySession", "PLAYER_ERROR_${decision.name} positionMs=${p.currentPosition}")
            pendingRecovery?.let(mainHandler::removeCallbacks)
            if (decision == VideoRecoveryPolicy.Decision.GIVE_UP) {
                pendingRecovery = null
                onUnrecoverableError?.invoke()
                return
            }
            val position = p.currentPosition
            val delay = VideoRecoveryPolicy.delayMs(recoveryAttempts++)
            pendingRecovery = Runnable {
                if (!current()) return@Runnable
                pendingRecovery = null
                if (decision == VideoRecoveryPolicy.Decision.REPOSITION) p.seekToDefaultPosition()
                else p.seekTo(position)
                p.prepare() // retain playWhenReady, including pauses during the backoff
            }.also { mainHandler.postDelayed(it, delay) }
            _reportPlaybackInfo()
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (!current()) return
            if (state == Player.STATE_READY) hasBeenReady = true
            if (state == Player.STATE_ENDED) {
                if (p.isCurrentMediaItemLive) {
                    onPlayerError(PlaybackException("Live timeline ended", null, PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW))
                } else onEnded?.invoke()
            } else _reportPlaybackInfo()
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (current()) _reportPlaybackInfo()
        }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            // Publish established duration without blocking the native HTTP thread.
            if (current()) _reportPlaybackInfo()
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            if (current()) onTitle?.invoke(mediaMetadata.title?.toString())
        }
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (!current() || videoSize.height == 0) return
            val width = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt()
            onVideoSize?.invoke(width, videoSize.height, width.toFloat() / videoSize.height)
        }
    }

    fun play(location: String, startPositionSeconds: Float) = onMain {
        // recycling must not report the stopped sentinel: senders poll right after /play
        _stopInternal(reportStopped = false)
        recoveryAttempts = 0
        hasBeenReady = false
        val loadPolicy = object : DefaultLoadErrorHandlingPolicy(6) {
            override fun getRetryDelayMsFor(info: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val delay = super.getRetryDelayMsFor(info)
                return if (delay == C.TIME_UNSET) delay else delay.coerceIn(1_000L, 5_000L)
            }
        }
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setLoadErrorHandlingPolicy(loadPolicy))
            .build()
        player = p
        playerListener = listenerFor(p, callbacks.token()).also(p::addListener)
        pendingSurface?.let(p::setVideoSurface)
        onPlaybackInfo?.invoke(PlaybackSnapshot(startPositionSeconds.coerceAtLeast(0f), 0f, 0f, false, true, buffering = true))
        p.playWhenReady = true
        p.setMediaItem(MediaItem.fromUri(location), (startPositionSeconds * 1000).toLong().coerceAtLeast(0))
        p.prepare()
        _reportPlaybackInfo()
        mainHandler.postDelayed(_reportTick, REPORT_INTERVAL_MS)
    }

    fun scrub(positionSeconds: Float) = onMain {
        player?.seekTo((positionSeconds * 1000).toLong().coerceAtLeast(0))
        _reportPlaybackInfo()
    }

    // coalesces the seek spam from drag-seeking
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun setScrubbing(enabled: Boolean) = onMain {
        player?.isScrubbingModeEnabled = enabled
    }

    fun setRate(rate: Float) = onMain {
        val p = player ?: return@onMain
        if (rate <= 0f) {
            p.playWhenReady = false
        } else {
            p.playbackParameters = PlaybackParameters(rate.coerceAtLeast(0.1f))
            p.playWhenReady = true
        }
        _reportPlaybackInfo()
    }

    // local-only: the sender self-syncs from its next /playback-info poll
    fun setPlaying(playing: Boolean) = onMain {
        player?.playWhenReady = playing
        _reportPlaybackInfo()
    }

    // local speed toggle: unlike setRate it must not resume a paused player
    fun setSpeed(speed: Float) = onMain {
        player?.playbackParameters = PlaybackParameters(speed.coerceAtLeast(0.1f))
        _reportPlaybackInfo()
    }

    fun setSkipSilence(enabled: Boolean) = onMain {
        player?.skipSilenceEnabled = enabled
        _reportPlaybackInfo()
    }

    fun seekBy(deltaMs: Long) = onMain {
        val p = player ?: return@onMain
        var target = (p.currentPosition + deltaMs).coerceAtLeast(0)
        val duration = p.duration
        if (duration != C.TIME_UNSET) {
            target = target.coerceAtMost(duration)
        }
        p.seekTo(target)
        _reportPlaybackInfo()
    }

    fun setSurface(surface: Surface) = onMain {
        pendingSurface = surface
        player?.setVideoSurface(surface)
    }

    // no-op if a newer surface already replaced this one
    fun clearSurface(surface: Surface) = onMain {
        if (pendingSurface !== surface) return@onMain
        pendingSurface = null
        player?.setVideoSurface(null)
    }

    fun stop(reportStopped: Boolean = true) = onMain { _stopInternal(reportStopped) }

    private fun _stopInternal(reportStopped: Boolean) {
        pendingRecovery?.let(mainHandler::removeCallbacks)
        pendingRecovery = null
        mainHandler.removeCallbacks(_reportTick)
        callbacks.invalidate()
        val oldPlayer = player
        player = null // Invalidate before release can dispatch any final events.
        oldPlayer?.let {
            playerListener?.let(it::removeListener)
            it.release()
        }
        playerListener = null
        // duration=-1 is the "video finished" sentinel for the playback-info handler
        if (reportStopped) {
            onPlaybackInfo?.invoke(PlaybackSnapshot(0f, -1f, 0f, false, false))
        }
    }

    private fun _reportPlaybackInfo() {
        val p = player ?: return
        val durationMs = p.duration
        val position = p.currentPosition / 1000f
        // 0 = live/unknown (TIME_UNSET); a real value only exists for vod
        val duration = if (durationMs == C.TIME_UNSET) 0f else (durationMs / 1000f).coerceAtLeast(0f)
        val rate = if (p.playWhenReady && p.playbackState == Player.STATE_READY) p.playbackParameters.speed else 0f
        // Once a live timeline was playable, pause/seek/rebuffer must not erase it.
        val ready = if (p.isCurrentMediaItemLive) hasBeenReady else durationMs != C.TIME_UNSET
        onPlaybackInfo?.invoke(
            PlaybackSnapshot(
                position = position,
                duration = duration,
                rate = rate,
                ready = ready,
                playWhenReady = p.playWhenReady,
                speed = p.playbackParameters.speed,
                skipSilence = p.skipSilenceEnabled,
                buffering = p.playbackState == Player.STATE_BUFFERING || pendingRecovery != null,
            )
        )
    }

    companion object {
        private const val REPORT_INTERVAL_MS = 250L
    }
}
