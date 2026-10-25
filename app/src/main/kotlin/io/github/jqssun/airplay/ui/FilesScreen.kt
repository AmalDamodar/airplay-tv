package io.github.jqssun.airplay.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.shape.CircleShape
import io.github.jqssun.airplay.ui.requestFocusUntilLanded
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.files.FileReceiver
import kotlinx.coroutines.delay

private val RowShape = RoundedCornerShape(16.dp)

/**
 * Receive files from a phone or computer. The receiver runs only while this screen is open, so nothing listens on
 * the network the rest of the time. Left: how to connect. Right: what has arrived, each file can be opened.
 */
@Composable
fun FilesScreen() {
    val ctx = LocalContext.current
    val view = LocalView.current
    val st by FileReceiver.state.collectAsState()

    // Starting is safe to repeat. Stopping happens when the tab is left (MainScreen) or the app is closed, not when
    // Android merely rebuilds the screen, so a transfer is not cut off by that.
    DisposableEffect(Unit) {
        FileReceiver.start(ctx)
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(5_000); FileReceiver.refreshAddress() }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val wide = maxWidth >= 640.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(stringResource(R.string.files_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.files_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConnectPanel(st, Modifier.width(330.dp).fillMaxHeight())
                    ReceivedPanel(st, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConnectPanel(st, Modifier.fillMaxWidth())
                    ReceivedPanel(st, Modifier.fillMaxWidth().height(360.dp))
                }
            }
        }
    }
}

/** QR code, address and PIN. */
@Composable
private fun ConnectPanel(st: FileReceiver.State, modifier: Modifier) {
    val g = LocalGlass.current
    Column(
        modifier.glass().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val url = st.url
        if (url != null && st.running) {
            QrCode(url, Modifier.size(150.dp))
            Text(
                stringResource(R.string.files_scan),
                style = MaterialTheme.typography.bodySmall, color = g.textDim, textAlign = TextAlign.Center,
            )
            Text(st.shownUrl ?: "", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = g.text, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.files_pin), style = MaterialTheme.typography.titleSmall, color = g.textDim)
                Text(
                    st.pin,
                    fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 5.sp, color = g.text,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(g.pill).padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
            Text(
                stringResource(R.string.files_pin_hint) + " " + stringResource(R.string.files_saved_in),
                style = MaterialTheme.typography.bodySmall, color = g.textDim, textAlign = TextAlign.Center,
            )
        } else {
            Text(
                stringResource(when (st.error) { null -> R.string.files_no_network; "idle" -> R.string.files_idle; else -> R.string.files_port_busy }),
                style = MaterialTheme.typography.titleMedium, color = g.textDim, textAlign = TextAlign.Center,
            )
        }
    }
}

/** The file arriving now (with progress) on top, then everything received, newest first. */
@Composable
private fun ReceivedPanel(st: FileReceiver.State, modifier: Modifier) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val cur = st.current
    var toDelete by remember { mutableStateOf<FileReceiver.Received?>(null) }
    Column(modifier.glass().padding(vertical = 14.dp)) {
        Text(
            stringResource(R.string.files_received),
            style = MaterialTheme.typography.titleMedium, color = g.textDim,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 6.dp),
        )
        if (cur != null) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(cur.name, style = MaterialTheme.typography.bodyLarge, color = g.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { if (cur.total > 0) cur.done.toFloat() / cur.total else 0f },
                    modifier = Modifier.fillMaxWidth(), color = g.text, trackColor = g.divider,
                )
                val pct = if (cur.total > 0) (100 * cur.done / cur.total).toInt() else 0
                Text(
                    "${sizeText(cur.done)} / ${sizeText(cur.total)}  ·  $pct%",
                    style = MaterialTheme.typography.bodySmall, color = g.textDim,
                )
            }
        }
        if (st.received.isEmpty() && cur == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.files_none), style = MaterialTheme.typography.bodyMedium, color = g.textDim)
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(st.received, key = { System.identityHashCode(it) }) { r ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.bodyLarge, color = g.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sizeText(r.size), style = MaterialTheme.typography.bodySmall, color = g.textDim)
                        }
                        RowAction(stringResource(R.string.files_open), Icons.AutoMirrored.Filled.OpenInNew, showLabel = true) { openReceived(ctx, r) }
                        RowAction(stringResource(R.string.files_open_folder), Icons.Default.FolderOpen) { showInFilesApp(ctx) }
                        RowAction(stringResource(R.string.files_delete), Icons.Default.DeleteOutline) { toDelete = r }
                    }
                }
            }
        }
    }
    toDelete?.let { r ->
        DeleteDialog(
            name = r.name,
            onDeleteFile = {
                toDelete = null
                val ok = FileReceiver.deleteFile(ctx, r)
                Toast.makeText(ctx, if (ok) R.string.files_deleted else R.string.files_delete_failed, Toast.LENGTH_SHORT).show()
            },
            onRemoveFromList = { toDelete = null; FileReceiver.remove(r) },
            onCancel = { toDelete = null },
        )
    }
}

