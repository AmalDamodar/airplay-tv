package io.github.jqssun.airplay.renderer

/** Invalidate on the owner/main thread before releasing resources. A native
 * producer may capture a token, but callbacks are checked on the owner thread. */
internal class CallbackGate {
    @Volatile private var generation = 0L
    fun token(): Long = generation
    fun invalidate() { generation++ }
    fun isCurrent(token: Long): Boolean = token == generation
}
