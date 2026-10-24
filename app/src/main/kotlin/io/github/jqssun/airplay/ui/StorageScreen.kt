package io.github.jqssun.airplay.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.jqssun.airplay.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.text.DateFormat
import java.util.Date

private val RowShape = RoundedCornerShape(16.dp)

/** What one line of the list stands for. */
private data class Entry(val file: File, val isDir: Boolean, val size: Long, val modified: Long, val label: String? = null, val usb: Boolean = false)

private fun internalRoot(): File = Environment.getExternalStorageDirectory()

/** Other volumes such as USB drives; the built-in storage is listed separately. */
private fun removableVolumes(): List<File> =
    File("/storage").listFiles()?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" && it.canRead() }.orEmpty()

private fun hasStorageAccess(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

/** Folders first, then files, each by name; hidden files are left out. */
private fun list(dir: File): List<Entry>? {
    val files = dir.listFiles() ?: return null
    return files.filter { !it.name.startsWith(".") }
        .map { Entry(it, it.isDirectory, if (it.isFile) it.length() else 0L, it.lastModified()) }
        .sortedWith(compareByDescending<Entry> { it.isDir }.thenBy { it.file.name.lowercase() })
}

private fun mimeOf(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "apk" -> "application/vnd.android.package-archive"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}

/** Deletes a file or a folder with its contents, then tells the media database so it does not keep a ghost entry. */
private fun deletePath(ctx: Context, target: File): Boolean {
    val paths = ArrayList<String>()
    target.walkTopDown().filter { it.isFile }.take(5000).forEach { paths += it.absolutePath }
    paths += target.absolutePath
    val ok = if (target.isDirectory) target.deleteRecursively() else target.delete()
    MediaScannerConnection.scanFile(ctx, paths.toTypedArray(), null, null)
    return ok
}


/** A copy or move waiting for a destination folder. */
private class Clip(val file: File, val move: Boolean)

/** "name (1).ext" style name that does not exist in [dir] yet. */
private fun uniqueTarget(dir: File, name: String): File {
    var target = File(dir, name)
    if (!target.exists()) return target
    val base = name.substringBeforeLast('.', name)
    val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
    var n = 1
    while (target.exists() && n < 10_000) { target = File(dir, "$base ($n)$ext"); n++ }
    return target
}

private fun treeSize(f: File): Long = if (f.isFile) f.length() else f.walkTopDown().filter { it.isFile }.sumOf { it.length() }

/** Copies a file or folder, reporting bytes as they go; stops early when [cancel] is set. */
private fun copyTree(src: File, dst: File, cancel: AtomicBoolean, onBytes: (Long) -> Unit) {
    if (src.isDirectory) {
        dst.mkdirs()
        src.listFiles()?.forEach { if (!cancel.get()) copyTree(it, File(dst, it.name), cancel, onBytes) }
    } else {
        src.inputStream().use { input ->
            dst.outputStream().use { output ->
                val buf = ByteArray(256 * 1024)
                while (!cancel.get()) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    onBytes(n.toLong())
                }
            }
        }
    }
}

private fun pathsIn(f: File): List<String> =
    if (f.isDirectory) f.walkTopDown().filter { it.isFile }.take(5000).map { it.absolutePath }.toList() else listOf(f.absolutePath)

/** Tells the media database about changed files so it shows no ghost entries. */
private fun rescan(ctx: Context, paths: List<String>) {
    if (paths.isNotEmpty()) MediaScannerConnection.scanFile(ctx, paths.take(5000).toTypedArray(), null, null)
}

/** Copies or moves [clip] into [dir]. Returns the new file, or null if it failed or was cancelled. */
private fun paste(ctx: Context, clip: Clip, dir: File, cancel: AtomicBoolean, onProgress: (Float) -> Unit): File? {
    val src = clip.file
    val target = uniqueTarget(dir, src.name)
    val before = pathsIn(src)
    if (clip.move && src.renameTo(target)) {
        rescan(ctx, before + pathsIn(target))
        return target
    }
    val total = treeSize(src).coerceAtLeast(1L)
    var done = 0L
    try {
        copyTree(src, target, cancel) { done += it; onProgress(done.toFloat() / total) }
    } catch (_: Exception) {
        target.deleteRecursively()
        return null
    }
    if (cancel.get()) { target.deleteRecursively(); return null }
    if (clip.move) src.deleteRecursively()
    rescan(ctx, before + pathsIn(target))
    return target
}

