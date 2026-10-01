package io.github.jqssun.airplay.renderer

import org.junit.Assert.*
import org.junit.Test

class CallbackGateTest {
    @Test fun `replacement and stop cancel callbacks and delayed retries`() {
        val gate = CallbackGate()
        val first = gate.token()
        assertTrue(gate.isCurrent(first))
        gate.invalidate() // replacement, even for the same URL
        val second = gate.token()
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
        gate.invalidate() // explicit local/server stop
        assertFalse(gate.isCurrent(first))
        assertFalse(gate.isCurrent(second))
    }
    @Test fun `queued work from stopped server cannot mutate a restarted server`() {
        val gate = CallbackGate()
        var state = "new session"
        val oldServer = gate.token()
        val queuedStop = { if (gate.isCurrent(oldServer)) state = "stopped" }
        gate.invalidate() // stop
        val duringStop = gate.token()
        gate.invalidate() // restart before the old queue drains
        queuedStop()
        assertEquals("new session", state)
        assertFalse(gate.isCurrent(duringStop))
    }
}
