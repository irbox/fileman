package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        else -> Icons.Default.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
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
                    imageVector = Icons.Default.ChevronRight,
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

// Built-in Text Editor Dialog
@Composable
fun TextEditorDialog(
    fileName: String,
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialContent) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
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
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("text_editor_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
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