private fun renameTo(ctx: Context, file: File, newName: String): File? {
    val target = File(file.parentFile, newName)
    val before = pathsIn(file)
    if (!file.renameTo(target)) return null
    rescan(ctx, before + pathsIn(target))
    return target
}

/**
 * A small file manager: internal storage and USB drives, folders first, driven entirely with the remote.
 * OK opens a folder or a file, Back goes up one level, the trash button (to the right of a row) deletes after asking.
 */
@Composable
fun StorageScreen(startAt: File?, onStartConsumed: () -> Unit) {
    val ctx = LocalContext.current
    val g = LocalGlass.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember { mutableStateOf(hasStorageAccess(ctx)) }
    var resumed by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { granted = hasStorageAccess(ctx); resumed++ }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!granted) {
        AccessPrompt(onGranted = { granted = hasStorageAccess(ctx) })
        return
    }

    val internal = remember { internalRoot() }
    var volumes by remember { mutableStateOf(removableVolumes()) }
    // null = the list of storage volumes
    var current by remember { mutableStateOf<File?>(startAt ?: internal) }
    var refresh by remember { mutableStateOf(0) }
    var comeFrom by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<Entry?>(null) }
    var menuFor by remember { mutableStateOf<Entry?>(null) }
    var renaming by remember { mutableStateOf<Entry?>(null) }
    var clip by remember { mutableStateOf<Clip?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val cancel = remember { AtomicBoolean(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(startAt) {
        if (startAt != null) { current = startAt; comeFrom = null; onStartConsumed() }
    }

    val folder = current
    val canGoUp = folder != null && (folder != internal || volumes.isNotEmpty())
    fun goUp() {
        val f = folder ?: return
        comeFrom = f.name
        current = when {
            f == internal -> null
            volumes.any { it == f } -> null
            else -> f.parentFile
        }
    }
    BackHandler(enabled = canGoUp) { goUp() }

    val entries by produceState<List<Entry>?>(initialValue = null, folder, refresh, resumed) {
        value = null
        value = withContext(Dispatchers.IO) {
            if (folder == null) {
                volumes = removableVolumes()
                listOf(Entry(internal, true, 0, 0, label = ctx.getString(R.string.storage_internal))) +
                    volumes.map { Entry(it, true, 0, 0, label = ctx.getString(R.string.storage_usb) + " " + it.name, usb = true) }
            } else list(folder)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(stringResource(R.string.storage_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                pathLabel(ctx, folder, internal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }

        clip?.let { c ->
            PasteBar(
                clip = c, busy = busy, progress = progress,
                canPaste = folder != null && !busy,
                onPaste = {
                    val dest = folder ?: return@PasteBar
                    val src = c.file
                    val inside = dest.canonicalPath == src.canonicalPath || dest.canonicalPath.startsWith(src.canonicalPath + "/")
                    if (inside && src.isDirectory) {
                        Toast.makeText(ctx, R.string.storage_into_itself, Toast.LENGTH_LONG).show()
                    } else if (c.move && dest.canonicalPath == src.parentFile?.canonicalPath) {
                        Toast.makeText(ctx, R.string.storage_same_folder, Toast.LENGTH_SHORT).show()
                        clip = null
                    } else {
                        busy = true; progress = 0f; cancel.set(false)
                        scope.launch {
                            val made = withContext(Dispatchers.IO) { paste(ctx, c, dest, cancel) { progress = it } }
                            busy = false
                            if (made != null) { clip = null; comeFrom = made.name; refresh++ }
                            else Toast.makeText(ctx, if (cancel.get()) R.string.storage_cancelled else R.string.storage_op_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onCancel = { if (busy) cancel.set(true) else clip = null },
            )
        }

        Column(Modifier.weight(1f).fillMaxWidth().glass().padding(vertical = 10.dp)) {
            val items = entries
            when {
                items == null -> Unit
                items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(if (folder != null && folder.listFiles() == null) R.string.storage_unreadable else R.string.storage_empty),
                        style = MaterialTheme.typography.bodyLarge, color = g.textDim,
                    )
                }
                else -> {
                    val listState = rememberLazyListState()
                    val focus = remember { FocusRequester() }
                    // after opening or leaving a folder, put the selection on the folder we came from, or the first row
                    val focusIndex = remember(items) { comeFrom?.let { n -> items.indexOfFirst { it.file.name == n }.takeIf { it >= 0 } } ?: 0 }
                    LaunchedEffect(items) {
                        listState.scrollToItem(focusIndex)
                        focus.requestFocusUntilLanded()
                    }
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp), state = listState) {
                        itemsIndexed(items, key = { _, e -> e.file.absolutePath }) { index, e ->
                            EntryRow(
                                e,
                                modifier = if (index == focusIndex) Modifier.focusRequester(focus) else Modifier,
                                onOpen = {
                                    if (e.isDir) { comeFrom = null; current = e.file }
                                    else openFile(ctx, e.file)
                                },
                                onMore = if (folder == null) null else ({ menuFor = e }),
                            )
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { e ->
        // the standard top-level folders (Download, DCIM, Android ...) can only be copied
        val protected = e.isDir && e.file.parentFile == internal
        ActionMenu(
            name = e.file.name,
            protected = protected,
            enabled = !busy,
            onCopy = { menuFor = null; clip = Clip(e.file, move = false) },
            onMove = { menuFor = null; clip = Clip(e.file, move = true) },
            onRename = { menuFor = null; renaming = e },
            onDelete = { menuFor = null; toDelete = e },
            onDismiss = { menuFor = null },
        )
    }

    renaming?.let { e ->
        TextEditDialog(
            label = stringResource(R.string.storage_rename),
            initial = e.file.name,
            range = null,
            validate = { n -> val t = n.trim(); t.isNotEmpty() && !t.contains('/') && (t == e.file.name || !File(e.file.parentFile, t).exists()) },
            onDismiss = { renaming = null },
            onConfirm = { n ->
                val made = renameTo(ctx, e.file, n.trim())
                if (made != null) { comeFrom = made.name; refresh++ }
                else Toast.makeText(ctx, R.string.storage_op_failed, Toast.LENGTH_LONG).show()
            },
        )
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.storage_delete_title, e.file.name), maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Text(
                    stringResource(if (e.isDir) R.string.storage_delete_folder else R.string.storage_delete_file),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                val cancel = remember { FocusRequester() }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val ok = deletePath(ctx, e.file)
                            toDelete = null
                            refresh++
                            Toast.makeText(ctx, if (ok) R.string.storage_deleted else R.string.storage_delete_failed, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.dpadFocus(CircleShape),
                    ) { Text(stringResource(R.string.files_delete)) }
                    TextButton(onClick = { toDelete = null }, modifier = Modifier.dpadFocus(CircleShape).focusRequester(cancel)) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
                // start on Cancel so one stray OK press deletes nothing
                LaunchedEffect(Unit) { cancel.requestFocusUntilLanded() }
            },
        )
    }
}

/** The "..." menu of a row: Copy, Move, Rename, Delete (last). */
@Composable
private fun ActionMenu(
    name: String, protected: Boolean, enabled: Boolean,
    onCopy: () -> Unit, onMove: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MenuItem(stringResource(R.string.storage_copy), Icons.Default.ContentCopy, enabled, Modifier.focusRequester(first), onCopy)
                if (!protected) {
                    MenuItem(stringResource(R.string.storage_move), Icons.Default.DriveFileMove, enabled, Modifier, onMove)
                    MenuItem(stringResource(R.string.storage_rename), Icons.Default.DriveFileRenameOutline, enabled, Modifier, onRename)
                    MenuItem(stringResource(R.string.files_delete), Icons.Default.DeleteOutline, enabled, Modifier, onDelete)
                }
            }
        },
        confirmButton = {},
    )
    LaunchedEffect(Unit) { first.requestFocusUntilLanded() }
}

@Composable
private fun MenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        modifier
            .fillMaxWidth()
            .dpadFocus(RowShape, focusScale = 1f)
            .clip(RowShape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = g.textDim)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) g.text else g.textDim)
    }
}

/** Shown while a copy or move waits for its destination: browse to the folder, then Paste here. */
@Composable
private fun PasteBar(clip: Clip, busy: Boolean, progress: Float, canPaste: Boolean, onPaste: () -> Unit, onCancel: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().glass().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    when { busy && clip.move -> R.string.storage_moving; busy -> R.string.storage_copying; clip.move -> R.string.storage_move_what; else -> R.string.storage_copy_what },
                    clip.file.name,
                ),
                style = MaterialTheme.typography.bodyLarge, color = g.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (busy) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = g.text, trackColor = g.divider)
            else Text(stringResource(R.string.storage_paste_hint), style = MaterialTheme.typography.bodySmall, color = g.textDim)
        }
        PillButton(stringResource(R.string.storage_paste), canPaste, onPaste)
        PillButton(stringResource(android.R.string.cancel), true, onCancel)
    }
}

