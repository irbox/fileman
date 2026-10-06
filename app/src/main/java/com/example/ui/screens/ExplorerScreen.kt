package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LibreCyan
import com.example.ui.theme.LibreIndigo
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExplorerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.explorerState.collectAsState()
    val clipboardItems by viewModel.clipboardItems.collectAsState()
    val clipboardOp by viewModel.clipboardOp.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(true) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showBatchZipDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deletePermanently by remember { mutableStateOf(false) }

    // Real-time Top App Bar Search Query & Filter
    var searchQuery by remember { mutableStateOf("") }

    val displayedFiles = remember(state.currentFiles, searchQuery) {
        if (searchQuery.isBlank()) {
            state.currentFiles
        } else {
            val q = searchQuery.trim()
            state.currentFiles.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    // Rename single item dialog
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var newRenameName by remember { mutableStateOf("") }

    // Back button handling: navigate up directory tree before exiting
    BackHandler(enabled = state.isSelectionMode || state.breadcrumbs.size > 1) {
        if (state.isSelectionMode) {
            viewModel.clearSelection()
        } else {
            searchQuery = ""
            viewModel.navigateUp()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (state.isSelectionMode) {
                // Batch Operation Top Bar
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                        Text(
                            text = "${state.selectedFiles.size} selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.copySelectedToClipboard() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = { viewModel.cutSelectedToClipboard() }) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Move")
                        }
                        IconButton(
                            onClick = { showBatchRenameDialog = true },
                            modifier = Modifier.testTag("batch_rename_icon_button")
                        ) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Batch Rename")
                        }
                        IconButton(
                            onClick = { showBatchZipDialog = true },
                            modifier = Modifier.testTag("batch_zip_icon_button")
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = "Compress to ZIP")
                        }
                        IconButton(
                            onClick = { showBatchDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("batch_delete_icon_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                // Standard Explorer Top Bar with integrated Scaffold top search bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.navigateUp()
                                },
                                enabled = state.breadcrumbs.size > 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Up")
                            }

                            // Real-time Search Bar directly in Scaffold Top App Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search files...", fontSize = 13.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("file_search_input")
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(onClick = { viewModel.toggleViewLayout() }) {
                                Icon(
                                    imageVector = when (state.viewLayout) {
                                        ViewLayout.LIST -> Icons.Default.GridView
                                        ViewLayout.GRID -> Icons.Default.ViewAgenda
                                        ViewLayout.COMPACT -> Icons.AutoMirrored.Filled.ViewList
                                    },
                                    contentDescription = "Toggle Layout"
                                )
                            }

                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Options")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Name (A to Z)") },
                                        onClick = {
                                            viewModel.setSortOption(SortOption(SortField.NAME, SortDirection.ASCENDING))
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (Z to A)") },
                                        onClick = {
                                            viewModel.setSortOption(SortOption(SortField.NAME, SortDirection.DESCENDING))
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Date (Newest First)") },
                                        onClick = {
                                            viewModel.setSortOption(SortOption(SortField.DATE, SortDirection.DESCENDING))
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Date (Oldest First)") },
                                        onClick = {
                                            viewModel.setSortOption(SortOption(SortField.DATE, SortDirection.ASCENDING))
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Size (Largest First)") },
                                        onClick = {
                                            viewModel.setSortOption(SortOption(SortField.SIZE, SortDirection.DESCENDING))
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Size (Smallest First)") },
                                        onClick = {
                                            viewModel.setSortOption(SortOption(SortField.SIZE, SortDirection.ASCENDING))
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Type (Extension)") },
                                        onClick = {
                                            viewModel.setSortOption(SortOption(SortField.TYPE, SortDirection.ASCENDING))
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Breadcrumb Navigation Bar
                        BreadcrumbBar(
                            breadcrumbs = state.breadcrumbs,
                            onBreadcrumbClick = { path ->
                                if (path.isNotEmpty()) {
                                    searchQuery = ""
                                    viewModel.navigateToDirectory(path)
                                }
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (state.isSelectionMode) {
                SelectionBottomBar(
                    selectedCount = state.selectedFiles.size,
                    onDelete = { showBatchDeleteConfirmDialog = true },
                    onMove = { viewModel.cutSelectedToClipboard() },
                    onShare = { viewModel.shareSelectedFiles() },
                    onCopy = { viewModel.copySelectedToClipboard() },
                    onCancel = { viewModel.clearSelection() }
                )
            }
        },
        floatingActionButton = {
            if (state.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { showBatchDeleteConfirmDialog = true },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Bulk Delete"
                        )
                    },
                    text = {
                        Text(
                            text = "Delete (${state.selectedFiles.size})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("bulk_delete_fab")
                )
            } else {
                var fabExpanded by remember { mutableStateOf(false) }

                Column(horizontalAlignment = Alignment.End) {
                    if (fabExpanded) {
                        FloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                isCreatingFolder = false
                                showCreateDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .testTag("create_file_fab")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New File")
                        }

                        FloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                isCreatingFolder = true
                                showCreateDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .testTag("create_folder_fab")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                        }
                    }

                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("main_explorer_fab")
                    ) {
                        Icon(
                            imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add New"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Clipboard paste banner if items are in clipboard
                if (clipboardItems.isNotEmpty() && clipboardOp != null) {
                    PasteClipboardBar(
                        items = clipboardItems,
                        operation = clipboardOp!!,
                        onPaste = { viewModel.pasteClipboard() },
                        onCancel = { viewModel.cancelClipboard() }
                    )
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (displayedFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No files matching \"$searchQuery\"",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Clear search")
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "This folder is empty",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    when (state.viewLayout) {
                        ViewLayout.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                items(displayedFiles, key = { it.path }) { item ->
                                    val isSelected = item.path in state.selectedFiles
                                    FileListItem(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = state.isSelectionMode,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                viewModel.toggleSelection(item.path)
                                            } else {
                                                viewModel.openFile(item)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleSelection(item.path)
                                        },
                                        onActionClick = { action ->
                                            when (action) {
                                                "open_system" -> {
                                                    val f = item.file ?: File(item.path)
                                                    viewModel.fileRepository.openWithExternalApp(f)
                                                }
                                                "copy" -> {
                                                    viewModel.toggleSelection(item.path)
                                                    viewModel.copySelectedToClipboard()
                                                }
                                                "cut" -> {
                                                    viewModel.toggleSelection(item.path)
                                                    viewModel.cutSelectedToClipboard()
                                                }
                                                "rename" -> {
                                                    itemToRename = item
                                                    newRenameName = item.name
                                                }
                                                "details" -> viewModel.showFileDetails(item)
                                                "share" -> viewModel.shareFile(item)
                                                "edit" -> viewModel.openTextEditor(item)
                                                "zip" -> {
                                                    viewModel.toggleSelection(item.path)
                                                    showBatchZipDialog = true
                                                }
                                                "vault" -> viewModel.importFileToVault(item)
                                                "delete" -> {
                                                    viewModel.toggleSelection(item.path)
                                                    viewModel.deleteSelected(permanent = false)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        ViewLayout.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp, top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(displayedFiles, key = { it.path }) { item ->
                                    val isSelected = item.path in state.selectedFiles
                                    FileGridItem(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = state.isSelectionMode,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                viewModel.toggleSelection(item.path)
                                            } else {
                                                viewModel.openFile(item)
                                            }
                                        },
                                        onLongClick = { viewModel.toggleSelection(item.path) }
                                    )
                                }
                            }
                        }
                        ViewLayout.COMPACT -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                items(displayedFiles, key = { it.path }) { item ->
                                    val isSelected = item.path in state.selectedFiles
                                    FileCompactItem(
                                        item = item,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                viewModel.toggleSelection(item.path)
                                            } else {
                                                viewModel.openFile(item)
                                            }
                                        },
                                        onLongClick = { viewModel.toggleSelection(item.path) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateItemDialog(
            isFolder = isCreatingFolder,
            onConfirm = { name ->
                if (isCreatingFolder) viewModel.createFolder(name) else viewModel.createNewFile(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showBatchRenameDialog) {
        BatchRenameDialog(
            selectedCount = state.selectedFiles.size,
            onConfirm = { prefix, suffix, find, replace, numbering ->
                viewModel.batchRename(prefix, suffix, find, replace, numbering)
                showBatchRenameDialog = false
            },
            onDismiss = { showBatchRenameDialog = false }
        )
    }

    if (showBatchZipDialog) {
        CreateZipDialog(
            selectedCount = state.selectedFiles.size,
            onConfirm = { zipName ->
                viewModel.compressSelectedToZip(zipName)
                showBatchZipDialog = false
            },
            onDismiss = { showBatchZipDialog = false }
        )
    }

    if (itemToRename != null) {
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newRenameName,
                    onValueChange = { newRenameName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_single_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val itm = itemToRename
                        if (itm != null && newRenameName.isNotBlank()) {
                            viewModel.renameFile(itm.path, newRenameName.trim())
                        }
                        itemToRename = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onActionClick: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val (icon, color) = getFileIconAndColor(item)
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.lastModified))

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("file_item_${item.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            val isImage = item.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") && item.file != null
            if (isImage) {
                AsyncImage(
                    model = item.file,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isDirectory) "${item.itemCount} items" else item.formattedSize,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • $dateStr",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in System App") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("open_system")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Details & Checksum") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("details")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("copy")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cut / Move") },
                        leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("cut")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("share")
                        }
                    )
                    if (!item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Edit in Text Editor") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onActionClick("edit")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Encrypt to Vault") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = LibreIndigo) },
                            onClick = {
                                menuExpanded = false
                                onActionClick("vault")
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("rename")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compress (.zip)") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("zip")
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onActionClick("delete")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (icon, color) = getFileIconAndColor(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("grid_file_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            val isImage = item.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") && item.file != null
            if (isImage) {
                AsyncImage(
                    model = item.file,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (item.isDirectory) "${item.itemCount} items" else item.formattedSize,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCompactItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (icon, color) = getFileIconAndColor(item)

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.name,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (item.isDirectory) "dir" else item.formattedSize,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
