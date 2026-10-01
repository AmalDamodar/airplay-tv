package io.github.jqssun.airplay.service

import io.github.jqssun.airplay.service.VideoSessionPolicy.Decision.*
import io.github.jqssun.airplay.service.VideoSessionPolicy.Reason.*
import org.junit.Assert.*
import org.junit.Test

class VideoSessionPolicyTest {
    @Test fun `activity keeps paused video alive without transport connections`() {
        assertTrue(VideoSessionPolicy.hasSession(connectionCount = 0, videoActive = true))
    }
    @Test fun `activity session ends only without video or transport`() {
        assertFalse(VideoSessionPolicy.hasSession(connectionCount = 0, videoActive = false))
        assertTrue(VideoSessionPolicy.hasSession(connectionCount = 1, videoActive = false))
    }
    @Test fun `explicit sender stop ends while paused or playing`() {
        for (paused in listOf(false, true)) {
            assertEquals(END, VideoSessionPolicy.decide(SENDER_STOP, paused))
        }
    }
    @Test fun `only the narrow receiver local pause grace ignores a sender stop`() {
        assertEquals(KEEP, VideoSessionPolicy.decide(SENDER_STOP, true, 100, 2_100))
        assertEquals(END, VideoSessionPolicy.decide(SENDER_STOP, true, 100, 2_101))
        assertEquals(END, VideoSessionPolicy.decide(SENDER_STOP, false, 100, 101))
        assertEquals(END, VideoSessionPolicy.decide(SENDER_STOP, true, null, 101))
        assertEquals(END, VideoSessionPolicy.decide(SENDER_STOP, true, 100, 99))
    }
    @Test fun `transport close preserves paused video without a stop`() {
        for (reason in listOf(PLAY_CONNECTION_CLOSED, ALL_CLIENTS_GONE)) {
            assertEquals(HOLD, VideoSessionPolicy.decide(reason, true, 100, 60_000))
        }
    }
    @Test fun `transport close cannot turn buffering or playing into a pause`() {
        // Playing intent is true during buffering, even though effective native rate is zero.
        for (reason in listOf(PLAY_CONNECTION_CLOSED, ALL_CLIENTS_GONE)) {
            assertEquals(KEEP, VideoSessionPolicy.decide(reason, paused = false))
        }
    }
    @Test fun `terminal reasons are final even during local pause grace`() {
        for (reason in listOf(LOCAL_EXIT, SERVER_STOP, PLAYER_ENDED, NEW_PLAY,
            PLAYER_ERROR_UNRECOVERABLE, NATIVE_HLS_EOS, NATIVE_HLS_SHUTDOWN)) {
            assertEquals(reason.name, END, VideoSessionPolicy.decide(reason, true, 100, 101))
        }
    }
    @Test fun `transport churn does not grant a sender stop exemption`() {
        assertEquals(HOLD, VideoSessionPolicy.decide(PLAY_CONNECTION_CLOSED, true))
        assertEquals(HOLD, VideoSessionPolicy.decide(ALL_CLIENTS_GONE, true))
        assertEquals(END, VideoSessionPolicy.decide(SENDER_STOP, true))
    }
}