/** One selectable action at the end of a row: a round icon button, or a pill with a label. */
@Composable
internal fun RowAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, showLabel: Boolean = false, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier
            .dpadFocus(CircleShape, focusScale = 1f)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .padding(horizontal = if (showLabel) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (showLabel) Text(label, style = MaterialTheme.typography.labelLarge, color = g.textDim)
        Icon(icon, contentDescription = label, tint = g.textDim)
    }
}

/** Two ways to get rid of a file: delete it from the TV, or only take it off the list. */
@Composable
private fun DeleteDialog(name: String, onDeleteFile: () -> Unit, onRemoveFromList: () -> Unit, onCancel: () -> Unit) {
    val keep = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.files_delete_title, name), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = { Text(stringResource(R.string.files_delete_text), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDeleteFile, modifier = Modifier.dpadFocus(CircleShape)) { Text(stringResource(R.string.files_delete_file)) }
                TextButton(onClick = onRemoveFromList, modifier = Modifier.dpadFocus(CircleShape)) { Text(stringResource(R.string.files_delete_list)) }
                TextButton(onClick = onCancel, modifier = Modifier.dpadFocus(CircleShape).focusRequester(keep)) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
    // start on Cancel so one stray OK press deletes nothing
    LaunchedEffect(Unit) { keep.requestFocusUntilLanded() }
}

private const val FILES_PACKAGE = "com.minimaltv.files"

/** Opens the Files app (a separate app from the MinimalTV setup) at the folder the received files are saved in. */
private fun showInFilesApp(ctx: Context) {
    val folder = java.io.File(android.os.Environment.getExternalStorageDirectory(), "Download/AirPlay TV")
    val intent = Intent().setClassName(FILES_PACKAGE, "$FILES_PACKAGE.MainActivity")
        .putExtra("$FILES_PACKAGE.PATH", folder.absolutePath)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(ctx, R.string.files_app_missing, Toast.LENGTH_LONG).show()
    }
}

// the TV's built-in stub only says "no app"; it never counts as a handler
internal const val STUB_PACKAGE = "com.google.android.tv.frameworkpackagestubs"

/** Opens a received file with whatever app on the TV can show it (an APK goes to the system installer). */
private fun openReceived(ctx: Context, r: FileReceiver.Received) = openContent(ctx, r.uri, r.mime, r.name)

/** Hands a file to an app that can show it; says so if there is none. */
internal fun openContent(ctx: Context, uri: android.net.Uri?, mime: String, name: String) {
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        clipData = uri?.let { ClipData.newRawUri("", it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val handlers = ctx.packageManager.queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY)
        .filter { it.activityInfo.packageName != STUB_PACKAGE }
    if (uri == null || handlers.isEmpty()) {
        Toast.makeText(ctx, R.string.files_cannot_open, Toast.LENGTH_LONG).show()
        return
    }
    try {
        if (handlers.size == 1) ctx.startActivity(view)
        else ctx.startActivity(Intent.createChooser(view, name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(ctx, R.string.files_cannot_open, Toast.LENGTH_LONG).show()
    }
}

internal fun sizeText(bytes: Long): String {
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
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)) {
        val m = matrix ?: return@Canvas
        val pad = size.minDimension * 0.07f
        val cell = (size.minDimension - 2 * pad) / m.width
        for (y in 0 until m.height) for (x in 0 until m.width) {
            if (m[x, y]) drawRect(Color.Black, Offset(pad + x * cell, pad + y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
