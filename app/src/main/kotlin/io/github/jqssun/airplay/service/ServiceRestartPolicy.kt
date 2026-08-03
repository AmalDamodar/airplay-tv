package io.github.jqssun.airplay.service

/** Decides whether a service command starts the receiver and whether Android may restart it. */
internal object ServiceRestartPolicy {
    fun shouldStartServer(
        action: String?,
        explicitStartAction: String,
        runInBackground: Boolean,
    ): Boolean = action == explicitStartAction || (action == null && runInBackground)

    fun shouldRestartAfterKill(
        serverRunning: Boolean,
        runInBackground: Boolean,
    ): Boolean = serverRunning && runInBackground
}
