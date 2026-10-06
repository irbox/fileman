package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ClipboardOp
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.ui.theme.*
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

@Composable
fun getCategoryIconAndColor(category: FileCategory): Pair<ImageVector, Color> {
    return when (category) {
        FileCategory.ALL -> Icons.Default.Folder to LibreCyan
        FileCategory.IMAGES -> Icons.Default.Image to ColorImage
        FileCategory.VIDEOS -> Icons.Default.VideoLibrary to ColorVideo
        FileCategory.AUDIO -> Icons.Default.MusicNote to ColorAudio
        FileCategory.DOCUMENTS -> Icons.Default.Description to ColorDoc
        FileCategory.DOWNLOADS -> Icons.Default.Download to LibreCyanLight
        FileCategory.ARCHIVES -> Icons.Default.Archive to ColorArchive
        FileCategory.APKS -> Icons.Default.Android to ColorApk
        FileCategory.TRASH -> Icons.Default.DeleteSweep to ColorTrash
        FileCategory.VAULT -> Icons.Default.Lock to ColorVault
    }
}

@Composable
fun getFileIconAndColor(item: FileItem): Pair<ImageVector, Color> {
    if (item.isDirectory) {
        return Icons.Default.Folder to LibreCyan
    }
    return when (item.extension.lowercase()) {
        "jpg", "jpeg", "png", "webp", "gif", "svg" -> Icons.Default.Image to ColorImage
        "mp4", "mkv", "mov", "avi", "webm" -> Icons.Default.VideoLibrary to ColorVideo
        "mp3", "flac", "wav", "m4a", "ogg" -> Icons.Default.Audiotrack to ColorAudio
        "pdf" -> Icons.Default.PictureAsPdf to ColorDoc
        "doc", "docx", "txt", "md", "csv", "json", "xml", "kt" -> Icons.Default.Description to ColorDoc
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Archive to ColorArchive
        "apk" -> Icons.Default.Android to ColorApk
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun BreadcrumbBar(
    breadcrumbs: List<Pair<String, String>>,
    onBreadcrumbClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(breadcrumbs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, pair ->
            val isLast = index == breadcrumbs.lastIndex
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLast) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isLast) { onBreadcrumbClick(pair.second) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (index == 0) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = pair.first,
                        fontSize = 13.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StorageProgressBar(
    usedPercentage: Float,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = usedPercentage.coerceIn(0.01f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(primaryColor)
        )
    }
}

// Checksum & Details Dialog
@Composable
fun FileDetailsDialog(
    item: FileItem,
    checksums: Map<String, String>,
    onDismiss: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(item.lastModified))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, color) = getFileIconAndColor(item)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Type", if (item.isDirectory) "Directory" else item.mimeType)
                DetailRow("Size", item.formattedSize)
                DetailRow("Location", item.path)
                DetailRow("Modified", dateStr)
                if (item.isDirectory) {
                    DetailRow("Contains", "${item.itemCount} items")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Security Checksums", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                if (checksums.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculating MD5, SHA-1, SHA-256...", fontSize = 12.sp)
                    }
                } else {
                    checksums.forEach { (algo, hash) ->
                        Column {
                            Text(algo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(hash, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("details_close_button")) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// Next-Gen Text & Code Editor with Line Numbers and Statistics
@Composable
fun TextEditorDialog(
    fileName: String,
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialContent) }

    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    val charCount = remember(text) { text.length }
    val wordCount = remember(text) {
        if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                            Text(
                                text = "$lineCount lines • $wordCount words • $charCount chars",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("text_editor_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    placeholder = { Text("Write content here...") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(text) },
                        modifier = Modifier.testTag("save_text_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save File")
                    }
                }
            }
        }
    }
}

