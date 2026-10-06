package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.FileItem
import com.example.data.repository.DiskCategoryUsage
import com.example.ui.components.DonutChart
import com.example.ui.components.getFileIconAndColor
import com.example.ui.theme.LibreCyan
import com.example.ui.theme.LibreEmerald
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StorageAnalyticsViewModel

@Composable
fun StorageAnalyticsScreen(
    viewModel: MainViewModel,
    analyticsViewModel: StorageAnalyticsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by analyticsViewModel.uiState.collectAsState()
    val trashItems by viewModel.fileRepository.trashItems.collectAsState(initial = emptyList())
    val trashSize = trashItems.sumOf { it.sizeBytes }
    val isLowPower by viewModel.isLowPowerMode.collectAsState()

    LaunchedEffect(Unit) {
        analyticsViewModel.loadStorageAnalytics()
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Storage Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    IconButton(
                        onClick = { analyticsViewModel.loadStorageAnalytics() },
                        modifier = Modifier.testTag("refresh_analytics_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Analytics")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("storage_analytics_screen"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Interactive Donut Chart Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donut_chart_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Disk Usage Breakdown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Free: ${FileItem.formatFileSize(state.freeBytes)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Donut Chart visualization
                            DonutChart(
                                categories = state.diskCategories,
                                totalUsedFormatted = FileItem.formatFileSize(state.usedBytes),
                                usedPercentage = (state.usedPercentage * 100).toInt(),
                                chartSize = 210.dp,
                                strokeWidth = 26.dp,
                                isLowPowerMode = isLowPower
                            )

                            if (isLowPower) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "⚡ Low Power Mode: Static analytics & reduced frame rasterization",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Recycle Bin Cleanup Card
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recycle Bin (${trashItems.size} items)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${FileItem.formatFileSize(trashSize)} can be reclaimed",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { analyticsViewModel.emptyTrash() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                enabled = trashItems.isNotEmpty(),
                                modifier = Modifier.testTag("empty_trash_button")
                            ) {
                                Text("Empty", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Cache Cleaner Card
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LibreEmerald.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(LibreEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = LibreEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System Cache & Temp Junk",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (state.reclaimedCacheBytes != null)
                                        "Reclaimed ${FileItem.formatFileSize(state.reclaimedCacheBytes!!)}!"
                                    else "Purge temporary and thumbnail caches",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { analyticsViewModel.cleanAppCache() },
                                colors = ButtonDefaults.buttonColors(containerColor = LibreEmerald),
                                modifier = Modifier.testTag("clean_cache_button")
                            ) {
                                Text("Optimize", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // File Type Categories Header
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "File Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Category Items
                items(state.diskCategories) { cat ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(cat.composeColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (cat.categoryName) {
                                    "Images" -> Icons.Default.Image
                                    "Video" -> Icons.Default.VideoLibrary
                                    "Documents" -> Icons.Default.Description
                                    else -> Icons.Default.FolderZip
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = cat.composeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.categoryName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${cat.fileCount} files",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = cat.formattedSize,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${(cat.percentageOfUsed * 100).toInt()}% of disk",
                                    fontSize = 11.sp,
                                    color = cat.composeColor
                                )
                            }
                        }
                    }
                }

                // Largest Files Section
                if (state.largeFiles.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Largest Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(state.largeFiles.take(6)) { fileItem ->
                        val (icon, color) = getFileIconAndColor(fileItem)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { analyticsViewModel.openFile(fileItem) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileItem.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = fileItem.path,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = fileItem.formattedSize,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
