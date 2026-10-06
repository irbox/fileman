package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.VaultItemEntity
import com.example.data.model.FileItem
import com.example.ui.theme.LibreCyan
import com.example.ui.theme.LibreEmerald
import com.example.ui.theme.LibreIndigo
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecureVaultScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val vaultState by viewModel.vaultState.collectAsState()

    var passcodeEntry by remember { mutableStateOf("") }
    var confirmPasscodeEntry by remember { mutableStateOf("") }
    var setupError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LibreIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Secure Vault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    if (vaultState.isUnlocked) {
                        Button(
                            onClick = { viewModel.lockVault() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("lock_vault_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock Safe", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("secure_vault_screen")
        ) {
            when {
                // Case 1: First time setup
                !vaultState.isConfigured -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(LibreIndigo.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = LibreIndigo,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Set Up Encrypted Vault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your sensitive files will be encrypted using 256-bit AES-GCM with keys derived via PBKDF2. No passwords or files ever leave your device.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = passcodeEntry,
                            onValueChange = { passcodeEntry = it },
                            label = { Text("Master Passcode (min 6 characters)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_setup_passcode")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPasscodeEntry,
                            onValueChange = { confirmPasscodeEntry = it },
                            label = { Text("Confirm Master Passcode") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_confirm_passcode")
                        )

                        if (setupError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = setupError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (passcodeEntry.length < 6) {
                                    setupError = "Passcode must be at least 6 characters"
                                } else if (passcodeEntry != confirmPasscodeEntry) {
                                    setupError = "Passcodes do not match"
                                } else {
                                    setupError = null
                                    viewModel.setupVaultPasscode(passcodeEntry)
                                    passcodeEntry = ""
                                    confirmPasscodeEntry = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_create_button")
                        ) {
                            Text("Initialize Secure Safe")
                        }
                    }
                }

                // Case 2: Configured but Locked
                !vaultState.isUnlocked -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(LibreCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = LibreCyan,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Vault Locked",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter your master passcode to unlock your encrypted records.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = passcodeEntry,
                            onValueChange = { passcodeEntry = it },
                            label = { Text("Master Passcode") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_unlock_input")
                        )

                        if (vaultState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = vaultState.errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (passcodeEntry.isNotEmpty()) {
                                    viewModel.unlockVault(passcodeEntry)
                                    passcodeEntry = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_unlock_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Safe")
                        }
                    }
                }

                // Case 3: Unlocked Vault List
                else -> {
                    if (vaultState.items.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No encrypted files in vault yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select any file in the file browser and choose 'Batch Operations' or item options to encrypt it with AES-256-GCM into this vault.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LibreEmerald.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = LibreEmerald)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Vault Active: ${vaultState.items.size} Files Protected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("AES-GCM-256 • Zero-knowledge offline encryption", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            items(vaultState.items) { item ->
                                VaultItemCard(
                                    item = item,
                                    onDecrypt = { viewModel.decryptVaultItem(item) },
                                    onDelete = { viewModel.deleteVaultItem(item) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(96.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultItemCard(
    item: VaultItemEntity,
    onDecrypt: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.dateEncrypted))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(LibreIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = LibreIndigo, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${FileItem.formatFileSize(item.sizeBytes)} • Encrypted on $dateStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDecrypt) {
                Icon(Icons.Default.LockOpen, contentDescription = "Decrypt", tint = LibreCyan)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
