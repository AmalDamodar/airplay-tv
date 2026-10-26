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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.UploadFile
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

private val CardShape = RoundedCornerShape(24.dp)

/**
 * Receiving files, as part of the Home screen. On a TV it is a grid of three tiles: [mirror] (the receiver tile)
 * and the violet-to-mint "Send files" tile with the QR code on top, and a wide blue "Received" tile below with
 * what has arrived. On a phone they stack and scroll. The receiver runs only while this section is on screen and
 * Home is selected, so nothing listens on the network the rest of the time.
 */
@Composable
internal fun ReceiveSection(modifier: Modifier = Modifier, compact: Boolean = false, mirror: @Composable (Modifier) -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val st by FileReceiver.state.collectAsState()

    // Starting is safe to repeat. Stopping happens when Home is left (MainScreen) or the app is closed, not when
    // Android merely rebuilds the screen, so a transfer is not cut off by that.
    DisposableEffect(Unit) {
        FileReceiver.start(ctx)
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(5_000); FileReceiver.refreshAddress() }
    }

    if (compact) {
        Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            mirror(Modifier.fillMaxWidth().height(170.dp))
            ConnectTile(st, Modifier.fillMaxWidth().height(230.dp), compact = true)
            ReceivedTile(st, Modifier.fillMaxWidth().height(260.dp), compact = true)
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.weight(1.15f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                mirror(Modifier.weight(1f).fillMaxHeight())
                ConnectTile(st, Modifier.weight(1.4f).fillMaxHeight())
            }
            ReceivedTile(st, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** Mint tile: what to do on the phone, the address, the PIN, and the QR code that holds all of it. */
@Composable
private fun ConnectTile(st: FileReceiver.State, modifier: Modifier, compact: Boolean = false) {
    GlowTile(TileColors.Mint, modifier) {
        Row(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                IconChip(Icons.Rounded.UploadFile)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.files_tile_title), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    val url = st.url
                    if (url != null && st.running) {
                        Text(stringResource(R.string.files_scan_short), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(st.shownUrl ?: "", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                            Text(
                                st.pin, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, color = Color.White,
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.22f), CircleShape).padding(horizontal = 12.dp, vertical = 3.dp),
                            )
                        }
                    } else {
                        Text(
                            stringResource(when (st.error) { null -> R.string.files_no_network; "idle" -> R.string.files_idle; else -> R.string.files_port_busy }),
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
            val url = st.url
            if (url != null && st.running) QrCode(url, Modifier.size(if (compact) 112.dp else 136.dp))
        }
    }
}

/** Blue tile: the file arriving now (with progress), then everything received, newest first, as a row of cards. */
@Composable
private fun ReceivedTile(st: FileReceiver.State, modifier: Modifier, compact: Boolean = false) {
    val ctx = LocalContext.current
    val cur = st.current
    var toDelete by remember { mutableStateOf<FileReceiver.Received?>(null) }
    GlowTile(TileColors.Blue, modifier) {
        Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(if (compact) 110.dp else 190.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                IconChip(Icons.Rounded.Inbox)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.files_received), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    if (!compact) Text(stringResource(R.string.files_saved_in), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
            if (st.received.isEmpty() && cur == null) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.files_none), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                val listState = rememberLazyListState()
                // a new arrival goes first; without this the row keeps showing the older cards it was on
                LaunchedEffect(st.received.size, cur == null) { listState.scrollToItem(0) }
                LazyRow(
                    Modifier.weight(1f).fillMaxHeight(), state = listState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (cur != null) item(key = "current") { ProgressCard(cur) }
                    items(st.received, key = { System.identityHashCode(it) }) { r ->
                        FileCard(r, onOpen = { openReceived(ctx, r) }, onFolder = { showInFilesApp(ctx) }, onDelete = { toDelete = r })
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

@Composable
private fun ProgressCard(cur: FileReceiver.Progress) {
    Column(
        Modifier.width(230.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.16f), CardShape).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.files_receiving_now), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Text(cur.name, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(
                progress = { if (cur.total > 0) cur.done.toFloat() / cur.total else 0f },
                modifier = Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha = 0.25f),
            )
            val pct = if (cur.total > 0) (100 * cur.done / cur.total).toInt() else 0
            Text("${sizeText(cur.done)} / ${sizeText(cur.total)}  ·  $pct%", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

/** One received file: tap to open; two small buttons show it in the Files app or delete it. */
@Composable
private fun FileCard(r: FileReceiver.Received, onOpen: () -> Unit, onFolder: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier.width(200.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.16f), CardShape).padding(8.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .dpadFocus(RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(fileIcon(r), null, Modifier.size(26.dp), tint = Color.White)
            Column {
                Text(r.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(sizeText(r.size), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TileAction(stringResource(R.string.files_open_folder), Icons.Rounded.FolderOpen, onFolder)
            TileAction(stringResource(R.string.files_delete), Icons.Rounded.DeleteOutline, onDelete)
        }
    }
}

private fun fileIcon(r: FileReceiver.Received): ImageVector = when {
    r.mime.startsWith("video/") -> Icons.Rounded.Movie
    r.mime.startsWith("image/") -> Icons.Rounded.Image
    r.mime.startsWith("audio/") -> Icons.Rounded.MusicNote
    r.mime == "application/vnd.android.package-archive" || r.name.endsWith(".apk", ignoreCase = true) -> Icons.Rounded.Android
    else -> Icons.Rounded.InsertDriveFile
}

/** A small round icon button at the foot of a file card. */
@Composable
private fun TileAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .dpadFocus(CircleShape)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, Modifier.size(22.dp), tint = Color.White.copy(alpha = 0.85f))
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
    val folder = java.io.File(android.os.Environment.getExternalStorageDirectory(), "Download/AirTV Play")
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
