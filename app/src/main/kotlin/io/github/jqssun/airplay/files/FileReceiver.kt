package io.github.jqssun.airplay.files

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import io.github.jqssun.airplay.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedInputStream
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Receives files sent from a browser on the same network (phone, tablet or computer).
 *
 * A tiny HTTP server: GET / serves the upload page, PUT /upload?name=... streams the request body straight to the
 * Downloads folder (so a multi-gigabyte movie never sits in memory). Every upload needs the PIN shown on the TV.
 * It only runs while the Files screen is open and only answers devices on the local network.
 */
object FileReceiver {
    data class Received(val name: String, val size: Long, val uri: Uri? = null, val mime: String = "application/octet-stream")
    data class Progress(val name: String, val done: Long, val total: Long)
    data class State(
        val running: Boolean = false,
        val address: String? = null,
        val port: Int = 0,
        val pin: String = "",
        val received: List<Received> = emptyList(),
        val current: Progress? = null,
        val error: String? = null,
    ) {
        /** What the QR code holds: the page, with the PIN already filled in. */
        val url: String? get() = address?.let { "http://$it:$port/#pin=$pin" }
        val shownUrl: String? get() = address?.let { "http://$it:$port" }
    }

    val state = MutableStateFlow(State())

    private const val BASE_PORT = 8765
    private const val MAX_NAME = 120
    private const val FREE_SPACE_MARGIN = 200L * 1024 * 1024
    private const val MAX_FAILS = 5
    private const val LOCK_MS = 60_000L

    private var app: Context? = null
    private var server: ServerSocket? = null
    private var pool: ExecutorService? = null
    private var pin = ""
    private val fails = ConcurrentHashMap<String, Pair<Int, Long>>()

