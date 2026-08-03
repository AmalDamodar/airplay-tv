package io.github.jqssun.airplay.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceRestartPolicyTest {

    @Test
    fun `explicit start action starts server when background mode is disabled`() {
        assertTrue(
            ServiceRestartPolicy.shouldStartServer(
                action = "start",
                explicitStartAction = "start",
                runInBackground = false,
            )
        )
    }

    @Test
    fun `null restart starts server when background mode is enabled`() {
        assertTrue(
            ServiceRestartPolicy.shouldStartServer(
                action = null,
                explicitStartAction = "start",
                runInBackground = true,
            )
        )
    }

    @Test
    fun `null restart does not start server when background mode is disabled`() {
        assertFalse(
            ServiceRestartPolicy.shouldStartServer(
                action = null,
                explicitStartAction = "start",
                runInBackground = false,
            )
        )
    }

    @Test
    fun `unrelated action does not start server`() {
        assertFalse(
            ServiceRestartPolicy.shouldStartServer(
                action = "unrelated",
                explicitStartAction = "start",
                runInBackground = true,
            )
        )
    }

    @Test
    fun `running background server requests sticky restart`() {
        assertTrue(
            ServiceRestartPolicy.shouldRestartAfterKill(
                serverRunning = true,
                runInBackground = true,
            )
        )
    }

    @Test
    fun `foreground-only server does not request sticky restart`() {
        assertFalse(
            ServiceRestartPolicy.shouldRestartAfterKill(
                serverRunning = true,
                runInBackground = false,
            )
        )
    }
}
