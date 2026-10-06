package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.LibreFilesTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationScreen

import androidx.fragment.app.FragmentActivity

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val EXPLORER = "explorer"
    const val RECENT = "recent"
    const val ANALYZER = "analyzer"
    const val VAULT = "vault"
    const val P2P_SYNC = "p2p_sync"
    const val SETTINGS = "settings"
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsState()
            val darkStyle by viewModel.darkThemeStyle.collectAsState()
            val accent by viewModel.accentChoice.collectAsState()
            val isDynamic by viewModel.isDynamicColor.collectAsState()

            LibreFilesTheme(
                darkTheme = isDark,
                darkThemeStyle = darkStyle,
                accentChoice = accent,
                dynamicColor = isDynamic
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
    val isLowPower by viewModel.isLowPowerMode.collectAsState()

    var activeTextFile by remember { mutableStateOf(viewModel.activeTextEditorFile) }
    var activeDetailsFile by remember { mutableStateOf(viewModel.activeDetailsFile) }

    val navController = rememberNavController()

    fun navigateTo(route: String, screen: NavigationScreen) {
        viewModel.navigateToScreen(screen)
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo(NavRoutes.DASHBOARD) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Sync external navigation triggers (e.g. Dashboard category clicks) with NavController
    LaunchedEffect(currentScreen) {
        val targetRoute = when (currentScreen) {
            NavigationScreen.DASHBOARD -> NavRoutes.DASHBOARD
            NavigationScreen.EXPLORER -> NavRoutes.EXPLORER
            NavigationScreen.RECENT -> NavRoutes.RECENT
            NavigationScreen.ANALYZER -> NavRoutes.ANALYZER
            NavigationScreen.VAULT -> NavRoutes.VAULT
            NavigationScreen.P2P_SYNC -> NavRoutes.P2P_SYNC
            NavigationScreen.SETTINGS -> NavRoutes.SETTINGS
        }
        if (navController.currentDestination?.route != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(NavRoutes.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

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

                    if (!(currentScreen == NavigationScreen.EXPLORER && explorerState.isSelectionMode)) {
                        NavigationBar(
                            modifier = Modifier.testTag("main_bottom_nav"),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == NavigationScreen.DASHBOARD,
                                onClick = { navigateTo(NavRoutes.DASHBOARD, NavigationScreen.DASHBOARD) },
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
                                onClick = { navigateTo(NavRoutes.EXPLORER, NavigationScreen.EXPLORER) },
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
                                onClick = { navigateTo(NavRoutes.ANALYZER, NavigationScreen.ANALYZER) },
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
                                onClick = { navigateTo(NavRoutes.VAULT, NavigationScreen.VAULT) },
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
                                onClick = { navigateTo(NavRoutes.SETTINGS, NavigationScreen.SETTINGS) },
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
                        onClick = { navigateTo(NavRoutes.DASHBOARD, NavigationScreen.DASHBOARD) },
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
                        onClick = { navigateTo(NavRoutes.EXPLORER, NavigationScreen.EXPLORER) },
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
                        selected = currentScreen == NavigationScreen.RECENT,
                        onClick = { navigateTo(NavRoutes.RECENT, NavigationScreen.RECENT) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == NavigationScreen.RECENT) Icons.Default.History else Icons.Outlined.History,
                                contentDescription = "Recent"
                            )
                        },
                        label = { Text("Recent") },
                        modifier = Modifier.testTag("rail_recent")
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationScreen.ANALYZER,
                        onClick = { navigateTo(NavRoutes.ANALYZER, NavigationScreen.ANALYZER) },
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
                        onClick = { navigateTo(NavRoutes.VAULT, NavigationScreen.VAULT) },
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
                        selected = currentScreen == NavigationScreen.P2P_SYNC,
                        onClick = { navigateTo(NavRoutes.P2P_SYNC, NavigationScreen.P2P_SYNC) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == NavigationScreen.P2P_SYNC) Icons.Default.WifiTethering else Icons.Outlined.WifiTethering,
                                contentDescription = "Wi-Fi Sync"
                            )
                        },
                        label = { Text("Sync") },
                        modifier = Modifier.testTag("rail_sync")
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationScreen.SETTINGS,
                        onClick = { navigateTo(NavRoutes.SETTINGS, NavigationScreen.SETTINGS) },
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

                    val enterAnimDuration = if (isLowPower) 100 else 380
                    val exitAnimDuration = if (isLowPower) 80 else 320

                    // Material 3 Expressive motion navigation between views
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.DASHBOARD,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * 0.18f).toInt() },
                                animationSpec = tween(
                                    durationMillis = enterAnimDuration,
                                    easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                                )
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = (enterAnimDuration * 0.8f).toInt(), easing = LinearOutSlowInEasing)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                                animationSpec = tween(
                                    durationMillis = exitAnimDuration,
                                    easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
                                )
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = (exitAnimDuration * 0.75f).toInt(), easing = FastOutLinearInEasing)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -(fullWidth * 0.18f).toInt() },
                                animationSpec = tween(
                                    durationMillis = enterAnimDuration,
                                    easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                                )
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = (enterAnimDuration * 0.8f).toInt(), easing = LinearOutSlowInEasing)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (fullWidth * 0.18f).toInt() },
                                animationSpec = tween(
                                    durationMillis = exitAnimDuration,
                                    easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
                                )
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = (exitAnimDuration * 0.75f).toInt(), easing = FastOutLinearInEasing)
                            )
                        }
                    ) {
                        composable(NavRoutes.DASHBOARD) {
                            DashboardScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.EXPLORER) {
                            ExplorerScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.RECENT) {
                            RecentFilesScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.ANALYZER) {
                            StorageAnalyticsScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.VAULT) {
                            SecureVaultScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.P2P_SYNC) {
                            P2pSyncScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.SETTINGS) {
                            PrivacyAboutScreen(viewModel = viewModel)
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
