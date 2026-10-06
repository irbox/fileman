package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.ui.components.FileMetadataBottomSheet
import com.example.ui.components.getFileIconAndColor
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeGroup(val title: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_WEEK("Last Week"),
    OLDER("Older")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = { viewModel.navigateToScreen(NavigationScreen.DASHBOARD) }
) {
    BackHandler { onNavigateBack() }

    val recentFiles by viewModel.recentFiles.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedItemForMetadata by remember { mutableStateOf<FileItem?>(null) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var newRenameName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadRecentFiles()
    }

    // Filter files by search query and category
    val filteredFiles = remember(recentFiles, searchQuery, selectedCategoryFilter) {
        recentFiles.filter { file ->
            val matchesSearch = searchQuery.isBlank() || file.name.contains(searchQuery.trim(), ignoreCase = true)
            val matchesCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Images" -> file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg")
                "Documents" -> file.extension.lowercase() in listOf("pdf", "doc", "docx", "txt", "xlsx", "csv", "ppt", "pptx")
                "Videos" -> file.extension.lowercase() in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp")
                "Audio" -> file.extension.lowercase() in listOf("mp3", "wav", "flac", "m4a", "ogg", "aac")
                "Archives" -> file.extension.lowercase() in listOf("zip", "rar", "7z", "tar", "gz")
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    // Time-based grouping: Today, Yesterday, Last Week, Older
    val groupedFiles = remember(filteredFiles) {
        val now = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        val lastWeekStart = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis

        val map = linkedMapOf<TimeGroup, MutableList<FileItem>>()
        for (file in filteredFiles) {
            val group = when {
                file.lastModified >= todayStart -> TimeGroup.TODAY
                file.lastModified >= yesterdayStart -> TimeGroup.YESTERDAY
                file.lastModified >= lastWeekStart -> TimeGroup.LAST_WEEK
                else -> TimeGroup.OLDER
            }
            map.getOrPut(group) { mutableListOf() }.add(file)
        }
        map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Recent Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            "${filteredFiles.size} items accessed recently",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("recent_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRecentFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Recent Files")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Expressive Search Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter recent files...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(48.dp)
                        .testTag("recent_search_input")
                )
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("All", "Images", "Documents", "Videos", "Audio", "Archives")
                categories.forEach { category ->
                    val isSelected = selectedCategoryFilter == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category, fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_chip_$category")
                    )
                }
            }

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches for \"$searchQuery\"" else "No recent files found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Files opened, edited or modified across LibreFiles will appear here grouped by timeline.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        FilledTonalButton(
                            onClick = { viewModel.navigateToScreen(NavigationScreen.EXPLORER) },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Files")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedFiles.forEach { (timeGroup, filesInGroup) ->
                        item(key = "header_${timeGroup.name}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = timeGroup.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${filesInGroup.size} files",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                HorizontalDivider(
                                    modifier = Modifier
                                        .weight(2f)
                                        .padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        items(filesInGroup, key = { it.path }) { item ->
                            RecentFileCard(
                                item = item,
                                onClick = { viewModel.openFile(item) },
                                onActionClick = { selectedItemForMetadata = item },
                                onShare = { viewModel.shareFile(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Metadata & Quick Actions Bottom Sheet modal
    selectedItemForMetadata?.let { targetItem ->
        FileMetadataBottomSheet(
            item = targetItem,
            onDismiss = { selectedItemForMetadata = null },
            onRename = {
                itemToRename = targetItem
                newRenameName = targetItem.name
            },
            onShare = {
                viewModel.shareFile(targetItem)
            },
            onOpen = {
                viewModel.openFile(targetItem)
            },
            onCompress = { format ->
                viewModel.compressSingleFile(targetItem, format)
            }
        )
    }

    // Rename dialog
    itemToRename?.let { targetItem ->
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newRenameName,
                    onValueChange = { newRenameName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newRenameName.trim()
                        if (trimmed.isNotEmpty() && trimmed != targetItem.name) {
                            viewModel.renameFile(targetItem.path, trimmed)
                        }
                        itemToRename = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecentFileCard(
    item: FileItem,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    onShare: () -> Unit
) {
    val (icon, color) = getFileIconAndColor(item)
    val timeFormatted = remember(item.lastModified) {
        val date = Date(item.lastModified)
        val cal = Calendar.getInstance().apply { time = date }
        val now = Calendar.getInstance()
        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        ) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("recent_file_${item.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.formattedSize,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = timeFormatted,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.extension.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = item.extension.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onActionClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("recent_item_more_${item.name}")
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
