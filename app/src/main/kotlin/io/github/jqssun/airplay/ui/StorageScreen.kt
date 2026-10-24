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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import kotlinx.coroutines.withContext
import java.io.File
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
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) granted = hasStorageAccess(ctx) }
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

    val entries by produceState<List<Entry>?>(initialValue = null, folder, refresh) {
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
                                // the volume list and the standard top-level folders (Download, DCIM, Android ...) cannot be deleted from here
                                onDelete = if (folder == null || (e.isDir && e.file.parentFile == internal)) null else ({ toDelete = e }),
                            )
                        }
                    }
                }
            }
        }
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

@Composable
private fun EntryRow(e: Entry, modifier: Modifier, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
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
        if (onDelete != null) RowAction(stringResource(R.string.files_delete), Icons.Default.DeleteOutline) { onDelete() }
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