@Composable
private fun PillButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) g.text else g.textDim,
        modifier = Modifier
            .dpadFocus(CircleShape, focusScale = 1f)
            .clip(CircleShape)
            .background(g.pill)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

@Composable
private fun EntryRow(e: Entry, modifier: Modifier, onOpen: () -> Unit, onMore: (() -> Unit)?) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier
                .weight(1f)
                .dpadFocus(RowShape, focusScale = 1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                when { e.usb -> Icons.Default.Usb; e.isDir -> Icons.Default.Folder; else -> Icons.Default.InsertDriveFile },
                contentDescription = null, tint = if (e.isDir) g.text else g.textDim, modifier = Modifier.size(30.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(e.label ?: e.file.name, style = MaterialTheme.typography.bodyLarge, color = g.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle(e), style = MaterialTheme.typography.bodySmall, color = g.textDim, maxLines = 1)
            }
            if (e.isDir) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = g.textDim)
        }
        if (onMore != null) RowAction(stringResource(R.string.storage_more), Icons.Default.MoreVert) { onMore() }
    }
}

private fun subtitle(e: Entry): String = when {
    e.label != null -> ""
    e.isDir -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(e.modified))
    else -> sizeText(e.size) + "  ·  " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(e.modified))
}

/** "Internal storage › Download › AirPlay TV" */
private fun pathLabel(ctx: Context, folder: File?, internal: File): String {
    if (folder == null) return ctx.getString(R.string.storage_volumes)
    val root = internal.absolutePath
    val path = folder.absolutePath
    val shown = if (path == root || path.startsWith("$root/")) ctx.getString(R.string.storage_internal) + path.removePrefix(root)
    else path
    return shown.split('/').filter { it.isNotEmpty() }.joinToString("  ›  ")
}

private fun openFile(ctx: Context, file: File) {
    val uri = try { FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file) } catch (_: Exception) { null }
    openContent(ctx, uri, mimeOf(file.name), file.name)
}

/** Asks for access to the storage the first time. */
@Composable
private fun AccessPrompt(onGranted: () -> Unit) {
    val ctx = LocalContext.current
    val g = LocalGlass.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onGranted() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocusUntilLanded() }
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.glass().padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(stringResource(R.string.storage_need_access), style = MaterialTheme.typography.titleMedium, color = g.text)
            Row(
                Modifier
                    .focusRequester(focus)
                    .dpadFocus(CircleShape, focusScale = 1f)
                    .clip(CircleShape)
                    .background(g.pill)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (Build.VERSION.SDK_INT >= 30) {
                            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + ctx.packageName))
                            if (i.resolveActivity(ctx.packageManager) != null) ctx.startActivity(i)
                            else Toast.makeText(ctx, R.string.storage_no_settings, Toast.LENGTH_LONG).show()
                        } else {
                            launcher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        }
                    }
                    .padding(horizontal = 30.dp, vertical = 12.dp),
            ) { Text(stringResource(R.string.storage_allow), style = MaterialTheme.typography.labelLarge, color = g.text) }
        }
    }
}
