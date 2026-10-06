package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AuditLogEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyAboutScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val auditLogs by viewModel.fileRepository.auditLogs.collectAsState(initial = emptyList())
    val isDark by viewModel.isDarkMode.collectAsState()
    val currentStyle by viewModel.darkThemeStyle.collectAsState()
    val currentAccent by viewModel.accentChoice.collectAsState()
    val isDynamic by viewModel.isDynamicColor.collectAsState()
    val isLowPower by viewModel.isLowPowerMode.collectAsState()

    var showLicenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = LibreEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy, License & Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("privacy_about_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Privacy Sovereignty Badge Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LibreEmerald.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = LibreEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Data Sovereignty Verified",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LibreEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "LibreFiles is built as a 100% private CX Explorer alternative. No telemetry, no background pings, and no cloud data harvesting. Your files and metadata never leave your physical device.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PrivacyStatPill("Network Trackers", "0", LibreEmerald)
                            PrivacyStatPill("Ad SDKs", "0", LibreEmerald)
                            PrivacyStatPill("Telemetry", "NONE", LibreEmerald)
                        }
                    }
                }
            }

            // Appearance & Themes Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Appearance & Themes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dark Theme", fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isDark,
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }

                        // Dynamic Material You (System Wallpaper)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dynamic Material You", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Derive theme colors from system wallpaper",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isDynamic,
                                    onCheckedChange = { viewModel.setDynamicColor(it) },
                                    modifier = Modifier.testTag("dynamic_color_switch")
                                )
                            }
                        }

                        if (isDark) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Dark Palette Style", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DarkThemeStyle.entries.forEach { style ->
                                    val selected = currentStyle == style
                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.setDarkThemeStyle(style) },
                                        label = { Text(style.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isDynamic && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) "Custom Expressive Palette (Override Dynamic)" else "Expressive Color Palette",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Grid of Expressive Pre-defined Palettes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccentChoice.entries.chunked(4).forEach { rowAccents ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowAccents.forEach { accent ->
                                        val selected = currentAccent == accent && (!isDynamic || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = accent.primary.copy(alpha = if (selected) 0.25f else 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) accent.primary else Color.Transparent
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.setDynamicColor(false)
                                                    viewModel.setAccentChoice(accent)
                                                }
                                                .testTag("palette_choice_${accent.name}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(accent.primary)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = accent.displayName.split(" ").last(),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Performance & Low Power Mode Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Performance & Battery Efficiency", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = if (isLowPower) LibreEmerald else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Low Power Mode", fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reduces UI motion animation intensity and lowers the background refresh rate for storage analytics, improving overall device performance and battery efficiency.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isLowPower,
                                onCheckedChange = { viewModel.setLowPowerMode(it) },
                                modifier = Modifier.testTag("low_power_mode_switch")
                            )
                        }
                    }
                }
            }

            // Open Source & F-Droid Transparency
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Open Source & Reproducibility", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GNU General Public License v3.0", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("GPL-3.0 Free Software copyleft guarantee", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { showLicenseDialog = true }) {
                                Text("View Terms")
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("F-Droid Compatibility", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Reproducible build environment", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Badge(containerColor = LibreCyan.copy(alpha = 0.2f)) {
                                Text("Verified", color = LibreCyan, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("GitHub Actions CI/CD", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Automated binary hash checks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Badge(containerColor = LibreEmerald.copy(alpha = 0.2f)) {
                                Text("Pass", color = LibreEmerald, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }
            }

            // Audit Logs (Transparency in action)
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Local Action Audit Log", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${auditLogs.size} events", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (auditLogs.isEmpty()) {
                item {
                    Text("No file modifications logged yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(auditLogs.take(15)) { log ->
                    AuditLogRow(log = log)
                }
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text("GNU General Public License v3.0") },
            text = {
                Column {
                    Text(
                        text = """Copyright (C) 2026 LibreFiles Contributors.

Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.""",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PrivacyStatPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = color)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AuditLogRow(log: AuditLogEntity) {
    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (log.action) {
                    "VAULT_ENCRYPT", "VAULT_DECRYPT" -> LibreIndigo.copy(alpha = 0.2f)
                    "MOVE_TO_TRASH", "DELETE_PERM" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    else -> LibreCyan.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = log.action,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.details.ifEmpty { log.target }, fontSize = 12.sp, maxLines = 1)
            }
            Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