    @Synchronized
    fun start(context: Context) {
        if (server != null) return
        val ctx = context.applicationContext
        app = ctx
        var bound: ServerSocket? = null
        for (offset in 0 until 10) {
            try {
                bound = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(BASE_PORT + offset))
                }
                break
            } catch (_: Exception) {
            }
        }
        if (bound == null) {
            state.update { it.copy(running = false, error = "port") }
            return
        }
        server = bound
        pin = "%04d".format(SecureRandom().nextInt(10_000))
        fails.clear()
        val executor = Executors.newCachedThreadPool()
        pool = executor
        state.update { it.copy(running = true, address = localAddress(), port = bound.localPort, pin = pin, current = null, error = null) }
        Thread({
            while (!bound.isClosed) {
                val client = try { bound.accept() } catch (_: Exception) { break }
                try { executor.execute { handle(client) } } catch (_: Exception) { runCatching { client.close() } }
            }
        }, "file-receiver").start()
    }

    @Synchronized
    fun stop() {
        runCatching { server?.close() }
        server = null
        runCatching { pool?.shutdownNow() }
        pool = null
        state.update { it.copy(running = false, current = null) }
    }

    /** Takes a file off the list; the file itself stays on the TV. */
    fun remove(r: Received) = state.update { it.copy(received = it.received - r) }

    /** Deletes the file from the TV's storage and takes it off the list. A file that is already gone counts as deleted. */
    fun deleteFile(ctx: Context, r: Received): Boolean {
        val uri = r.uri ?: return false
        val ok = try { ctx.contentResolver.delete(uri, null, null) >= 0 } catch (_: Exception) { false }
        if (ok) remove(r)
        return ok
    }

    /** Re-reads the address, for when the network changed while the screen was open. */
    fun refreshAddress() {
        if (server != null) state.update { it.copy(address = localAddress()) }
    }

    // ---------------------------------------------------------------- http

    private fun handle(sock: Socket) {
        try {
            sock.use {
                sock.soTimeout = 30_000
                val peer = sock.inetAddress
                if (!(peer.isSiteLocalAddress || peer.isLoopbackAddress || peer.isLinkLocalAddress)) return
                val input = BufferedInputStream(sock.getInputStream(), 64 * 1024)
                val out = sock.getOutputStream()
                val requestLine = readLine(input) ?: return
                val headers = HashMap<String, String>()
                while (true) {
                    val line = readLine(input) ?: return
                    if (line.isEmpty()) break
                    if (headers.size >= 64) return reply(out, 431, "text/plain", "Too many headers")
                    val i = line.indexOf(':')
                    if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                }
                val parts = requestLine.split(' ')
                if (parts.size < 2) return reply(out, 400, "text/plain", "Bad request")
                val method = parts[0]
                val path = parts[1].substringBefore('?')
                val query = parseQuery(parts[1].substringAfter('?', ""))
                when {
                    method == "GET" && path == "/" -> page(out)
                    method == "POST" && path == "/check" -> if (authorized(peer, headers, out)) reply(out, 200, "application/json", "{\"ok\":true}")
                    method == "PUT" && path == "/upload" -> if (authorized(peer, headers, out)) upload(input, out, headers, query["name"])
                    else -> reply(out, 404, "text/plain", "Not found")
                }
            }
        } catch (_: SocketTimeoutException) {
        } catch (_: Exception) {
        }
    }

    private fun page(out: OutputStream) {
        val ctx = app ?: return reply(out, 500, "text/plain", "Not ready")
        val name = ctx.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
            .getString(Prefs.SERVER_NAME, Prefs.DEF_SERVER_NAME) ?: Prefs.DEF_SERVER_NAME
        val html = ctx.assets.open("receive.html").bufferedReader().use { it.readText() }
            .replace("{{NAME}}", name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
        reply(out, 200, "text/html; charset=utf-8", html)
    }

    private fun authorized(peer: InetAddress, headers: Map<String, String>, out: OutputStream): Boolean {
        val key = peer.hostAddress ?: "?"
        val now = System.currentTimeMillis()
        val (count, lockedUntil) = fails[key] ?: (0 to 0L)
        if (lockedUntil > now) {
            reply(out, 429, "application/json", "{\"error\":\"locked\"}")
            return false
        }
        val given = headers["x-pin"] ?: ""
        // constant-time compare
        if (MessageDigest.isEqual(given.toByteArray(), pin.toByteArray())) {
            fails.remove(key)
            return true
        }
        val n = count + 1
        fails[key] = if (n >= MAX_FAILS) 0 to now + LOCK_MS else n to 0L
        reply(out, 403, "application/json", "{\"error\":\"pin\"}")
        return false
    }

    private fun upload(input: BufferedInputStream, out: OutputStream, headers: Map<String, String>, rawName: String?) {
        val ctx = app ?: return reply(out, 500, "text/plain", "Not ready")
        val total = headers["content-length"]?.toLongOrNull()
        if (total == null) return reply(out, 411, "application/json", "{\"error\":\"length\"}")
        if (total <= 0) return reply(out, 400, "application/json", "{\"error\":\"empty\"}")
        val name = sanitize(rawName)
        val free = runCatching { StatFs(Environment.getExternalStorageDirectory().path).availableBytes }.getOrDefault(Long.MAX_VALUE)
        if (total + FREE_SPACE_MARGIN > free) return reply(out, 507, "application/json", "{\"error\":\"space\"}")

        val sink = Sink.open(ctx, name) ?: return reply(out, 500, "application/json", "{\"error\":\"write\"}")
        var done = 0L
        var lastPublish = 0L
        var ok = false
        try {
            val buf = ByteArray(256 * 1024)
            state.update { it.copy(current = Progress(name, 0, total)) }
            while (done < total) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), total - done).toInt())
                if (n < 0) break
                sink.stream.write(buf, 0, n)
                done += n
                val now = System.currentTimeMillis()
                if (now - lastPublish > 250) {
                    lastPublish = now
                    state.update { it.copy(current = Progress(name, done, total)) }
                }
            }
            ok = done == total
        } catch (_: Exception) {
        } finally {
            runCatching { sink.stream.close() }
            if (ok) sink.finish() else sink.discard()
        }
        if (ok) {
            state.update { it.copy(current = null, received = listOf(Received(sink.name, total, sink.uri, sink.mime)) + it.received) }
            reply(out, 200, "application/json", "{\"ok\":true,\"name\":${json(sink.name)},\"size\":$total}")
        } else {
            state.update { it.copy(current = null) }
        }
    }

    private fun reply(out: OutputStream, status: Int, type: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val reason = when (status) {
            200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; 404 -> "Not Found"; 411 -> "Length Required"
            429 -> "Too Many Requests"; 431 -> "Request Header Fields Too Large"; 507 -> "Insufficient Storage"
            else -> "Error"
        }
        val head = "HTTP/1.1 $status $reason\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\n" +
            "Cache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n"
        out.write(head.toByteArray(Charsets.ISO_8859_1))
        out.write(bytes)
        out.flush()
    }

    private fun readLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().trimEnd('\r')
            if (sb.length >= 8192) return null
            sb.append(b.toChar())
        }
    }

    private fun parseQuery(q: String): Map<String, String> =
        q.split('&').filter { it.contains('=') }.associate {
            val k = it.substringBefore('=')
            val v = runCatching { URLDecoder.decode(it.substringAfter('='), "UTF-8") }.getOrDefault("")
            k to v
        }

    private fun json(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** A file name that cannot leave the folder or hide: no paths, no control characters, no leading dots. */
    internal fun sanitize(raw: String?): String {
        var n = (raw ?: "").substringAfterLast('/').substringAfterLast('\\')
        n = n.filter { it.code >= 32 && it != ':' && it != '*' && it != '?' && it != '"' && it != '<' && it != '>' && it != '|' }
        n = n.trim().trimStart('.')
        if (n.length > MAX_NAME) {
            val ext = n.substringAfterLast('.', "").take(10)
            n = n.take(MAX_NAME - ext.length - 1) + if (ext.isNotEmpty()) ".$ext" else ""
        }
        return n.ifBlank { "file-" + System.currentTimeMillis() }
    }

    private fun localAddress(): String? {
        val candidates = ArrayList<Pair<Int, String>>()
        try {
            for (nif in NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                for (a in nif.inetAddresses) {
                    if (a is Inet4Address && a.isSiteLocalAddress) {
                        val rank = if (nif.name.startsWith("wlan") || nif.name.startsWith("eth")) 0 else 1
                        candidates += rank to (a.hostAddress ?: continue)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return candidates.minByOrNull { it.first }?.second
    }

    // ---------------------------------------------------------------- storage

    /** Where an incoming file is written, and how to finish or throw it away. */
    private class Sink(
        val name: String,
        val stream: OutputStream,
        val finish: () -> Unit,
        val discard: () -> Unit,
        val uri: Uri? = null,
        val mime: String = "application/octet-stream",
    ) {
        companion object {
            fun open(ctx: Context, name: String): Sink? = try {
                if (Build.VERSION.SDK_INT >= 29) openMediaStore(ctx, name) else openFile(ctx, name)
            } catch (_: Exception) {
                null
            }

            private fun mimeOf(name: String): String {
                val ext = name.substringAfterLast('.', "").lowercase()
                return when (ext) {
                    "apk" -> "application/vnd.android.package-archive"
                    else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                }
            }

            private fun openMediaStore(ctx: Context, name: String): Sink? {
                val resolver = ctx.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeOf(name))
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AirPlay TV")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                val stream = resolver.openOutputStream(uri) ?: return null
                // the system may have renamed it to avoid a clash: report the real name
                val finalName = resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)
                    ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: name
                return Sink(
                    finalName, stream,
                    finish = {
                        val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                        resolver.update(uri, done, null, null)
                    },
                    discard = { runCatching { resolver.delete(uri, null, null) } },
                    uri = uri,
                    mime = mimeOf(name),
                )
            }

            private fun openFile(ctx: Context, name: String): Sink? {
                val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AirPlay TV").apply { mkdirs() }
                var target = File(dir, name)
                var i = 1
                val base = name.substringBeforeLast('.', name)
                val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
                while (target.exists()) target = File(dir, "$base ($i)$ext").also { i++ }
                val stream = target.outputStream()
                return Sink(target.name, stream, finish = {}, discard = { runCatching { target.delete() } })
            }
        }
    }
}
