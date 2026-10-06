package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.LibreFilesTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsState()
            val darkStyle by viewModel.darkThemeStyle.collectAsState()
            val accent by viewModel.accentChoice.collectAsState()

            LibreFilesTheme(
                darkTheme = isDark,
                darkThemeStyle = darkStyle,
                accentChoice = accent
            ) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    val currentScreen by viewModel.currentScreen.collectAsState()
    val textEditorContent by viewModel.textEditorContent.collectAsState()
    val checksums by viewModel.activeFileChecksums.collectAsState()
    val explorerState by viewModel.explorerState.collectAsState()

    val activeImageFile by viewModel.activeImagePreviewFile.collectAsState()
    val activeZipFile by viewModel.activeZipFile.collectAsState()
    val activeAudioFile by viewModel.activeAudioFile.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()

    var activeTextFile by remember { mutableStateOf(viewModel.activeTextEditorFile) }
    var activeDetailsFile by remember { mutableStateOf(viewModel.activeDetailsFile) }

    // Keep active file in sync
    LaunchedEffect(viewModel.activeTextEditorFile) {
        activeTextFile = viewModel.activeTextEditorFile
    }
    LaunchedEffect(viewModel.activeDetailsFile) {
        activeDetailsFile = viewModel.activeDetailsFile
    }

    // Show toast for status messages
    LaunchedEffect(explorerState.statusMessage) {
        explorerState.statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkStoragePermissions()
                viewModel.refreshStorageVolumes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isExpanded) {
                Column {
                    // Bottom Audio Player Bar if active
                    activeAudioFile?.let { audioItem ->
                        AudioPlayerBar(
                            item = audioItem,
                            isPlaying = isAudioPlaying,
                            onTogglePlay = { viewModel.toggleAudioPlayback() },
                            onClose = { viewModel.stopAudio() }
                        )
                    }

                    NavigationBar(
                        modifier = Modifier.testTag("main_bottom_nav"),
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.DASHBOARD,
                            onClick = { viewModel.navigateToScreen(NavigationScreen.DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                    contentDescription = "Dashboard",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_dashboard")
                        )

                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.EXPLORER,
                            onClick = { viewModel.navigateToScreen(NavigationScreen.EXPLORER) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.EXPLORER) Icons.Default.Folder else Icons.Outlined.Folder,
                                    contentDescription = "Explorer",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Files", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_explorer")
                        )

                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.ANALYZER,
                            onClick = { viewModel.navigateToScreen(NavigationScreen.ANALYZER) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.ANALYZER) Icons.Default.PieChart else Icons.Outlined.PieChart,
                                    contentDescription = "Analyze",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Analyze", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_analyzer")
                        )

                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.VAULT,
                            onClick = { viewModel.navigateToScreen(NavigationScreen.VAULT) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.VAULT) Icons.Default.Lock else Icons.Outlined.Lock,
                                    contentDescription = "Vault",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Vault", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_vault")
                        )

                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.SETTINGS,
                            onClick = { viewModel.navigateToScreen(NavigationScreen.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.SETTINGS) Icons.Default.Shield else Icons.Outlined.Shield,
                                    contentDescription = "Privacy",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Privacy", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_privacy")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpanded) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("main_nav_rail"),
                    header = {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "LibreFiles Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(32.dp)
                        )
                    }
                ) {
                    NavigationRailItem(
                        selected = currentScreen == NavigationScreen.DASHBOARD,
                        onClick = { viewModel.navigateToScreen(NavigationScreen.DASHBOARD) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == NavigationScreen.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("rail_dashboard")
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationScreen.EXPLORER,
                        onClick = { viewModel.navigateToScreen(NavigationScreen.EXPLORER) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == NavigationScreen.EXPLORER) Icons.Default.Folder else Icons.Outlined.Folder,
                                contentDescription = "Explorer"
                            )
                        },
                        label = { Text("Files") },
                        modifier = Modifier.testTag("rail_explorer")
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationScreen.ANALYZER,
                        onClick = { viewModel.navigateToScreen(NavigationScreen.ANALYZER) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == NavigationScreen.ANALYZER) Icons.Default.PieChart else Icons.Outlined.PieChart,
                                contentDescription = "Analyze"
                            )
                        },
                        label = { Text("Analyze") },
                        modifier = Modifier.testTag("rail_analyzer")
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationScreen.VAULT,
                        onClick = { viewModel.navigateToScreen(NavigationScreen.VAULT) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == NavigationScreen.VAULT) Icons.Default.Lock else Icons.Outlined.Lock,
                                contentDescription = "Vault"
                            )
                        },
                        label = { Text("Vault") },
                        modifier = Modifier.testTag("rail_vault")
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationScreen.SETTINGS,
                        onClick = { viewModel.navigateToScreen(NavigationScreen.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == NavigationScreen.SETTINGS) Icons.Default.Shield else Icons.Outlined.Shield,
                                contentDescription = "Privacy"
                            )
                        },
                        label = { Text("Privacy") },
                        modifier = Modifier.testTag("rail_privacy")
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isExpanded) {
                        activeAudioFile?.let { audioItem ->
                            AudioPlayerBar(
                                item = audioItem,
                                isPlaying = isAudioPlaying,
                                onTogglePlay = { viewModel.toggleAudioPlayback() },
                                onClose = { viewModel.stopAudio() }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                            when (screen) {
                                NavigationScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                                NavigationScreen.EXPLORER -> ExplorerScreen(viewModel = viewModel)
                                NavigationScreen.ANALYZER -> StorageAnalyzerScreen(viewModel = viewModel)
                                NavigationScreen.VAULT -> SecureVaultScreen(viewModel = viewModel)
                                NavigationScreen.SETTINGS -> PrivacyAboutScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                // Global Text Editor Dialog
                activeTextFile?.let { fileItem ->
                    TextEditorDialog(
                        fileName = fileItem.name,
                        initialContent = textEditorContent,
                        onSave = { newContent ->
                            viewModel.saveTextEditor(newContent)
                            activeTextFile = null
                        },
                        onDismiss = {
                            viewModel.activeTextEditorFile = null
                            activeTextFile = null
                        }
                    )
                }

                // Global Details & Checksum Dialog
                activeDetailsFile?.let { fileItem ->
                    FileDetailsDialog(
                        item = fileItem,
                        checksums = checksums,
                        onDismiss = {
                            viewModel.activeDetailsFile = null
                            activeDetailsFile = null
                        }
                    )
                }

                // Global Image Viewer Dialog
                activeImageFile?.let { imageItem ->
                    ImageViewerDialog(
                        item = imageItem,
                        onDismiss = {
                            viewModel.activeImagePreviewFile.value = null
                        }
                    )
                }

                // Global Zip Inspector Dialog
                activeZipFile?.let { zipItem ->
                    ZipInspectorDialog(
                        item = zipItem,
                        onExtract = {
                            viewModel.extractZip(zipItem)
                        },
                        onDismiss = {
                            viewModel.activeZipFile.value = null
                        }
                    )
                }
            }
        }
    }
}
