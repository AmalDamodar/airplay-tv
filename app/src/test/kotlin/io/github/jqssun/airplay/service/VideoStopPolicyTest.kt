package io.github.jqssun.airplay.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoStopPolicyTest {

    @Test
    fun `immediate remote stop after local pause is ignored`() {
        assertTrue(
            VideoStopPolicy.shouldIgnoreRemoteStop(
                localPauseAtMs = 10_000L,
                nowMs = 10_302L,
                playerIsPaused = true,
                graceMs = 2_000L,
            )
        )
    }

    @Test
    fun `remote stop after grace period is honored`() {
        assertFalse(
            VideoStopPolicy.shouldIgnoreRemoteStop(
                localPauseAtMs = 10_000L,
                nowMs = 12_001L,
                playerIsPaused = true,
                graceMs = 2_000L,
            )
        )
    }

    @Test
    fun `remote stop without a local pause is honored`() {
        assertFalse(
            VideoStopPolicy.shouldIgnoreRemoteStop(
                localPauseAtMs = null,
                nowMs = 10_302L,
                playerIsPaused = true,
                graceMs = 2_000L,
            )
        )
    }

    @Test
    fun `remote stop while player is running is honored`() {
        assertFalse(
            VideoStopPolicy.shouldIgnoreRemoteStop(
                localPauseAtMs = 10_000L,
                nowMs = 10_302L,
                playerIsPaused = false,
                graceMs = 2_000L,
            )
        )
    }
}
