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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.data.p2p.P2pConnectionStatus
import com.example.data.p2p.P2pDeviceItem
import com.example.ui.theme.LibreCyan
import com.example.ui.theme.LibreEmerald
import com.example.ui.theme.LibreIndigo
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationScreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2pSyncScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = { viewModel.navigateToScreen(NavigationScreen.DASHBOARD) }
) {
    BackHandler { onNavigateBack() }

    val p2pState by viewModel.p2pSyncState.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val isLowPower by viewModel.isLowPowerMode.collectAsState()

    var showSendFileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRecentFiles()
        viewModel.startP2pDiscovery()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Wi-Fi Direct File Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Local-only Peer-to-Peer Transfer",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("p2p_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startP2pDiscovery() },
                        modifier = Modifier.testTag("p2p_refresh_discovery_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan for peers")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("p2p_sync_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Zero-Telemetry Sovereignty Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LibreEmerald.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LibreEmerald.copy(alpha = 0.2f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WifiTethering,
                                    contentDescription = null,
                                    tint = LibreEmerald,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zero Telemetry • Offline Link",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = LibreEmerald
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Move files directly between devices over high-speed Wi-Fi Direct. No routers, no internet, and zero data telemetry.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Connection Status & Link Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Connection State",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (p2pState.connectionStatus) {
                                    P2pConnectionStatus.CONNECTED -> LibreEmerald.copy(alpha = 0.18f)
                                    P2pConnectionStatus.TRANSFERRING -> LibreCyan.copy(alpha = 0.18f)
                                    P2pConnectionStatus.CONNECTING -> LibreIndigo.copy(alpha = 0.18f)
                                    P2pConnectionStatus.DISCOVERING -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = p2pState.connectionStatus.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (p2pState.connectionStatus) {
                                        P2pConnectionStatus.CONNECTED -> LibreEmerald
                                        P2pConnectionStatus.TRANSFERRING -> LibreCyan
                                        P2pConnectionStatus.CONNECTING -> LibreIndigo
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = p2pState.statusMessage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        p2pState.connectedDevice?.let { peer ->
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = peer.deviceName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = peer.deviceAddress,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.disconnectP2p() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Disconnect", fontSize = 12.sp)
                                }
                            }
                        }

                        if (isLowPower) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "⚡ Low Power Mode active: Continuous peer broadcast is throttled to conserve battery.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Transfer Progress Section (if transferring or completed)
            if (p2pState.transferProgress.isActive || p2pState.transferProgress.bytesTransferred > 0) {
                item {
                    val progress = p2pState.transferProgress
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (progress.isSending) Icons.Default.Upload else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (progress.isSending) "Sending File" else "Receiving File",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                if (progress.speedMbps > 0) {
                                    Text(
                                        text = "%.1f Mbps".format(progress.speedMbps),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = progress.fileName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress.percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = progress.statusMessage,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progress.percentage * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Action Row: Send File & Scan
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showSendFileDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("p2p_send_file_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send File", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.startP2pDiscovery() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("p2p_scan_peers_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Peers", fontSize = 13.sp)
                    }
                }
            }

            // Nearby Discovered Devices
            item {
                Text(
                    text = "Nearby Devices (${p2pState.discoveredDevices.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (p2pState.discoveredDevices.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiFind,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Searching for nearby offline peers...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Make sure Wi-Fi Direct or P2P Sync is enabled on the second device.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(p2pState.discoveredDevices, key = { it.deviceAddress }) { device ->
                    DevicePeerCard(
                        device = device,
                        isConnected = p2pState.connectedDevice?.deviceAddress == device.deviceAddress,
                        onConnect = { viewModel.connectToP2pDevice(device) }
                    )
                }
            }
        }
    }

    // Send File Selection Dialog
    if (showSendFileDialog) {
        AlertDialog(
            onDismissRequest = { showSendFileDialog = false },
            title = { Text("Select File to Sync") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose an item from recent files to transfer:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentFiles.isEmpty()) {
                        Text(
                            text = "No files available yet. Create or browse files in Explorer first.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(recentFiles.take(10), key = { it.path }) { fileItem ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showSendFileDialog = false
                                            val file = fileItem.file ?: File(fileItem.path)
                                            val targetIp = p2pState.groupOwnerIp ?: "192.168.49.1"
                                            viewModel.sendP2pFile(file)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fileItem.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = fileItem.formattedSize,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSendFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DevicePeerCard(
    device: P2pDeviceItem,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("peer_device_${device.deviceAddress}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isConnected) LibreEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.WifiTethering else Icons.Default.Devices,
                        contentDescription = null,
                        tint = if (isConnected) LibreEmerald else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.status,
                        fontSize = 11.sp,
                        color = if (isConnected) LibreEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = device.deviceAddress,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (isConnected) {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Linked", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("connect_peer_btn_${device.deviceAddress}")
                ) {
                    Text("Connect", fontSize = 12.sp)
                }
            }
        }
    }
}
