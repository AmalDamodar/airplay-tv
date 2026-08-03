package io.github.jqssun.airplay.service

import org.junit.Assert.assertEquals
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun `media URL removes credentials path query and fragment`() {
        assertEquals(
            "https://media.example:8443/<redacted>",
            LogSanitizer.mediaLocation(
                "https://user:secret@media.example:8443/private/movie.m3u8?token=secret#part",
            ),
        )
    }

    @Test
    fun `local media URL keeps only origin`() {
        assertEquals(
            "http://127.0.0.1:7000/<redacted>",
            LogSanitizer.mediaLocation("http://127.0.0.1:7000/session/secret.m3u8"),
        )
    }

    @Test
    fun `malformed or relative media location is fully redacted`() {
        assertEquals("<redacted>", LogSanitizer.mediaLocation("not a valid uri?token=secret"))
        assertEquals("<redacted>", LogSanitizer.mediaLocation("/private/movie.m3u8"))
    }
}
