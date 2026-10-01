package io.github.jqssun.airplay.renderer

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoRecoveryPolicyTest {
    @Test fun `only transient network failures retry not all source failures`() {
        fun recoverable(code: Int, status: Int? = null) = VideoRecoveryPolicy.isRecoverable(code, status)
        assertEquals(true, recoverable(2001)) // IO_NETWORK_CONNECTION_FAILED
        assertEquals(true, recoverable(2002)) // IO_NETWORK_CONNECTION_TIMEOUT
        assertEquals(true, recoverable(2004, 503))
        assertEquals(true, recoverable(2004, 429))
        assertEquals(false, recoverable(2004, 401))
        assertEquals(false, recoverable(2004, 403))
        assertEquals(false, recoverable(2004, 404))
        assertEquals(false, recoverable(3002)) // PARSING_MANIFEST_MALFORMED
        assertEquals(false, recoverable(4001)) // DECODER_INIT_FAILED
        assertEquals(false, recoverable(2005)) // IO_FILE_NOT_FOUND
        assertEquals(false, recoverable(6002)) // DRM_SCHEME_UNSUPPORTED
    }

    private fun decide(behindLiveWindow: Boolean, recoverable: Boolean, attempts: Int): String {
        return VideoRecoveryPolicy.decide(behindLiveWindow, recoverable, attempts).name
    }
    @Test fun `behind live window repositions`() { assertEquals("REPOSITION", decide(true, true, 0)) }
    @Test fun `transient source error retries`() { assertEquals("RETRY", decide(false, true, 0)) }
    @Test fun `retry budget is bounded`() {
        assertEquals("RETRY", decide(false, true, 2))
        assertEquals("GIVE_UP", decide(false, true, 3))
    }
    @Test fun `repeated live window error also exhausts budget`() { assertEquals("GIVE_UP", decide(true, true, 3)) }
    @Test fun `backoff is bounded`() {
        assertEquals(1_000L, VideoRecoveryPolicy.delayMs(0))
        assertEquals(2_000L, VideoRecoveryPolicy.delayMs(1))
        assertEquals(4_000L, VideoRecoveryPolicy.delayMs(2))
        assertEquals(4_000L, VideoRecoveryPolicy.delayMs(100))
    }
    @Test fun `decoder or fatal error gives up immediately`() { assertEquals("GIVE_UP", decide(false, false, 0)) }
}
