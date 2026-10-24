package io.github.jqssun.airplay.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.files.FileReceiver
import kotlinx.coroutines.delay

/**
 * Receive files from a phone or computer. The receiver runs only while this screen is open, so nothing listens on
 * the network the rest of the time.
 */
@Composable
fun FilesScreen() {
    val ctx = LocalContext.current
    val view = LocalView.current
    val st by FileReceiver.state.collectAsState()
    val g = LocalGlass.current

    DisposableEffect(Unit) {
        FileReceiver.start(ctx)
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            FileReceiver.stop()
        }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(5_000); FileReceiver.refreshAddress() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(stringResource(R.string.files_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.files_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            Modifier.fillMaxWidth().glass().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val url = st.url
            if (url != null && st.running) {
                QrCode(url, Modifier.size(200.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.files_scan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = g.textDim,
                    )
                    Text(
                        st.shownUrl ?: "",
                        fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = g.text,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.files_pin), style = MaterialTheme.typography.titleMedium, color = g.textDim)
                        Text(
                            st.pin,
                            fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 6.sp, color = g.text,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(g.pill).padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.files_pin_hint) + ". " + stringResource(R.string.files_saved_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = g.textDim,
                    )
                }
            } else {
                Text(
                    stringResource(if (st.error != null) R.string.files_port_busy else R.string.files_no_network),
                    style = MaterialTheme.typography.titleMedium,
                    color = g.textDim,
                )
            }
        }

        Column(Modifier.fillMaxWidth().glass().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.files_received), style = MaterialTheme.typography.titleMedium, color = g.textDim)
            val cur = st.current
            if (cur != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.files_receiving, cur.name), style = MaterialTheme.typography.bodyLarge, color = g.text)
                    LinearProgressIndicator(
                        progress = { if (cur.total > 0) cur.done.toFloat() / cur.total else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = g.text,
                        trackColor = g.divider,
                    )
                    Text("${sizeText(cur.done)} / ${sizeText(cur.total)}", style = MaterialTheme.typography.bodySmall, color = g.textDim)
                }
            }
            if (st.received.isEmpty() && cur == null) {
                Text(stringResource(R.string.files_none), style = MaterialTheme.typography.bodyMedium, color = g.textDim)
            }
            st.received.take(20).forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(it.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = g.text, maxLines = 1)
                    Text(sizeText(it.size), style = MaterialTheme.typography.bodyMedium, color = g.textDim)
                }
            }
        }
    }
}

private fun sizeText(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    do { v /= 1024; i++ } while (v >= 1024 && i < units.size - 1)
    return if (v >= 100 || i == 0) "%.0f %s".format(v, units[i]) else "%.1f %s".format(v, units[i])
}

/** Black modules on a white rounded tile, so any phone camera can read it in light or dark theme. */
@Composable
private fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix: BitMatrix? = remember(text) {
        runCatching {
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
        }.getOrNull()
    }
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)) {
        val m = matrix ?: return@Canvas
        val pad = size.minDimension * 0.07f
        val cell = (size.minDimension - 2 * pad) / m.width
        for (y in 0 until m.height) for (x in 0 until m.width) {
            if (m[x, y]) drawRect(Color.Black, Offset(pad + x * cell, pad + y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