// Built-in Fullscreen Image Previewer
@Composable
fun ImageViewerDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.file ?: item.uri ?: item.path,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.formattedSize,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = item.mimeType,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// Floating Bottom Audio Player Bar
@Composable
fun AudioPlayerBar(
    item: FileItem,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Local Audio Player • ${item.formattedSize}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ZIP Archive Inspector Dialog
@Composable
fun ZipInspectorDialog(
    item: FileItem,
    onExtract: () -> Unit,
    onDismiss: () -> Unit
) {
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(item.path) {
        val file = item.file ?: File(item.path)
        if (file.exists()) {
            val list = mutableListOf<String>()
            try {
                ZipInputStream(FileInputStream(file)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && list.size < 40) {
                        list.add(entry.name)
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            entries = list
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = LibreCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Archive Contents (${item.formattedSize}):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        entries.forEach { entryName ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (entryName.endsWith("/")) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(entryName, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onExtract()
                onDismiss()
            }) {
                Text("Extract Here")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// Persistent Clipboard Floating Bar
@Composable
fun PasteClipboardBar(
    items: List<FileItem>,
    operation: ClipboardOp,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (operation == ClipboardOp.COPY) Icons.Default.ContentCopy else Icons.Default.ContentCut,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (operation == ClipboardOp.COPY) "Copying" else "Moving"} ${items.size} items",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Navigate to destination folder and paste",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onCancel) {
                Text("Cancel", fontSize = 12.sp)
            }
            Button(
                onClick = onPaste,
                modifier = Modifier.testTag("paste_clipboard_button")
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste", fontSize = 12.sp)
            }
        }
    }
}

// Batch Rename Dialog
@Composable
fun BatchRenameDialog(
    selectedCount: Int,
    onConfirm: (prefix: String, suffix: String, find: String, replace: String, numbering: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var numbering by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Batch Rename ($selectedCount items)")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Prefix (e.g. Doc_)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_prefix_input")
                )
                OutlinedTextField(
                    value = suffix,
                    onValueChange = { suffix = it },
                    label = { Text("Suffix (e.g. _v1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        label = { Text("Find") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text("Replace") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { numbering = !numbering }
                ) {
                    Checkbox(checked = numbering, onCheckedChange = { numbering = it })
                    Text("Add sequential numbers (_001, _002)", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(prefix, suffix, findText, replaceText, numbering) },
                modifier = Modifier.testTag("confirm_batch_rename")
            ) {
                Text("Rename All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Create New Item Dialog
@Composable
fun CreateItemDialog(
    isFolder: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isFolder) "Create New Directory" else "Create New File")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isFolder) "Folder Name" else "File Name (e.g. notes.txt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_item_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_item")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ZIP Compression Dialog
@Composable
fun CreateZipDialog(
    selectedCount: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var zipName by remember { mutableStateOf("Archive_${System.currentTimeMillis() % 10000}.zip") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress to ZIP") },
        text = {
            Column {
                Text("Compressing $selectedCount items into archive.", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("zip_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (zipName.isNotBlank()) onConfirm(zipName.trim()) },
                modifier = Modifier.testTag("confirm_zip_button")
            ) {
                Text("Compress")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Multi-Select Contextual Bottom Action Bar
@Composable
fun SelectionBottomBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("selection_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share
            TextButton(
                onClick = onShare,
                modifier = Modifier.testTag("selection_share_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Share", fontSize = 11.sp)
                }
            }

            // Move (Cut to clipboard)
            TextButton(
                onClick = onMove,
                modifier = Modifier.testTag("selection_move_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = "Move",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Move", fontSize = 11.sp)
                }
            }

            // Copy
            TextButton(
                onClick = onCopy,
                modifier = Modifier.testTag("selection_copy_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Copy", fontSize = 11.sp)
                }
            }

            // Delete
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("selection_delete_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Delete", fontSize = 11.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Sort & Hidden Files Bottom Sheet Menu
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionsBottomSheet(
    currentSortOption: com.example.data.model.SortOption,
    showHiddenFiles: Boolean,
    onSortSelected: (com.example.data.model.SortField, com.example.data.model.SortDirection) -> Unit,
    onToggleShowHidden: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .testTag("sort_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort & View Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SORT BY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sort Options List
            val sortConfigs = listOf(
                Triple("Name (A to Z)", com.example.data.model.SortField.NAME, com.example.data.model.SortDirection.ASCENDING),
                Triple("Name (Z to A)", com.example.data.model.SortField.NAME, com.example.data.model.SortDirection.DESCENDING),
                Triple("Size (Largest first)", com.example.data.model.SortField.SIZE, com.example.data.model.SortDirection.DESCENDING),
                Triple("Size (Smallest first)", com.example.data.model.SortField.SIZE, com.example.data.model.SortDirection.ASCENDING),
                Triple("Last Modified (Newest first)", com.example.data.model.SortField.DATE, com.example.data.model.SortDirection.DESCENDING),
                Triple("Last Modified (Oldest first)", com.example.data.model.SortField.DATE, com.example.data.model.SortDirection.ASCENDING)
            )

            sortConfigs.forEach { (label, field, direction) ->
                val isSelected = currentSortOption.field == field && currentSortOption.direction == direction
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSortSelected(field, direction)
                            onDismiss()
                        }
                        .testTag("sort_option_${field.name}_${direction.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSortSelected(field, direction)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SYSTEM VISIBILITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleShowHidden() }
                    .testTag("toggle_hidden_files_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (showHiddenFiles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (showHiddenFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Show hidden files",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Files and folders beginning with a dot (\".\")",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = showHiddenFiles,
                        onCheckedChange = { onToggleShowHidden() },
                        modifier = Modifier.testTag("show_hidden_files_switch")
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// File Metadata & Preview Bottom Sheet Modal
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileMetadataBottomSheet(
    item: FileItem,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onCompress: (String) -> Unit = {}
) {
    val (icon, color) = getFileIconAndColor(item)
    val isImage = item.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp") && (item.file != null || item.uri != null)
    val dateStr = SimpleDateFormat("MMMM dd, yyyy  'at'  hh:mm a", Locale.getDefault()).format(Date(item.lastModified))
    var showFormatDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .testTag("file_metadata_bottom_sheet")
        ) {
            // Header with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "File Details & Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thumbnail Preview Hero
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImage) {
                        AsyncImage(
                            model = item.file ?: item.uri,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (item.extension.isNotBlank()) item.extension.uppercase() else if (item.isDirectory) "DIRECTORY" else "FILE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = color
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File Name
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Attributes Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetadataRow(label = "Path", value = item.path)
                    MetadataRow(label = "Size", value = if (item.isDirectory) "${item.itemCount} items" else "${item.formattedSize} (${item.size} bytes)")
                    MetadataRow(label = "Extension", value = if (item.extension.isNotEmpty()) ".${item.extension}" else if (item.isDirectory) "Folder" else "None")
                    MetadataRow(label = "Modified", value = dateStr)
                    if (item.mimeType.isNotBlank() && item.mimeType != "*/*") {
                        MetadataRow(label = "MIME Type", value = item.mimeType)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Compress, Rename, Share, Open
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showFormatDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metadata_compress_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Compress",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ZIP", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onRename()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metadata_rename_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = "Rename",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rename", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onShare()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metadata_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onOpen()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metadata_open_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open", fontSize = 12.sp)
                }
            }
        }
    }

    if (showFormatDialog) {
        var selectedFormat by remember { mutableStateOf("zip") }
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("Compress Archive") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select archive format for background compression:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val formats = listOf(
                        Pair("ZIP Standard (.zip)", "zip"),
                        Pair("TAR Container (.tar)", "tar"),
                        Pair("Compressed GZIP (.tar.gz)", "tar.gz"),
                        Pair("7-Zip Container (.7z)", "7z")
                    )
                    formats.forEach { (name, ext) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedFormat == ext) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFormat = ext }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedFormat == ext, onClick = { selectedFormat = ext })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, fontSize = 13.sp, fontWeight = if (selectedFormat == ext) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFormatDialog = false
                        onDismiss()
                        onCompress(selectedFormat)
                    },
                    modifier = Modifier.testTag("confirm_format_compress_button")
                ) {
                    Text("Compress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
