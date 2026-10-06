package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.TextEditorDialog
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
    val currentScreen by viewModel.currentScreen.collectAsState()
    val textEditorContent by viewModel.textEditorContent.collectAsState()
    val checksums by viewModel.activeFileChecksums.collectAsState()

    var activeTextFile by remember { mutableStateOf(viewModel.activeTextEditorFile) }
    var activeDetailsFile by remember { mutableStateOf(viewModel.activeDetailsFile) }

    // Keep active file in sync
    LaunchedEffect(viewModel.activeTextEditorFile) {
        activeTextFile = viewModel.activeTextEditorFile
    }
    LaunchedEffect(viewModel.activeDetailsFile) {
        activeDetailsFile = viewModel.activeDetailsFile
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    NavigationScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    NavigationScreen.EXPLORER -> ExplorerScreen(viewModel = viewModel)
                    NavigationScreen.ANALYZER -> StorageAnalyzerScreen(viewModel = viewModel)
                    NavigationScreen.VAULT -> SecureVaultScreen(viewModel = viewModel)
                    NavigationScreen.SETTINGS -> PrivacyAboutScreen(viewModel = viewModel)
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
        }
    }
}
