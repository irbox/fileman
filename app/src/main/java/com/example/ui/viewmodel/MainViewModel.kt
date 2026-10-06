package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.crypto.VaultCrypto
import com.example.data.db.*
import com.example.data.model.*
import com.example.data.repository.FileManagerRepository
import com.example.data.repository.SafRepository
import com.example.data.repository.VaultRepository
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.DarkThemeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class NavigationScreen(val title: String) {
    DASHBOARD("Home"),
    EXPLORER("Files"),
    ANALYZER("Analyze"),
    VAULT("Vault"),
    SETTINGS("Privacy & About")
}

data class ExplorerUiState(
    val currentPath: String = "",
    val currentFiles: List<FileItem> = emptyList(),
    val breadcrumbs: List<Pair<String, String>> = emptyList(), // Name to Path
    val selectedFiles: Set<String> = emptySet(), // File paths
    val isSelectionMode: Boolean = false,
    val viewLayout: ViewLayout = ViewLayout.LIST,
    val sortOption: SortOption = SortOption(SortField.NAME, SortDirection.ASCENDING),
    val filterCriteria: FilterCriteria = FilterCriteria(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

data class VaultUiState(
    val isConfigured: Boolean = false,
    val isUnlocked: Boolean = false,
    val items: List<VaultItemEntity> = emptyList(),
    val currentPasscode: String = "",
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "libre_files.db"
    ).build()

    val fileRepository = FileManagerRepository(application, database)
    val safRepository = SafRepository(application)
    val vaultRepository = VaultRepository(application, database)

    // Navigation
    private val _currentScreen = MutableStateFlow(NavigationScreen.DASHBOARD)
    val currentScreen: StateFlow<NavigationScreen> = _currentScreen.asStateFlow()

    // Explorer State
    private val _explorerState = MutableStateFlow(ExplorerUiState())
    val explorerState: StateFlow<ExplorerUiState> = _explorerState.asStateFlow()

    // Storage Volumes & Stats
    private val _storageVolumes = MutableStateFlow<List<StorageVolume>>(emptyList())
    val storageVolumes: StateFlow<List<StorageVolume>> = _storageVolumes.asStateFlow()

    private val _storageAnalysis = MutableStateFlow<StorageAnalysisResult?>(null)
    val storageAnalysis: StateFlow<StorageAnalysisResult?> = _storageAnalysis.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()

    // Vault State
    private val _vaultState = MutableStateFlow(VaultUiState())
    val vaultState: StateFlow<VaultUiState> = _vaultState.asStateFlow()

    // Active Dialogs & Viewers
    var activeTextEditorFile: FileItem? = null
    val textEditorContent = MutableStateFlow("")

    var activeImagePreviewFile: FileItem? = null
    var activeAudioFile: FileItem? = null
    val isAudioPlaying = MutableStateFlow(false)

    var activeDetailsFile: FileItem? = null
    val activeFileChecksums = MutableStateFlow<Map<String, String>>(emptyMap())

    // Theme Customization
    val isDarkMode = MutableStateFlow(true)
    val darkThemeStyle = MutableStateFlow(DarkThemeStyle.SLATE)
    val accentChoice = MutableStateFlow(AccentChoice.CYAN)

    // Root directory
    private val defaultRoot: File by lazy {
        File(application.filesDir, "Local_Storage").apply { if (!exists()) mkdirs() }
    }

    init {
        viewModelScope.launch {
            fileRepository.initializeSampleFilesIfEmpty()
            refreshStorageVolumes()
            refreshVaultStatus()
            navigateToDirectory(defaultRoot.path)
            refreshStorageAnalysis()
            loadRecentFiles()
        }

        // Collect vault items
        viewModelScope.launch {
            vaultRepository.vaultItems.collect { list ->
                _vaultState.update { it.copy(items = list) }
            }
        }
    }

    fun navigateToScreen(screen: NavigationScreen) {
        _currentScreen.value = screen
    }

    fun refreshStorageVolumes() {
        viewModelScope.launch {
            val volumes = fileRepository.getStorageVolumes().toMutableList()
            volumes.addAll(safRepository.getPersistedUriVolumes())
            _storageVolumes.value = volumes
        }
    }

    fun refreshStorageAnalysis() {
        viewModelScope.launch {
            _storageAnalysis.value = fileRepository.analyzeStorage()
        }
    }

    private fun loadRecentFiles() {
        viewModelScope.launch {
            val all = fileRepository.listFiles(defaultRoot.path)
            val sub = mutableListOf<FileItem>()
            defaultRoot.walkTopDown().maxDepth(3).filter { it.isFile }.take(15).forEach { f ->
                sub.add(
                    FileItem(
                        file = f,
                        name = f.name,
                        path = f.path,
                        size = f.length(),
                        isDirectory = false,
                        lastModified = f.lastModified(),
                        extension = f.extension.lowercase(),
                        mimeType = fileRepository.getMimeType(f)
                    )
                )
            }
            _recentFiles.value = sub.sortedByDescending { it.lastModified }.take(8)
        }
    }

    fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            _explorerState.update { it.copy(isLoading = true, currentPath = path, selectedFiles = emptySet(), isSelectionMode = false) }
            val files = fileRepository.listFiles(
                directoryPath = path,
                filter = _explorerState.value.filterCriteria,
                sortOption = _explorerState.value.sortOption
            )

            // Build breadcrumbs
            val rootPath = defaultRoot.path
            val breadcrumbs = mutableListOf<Pair<String, String>>()
            breadcrumbs.add(Pair("Root", rootPath))

            if (path != rootPath && path.startsWith(rootPath)) {
                val rel = path.removePrefix(rootPath).trim('/')
                val parts = rel.split('/')
                var accum = rootPath
                for (p in parts) {
                    if (p.isNotEmpty()) {
                        accum += "/$p"
                        breadcrumbs.add(Pair(p, accum))
                    }
                }
            } else if (path != rootPath) {
                breadcrumbs.add(Pair(File(path).name, path))
            }

            _explorerState.update {
                it.copy(
                    currentFiles = files,
                    breadcrumbs = breadcrumbs,
                    isLoading = false
                )
            }
        }
    }

    fun navigateUp(): Boolean {
        val curr = _explorerState.value.currentPath
        val parent = File(curr).parentFile
        if (parent != null && curr != defaultRoot.path && parent.exists()) {
            navigateToDirectory(parent.path)
            return true
        }
        return false
    }

    fun openCategory(category: FileCategory) {
        viewModelScope.launch {
            _currentScreen.value = NavigationScreen.EXPLORER
            _explorerState.update {
                it.copy(
                    filterCriteria = it.filterCriteria.copy(selectedCategory = category),
                    isLoading = true
                )
            }
            if (category == FileCategory.ALL) {
                navigateToDirectory(defaultRoot.path)
            } else {
                val catFiles = fileRepository.getCategoryFiles(category)
                _explorerState.update {
                    it.copy(
                        currentFiles = catFiles,
                        breadcrumbs = listOf(Pair("Root", defaultRoot.path), Pair(category.title, "")),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String, isRegex: Boolean = false) {
        _explorerState.update {
            it.copy(filterCriteria = it.filterCriteria.copy(searchQuery = query, isRegexSearch = isRegex))
        }
        refreshCurrentDirectory()
    }

    fun setSortOption(sortOption: SortOption) {
        _explorerState.update { it.copy(sortOption = sortOption) }
        refreshCurrentDirectory()
    }

    fun toggleViewLayout() {
        _explorerState.update {
            val next = when (it.viewLayout) {
                ViewLayout.LIST -> ViewLayout.GRID
                ViewLayout.GRID -> ViewLayout.COMPACT
                ViewLayout.COMPACT -> ViewLayout.LIST
            }
            it.copy(viewLayout = next)
        }
    }

    fun toggleSelection(path: String) {
        _explorerState.update {
            val newSet = it.selectedFiles.toMutableSet()
            if (newSet.contains(path)) newSet.remove(path) else newSet.add(path)
            it.copy(
                selectedFiles = newSet,
                isSelectionMode = newSet.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        val allPaths = _explorerState.value.currentFiles.map { it.path }.toSet()
        _explorerState.update {
            it.copy(selectedFiles = allPaths, isSelectionMode = true)
        }
    }

    fun clearSelection() {
        _explorerState.update {
            it.copy(selectedFiles = emptySet(), isSelectionMode = false)
        }
    }

    fun refreshCurrentDirectory() {
        val curr = _explorerState.value.currentPath
        if (curr.isNotEmpty()) {
            navigateToDirectory(curr)
        }
    }

    // File Actions
    fun createFolder(name: String) {
        viewModelScope.launch {
            val success = fileRepository.createDirectory(_explorerState.value.currentPath, name)
            if (success) refreshCurrentDirectory()
        }
    }

    fun createNewFile(name: String, content: String = "") {
        viewModelScope.launch {
            val success = fileRepository.createFile(_explorerState.value.currentPath, name, content)
            if (success) refreshCurrentDirectory()
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            val success = fileRepository.renameFile(oldPath, newName)
            if (success) refreshCurrentDirectory()
        }
    }

    fun deleteSelected(permanent: Boolean = false) {
        viewModelScope.launch {
            val items = _explorerState.value.currentFiles.filter { it.path in _explorerState.value.selectedFiles }
            if (permanent) {
                fileRepository.deletePermanently(items)
            } else {
                fileRepository.moveToTrash(items)
            }
            clearSelection()
            refreshCurrentDirectory()
            refreshStorageAnalysis()
        }
    }

    fun batchRename(prefix: String, suffix: String, findText: String, replaceText: String, sequentialNumbering: Boolean) {
        viewModelScope.launch {
            val items = _explorerState.value.currentFiles.filter { it.path in _explorerState.value.selectedFiles }
            fileRepository.batchRename(items, prefix, suffix, findText, replaceText, sequentialNumbering)
            clearSelection()
            refreshCurrentDirectory()
        }
    }

    fun batchZip(zipName: String) {
        viewModelScope.launch {
            val items = _explorerState.value.currentFiles.filter { it.path in _explorerState.value.selectedFiles }
            fileRepository.createZipArchive(items, zipName, _explorerState.value.currentPath)
            clearSelection()
            refreshCurrentDirectory()
        }
    }

    fun extractZip(item: FileItem) {
        viewModelScope.launch {
            val file = item.file ?: return@launch
            val outDir = File(file.parentFile, file.nameWithoutExtension)
            fileRepository.extractZipArchive(file, outDir.path)
            refreshCurrentDirectory()
        }
    }

    fun computeChecksums(item: FileItem) {
        activeDetailsFile = item
        viewModelScope.launch(Dispatchers.IO) {
            val file = item.file ?: return@launch
            val md5 = VaultCrypto.calculateChecksum(file, "MD5")
            val sha1 = VaultCrypto.calculateChecksum(file, "SHA-1")
            val sha256 = VaultCrypto.calculateChecksum(file, "SHA-256")
            activeFileChecksums.value = mapOf(
                "MD5" to md5,
                "SHA-1" to sha1,
                "SHA-256" to sha256
            )
        }
    }

    fun openTextEditor(item: FileItem) {
        activeTextEditorFile = item
        viewModelScope.launch {
            val file = item.file ?: return@launch
            textEditorContent.value = fileRepository.readTextContent(file)
        }
    }

    fun saveTextEditor(newContent: String) {
        viewModelScope.launch {
            val file = activeTextEditorFile?.file ?: return@launch
            fileRepository.writeTextContent(file, newContent)
            activeTextEditorFile = null
            refreshCurrentDirectory()
        }
    }

    // Vault actions
    fun refreshVaultStatus() {
        val configured = vaultRepository.isVaultConfigured()
        _vaultState.update { it.copy(isConfigured = configured) }
    }

    fun setupVaultPasscode(passcode: String): Boolean {
        val success = vaultRepository.setupVaultPasscode(passcode)
        if (success) {
            _vaultState.update {
                it.copy(isConfigured = true, isUnlocked = true, currentPasscode = passcode, errorMessage = null)
            }
        }
        return success
    }

    fun unlockVault(passcode: String): Boolean {
        val valid = vaultRepository.verifyPasscode(passcode)
        if (valid) {
            _vaultState.update {
                it.copy(isUnlocked = true, currentPasscode = passcode, errorMessage = null)
            }
            return true
        } else {
            _vaultState.update { it.copy(errorMessage = "Incorrect Passcode") }
            return false
        }
    }

    fun lockVault() {
        _vaultState.update {
            it.copy(isUnlocked = false, currentPasscode = "", errorMessage = null)
        }
    }

    fun encryptSelectedToVault() {
        val passcode = _vaultState.value.currentPasscode
        if (passcode.isEmpty()) return
        viewModelScope.launch {
            val items = _explorerState.value.currentFiles.filter { it.path in _explorerState.value.selectedFiles }
            for (item in items) {
                val f = item.file ?: continue
                vaultRepository.encryptFileToVault(f, passcode, deleteOriginal = true)
            }
            clearSelection()
            refreshCurrentDirectory()
        }
    }

    fun decryptVaultItem(item: VaultItemEntity) {
        val passcode = _vaultState.value.currentPasscode
        if (passcode.isEmpty()) return
        viewModelScope.launch {
            vaultRepository.decryptFileFromVault(item.id, defaultRoot, passcode)
        }
    }

    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            vaultRepository.deleteVaultItemPermanently(item.id)
        }
    }

    // SAF actions
    fun handleSafTreeSelected(uri: Uri) {
        safRepository.takePersistablePermissions(uri)
        refreshStorageVolumes()
    }

    // Trash actions
    fun emptyTrash() {
        viewModelScope.launch {
            fileRepository.emptyTrash()
            refreshStorageAnalysis()
        }
    }
}
