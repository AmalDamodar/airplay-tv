package io.github.jqssun.airplay.service

import java.net.URI

/** Removes credentials, paths, queries, and fragments before a media location enters logs. */
internal object LogSanitizer {
    fun mediaLocation(raw: String): String = try {
        val uri = URI(raw)
        val scheme = uri.scheme?.lowercase() ?: return "<redacted>"
        val host = uri.host ?: return "<redacted>"
        val displayHost = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
        val port = if (uri.port >= 0) ":${uri.port}" else ""
        "$scheme://$displayHost$port/<redacted>"
    } catch (_: Exception) {
        "<redacted>"
    }
}
