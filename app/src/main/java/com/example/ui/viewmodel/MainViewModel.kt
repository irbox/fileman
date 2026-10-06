package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.crypto.VaultCrypto
import com.example.data.db.*
import com.example.data.model.*
import com.example.data.repository.FileManagerRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.SafRepository
import com.example.data.repository.VaultRepository
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.DarkThemeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val errorMessage: String? = null,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "libre_files.db"
    ).fallbackToDestructiveMigration().build()

    val fileRepository = FileManagerRepository(application, database)
    val safRepository = SafRepository(application)
    val vaultRepository = VaultRepository(application, database)
    val preferencesRepository = PreferencesRepository(application)

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

    // Permission state
    val hasStoragePermission = MutableStateFlow(fileRepository.hasAllFilesAccess())

    // Vault State
    private val _vaultState = MutableStateFlow(VaultUiState())
    val vaultState: StateFlow<VaultUiState> = _vaultState.asStateFlow()

    // Clipboard (Copy / Cut / Paste)
    private val _clipboardItems = MutableStateFlow<List<FileItem>>(emptyList())
    val clipboardItems: StateFlow<List<FileItem>> = _clipboardItems.asStateFlow()

    private val _clipboardOp = MutableStateFlow<ClipboardOp?>(null)
    val clipboardOp: StateFlow<ClipboardOp?> = _clipboardOp.asStateFlow()

    // Active Dialogs & Viewers
    var activeTextEditorFile: FileItem? = null
    val textEditorContent = MutableStateFlow("")

    val activeImagePreviewFile = MutableStateFlow<FileItem?>(null)
    val activeZipFile = MutableStateFlow<FileItem?>(null)

    // Audio Player
    val activeAudioFile = MutableStateFlow<FileItem?>(null)
    val isAudioPlaying = MutableStateFlow(false)
    private var mediaPlayer: MediaPlayer? = null

    var activeDetailsFile: FileItem? = null
    val activeFileChecksums = MutableStateFlow<Map<String, String>>(emptyMap())

    // Theme Customization
    val isDarkMode = MutableStateFlow(true)
    val darkThemeStyle = MutableStateFlow(DarkThemeStyle.SLATE)
    val accentChoice = MutableStateFlow(AccentChoice.CYAN)

    val defaultRoot: File
        get() = fileRepository.getPrimaryStorageRoot()

    init {
        viewModelScope.launch {
            checkStoragePermissions()
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

        // Collect persisted DataStore preferences
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                isDarkMode.value = prefs.isDarkMode
                darkThemeStyle.value = prefs.darkThemeStyle
                accentChoice.value = prefs.accentChoice
                _explorerState.update {
                    it.copy(
                        viewLayout = prefs.viewLayout,
                        sortOption = SortOption(prefs.sortField, prefs.sortDirection),
                        filterCriteria = it.filterCriteria.copy(showHidden = prefs.showHiddenFiles)
                    )
                }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        viewModelScope.launch { preferencesRepository.setDarkMode(enabled) }
    }

    fun setDarkThemeStyle(style: DarkThemeStyle) {
        darkThemeStyle.value = style
        viewModelScope.launch { preferencesRepository.setDarkThemeStyle(style) }
    }

    fun setAccentChoice(choice: AccentChoice) {
        accentChoice.value = choice
        viewModelScope.launch { preferencesRepository.setAccentChoice(choice) }
    }

    fun checkStoragePermissions() {
        val hasPerm = fileRepository.hasAllFilesAccess()
        val prev = hasStoragePermission.value
        hasStoragePermission.value = hasPerm
        if (!prev && hasPerm) {
            onPermissionGranted()
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            refreshStorageVolumes()
            navigateToDirectory(defaultRoot.path)
            refreshStorageAnalysis()
            loadRecentFiles()
        }
    }

    fun requestManageStorage() {
        try {
            val intent = fileRepository.getManageStorageIntent()
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // Fallback to system settings
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

    fun loadRecentFiles() {
        viewModelScope.launch {
            _recentFiles.value = fileRepository.getRecentFiles()
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
            breadcrumbs.add(Pair("Storage", rootPath))

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
                breadcrumbs.add(Pair(File(path).name.ifEmpty { "Folder" }, path))
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

    fun refreshCurrentDirectory() {
        val curr = _explorerState.value.currentPath
        if (curr.isNotEmpty()) {
            navigateToDirectory(curr)
        } else {
            navigateToDirectory(defaultRoot.path)
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
                    isLoading = true,
                    selectedFiles = emptySet(),
                    isSelectionMode = false
                )
            }
            if (category == FileCategory.ALL) {
                navigateToDirectory(defaultRoot.path)
            } else {
                val catFiles = fileRepository.getCategoryFiles(category)
                _explorerState.update {
                    it.copy(
                        currentFiles = catFiles,
                        breadcrumbs = listOf(Pair("Storage", defaultRoot.path), Pair(category.title, "")),
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
        viewModelScope.launch { preferencesRepository.setSortOption(sortOption.field, sortOption.direction) }
        refreshCurrentDirectory()
    }

    fun setViewLayout(layout: ViewLayout) {
        _explorerState.update { it.copy(viewLayout = layout) }
        viewModelScope.launch { preferencesRepository.setViewLayout(layout) }
    }

    fun toggleViewLayout() {
        val next = when (_explorerState.value.viewLayout) {
            ViewLayout.LIST -> ViewLayout.GRID
            ViewLayout.GRID -> ViewLayout.COMPACT
            ViewLayout.COMPACT -> ViewLayout.LIST
        }
        _explorerState.update { it.copy(viewLayout = next) }
        viewModelScope.launch { preferencesRepository.setViewLayout(next) }
    }

    fun toggleSelection(path: String) {
        _explorerState.update { current ->
            val set = current.selectedFiles.toMutableSet()
            if (set.contains(path)) set.remove(path) else set.add(path)
            current.copy(selectedFiles = set, isSelectionMode = set.isNotEmpty())
        }
    }

    fun selectAll() {
        _explorerState.update { current ->
            val allPaths = current.currentFiles.map { it.path }.toSet()
            current.copy(selectedFiles = allPaths, isSelectionMode = allPaths.isNotEmpty())
        }
    }

    fun clearSelection() {
        _explorerState.update { it.copy(selectedFiles = emptySet(), isSelectionMode = false) }
    }

    // Clipboard Copy / Cut / Paste
    fun copySelectedToClipboard() {
        val selected = getSelectedFileItems()
        if (selected.isNotEmpty()) {
            _clipboardItems.value = selected
            _clipboardOp.value = ClipboardOp.COPY
            clearSelection()
            _explorerState.update { it.copy(statusMessage = "Copied ${selected.size} items to clipboard") }
        }
    }

    fun cutSelectedToClipboard() {
        val selected = getSelectedFileItems()
        if (selected.isNotEmpty()) {
            _clipboardItems.value = selected
            _clipboardOp.value = ClipboardOp.CUT
            clearSelection()
            _explorerState.update { it.copy(statusMessage = "Ready to move ${selected.size} items") }
        }
    }

    fun cancelClipboard() {
        _clipboardItems.value = emptyList()
        _clipboardOp.value = null
    }

    fun pasteClipboard() {
        val targetPath = _explorerState.value.currentPath
        val items = _clipboardItems.value
        val op = _clipboardOp.value ?: return
        if (items.isEmpty() || targetPath.isEmpty()) return

        viewModelScope.launch {
            _explorerState.update { it.copy(isLoading = true) }
            val count = if (op == ClipboardOp.COPY) {
                fileRepository.batchCopy(items, targetPath)
            } else {
                fileRepository.batchMove(items, targetPath)
            }
            _clipboardItems.value = emptyList()
            _clipboardOp.value = null
            _explorerState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = if (op == ClipboardOp.COPY) "Copied $count items" else "Moved $count items"
                )
            }
            refreshCurrentDirectory()
        }
    }

    fun getSelectedFileItems(): List<FileItem> {
        val selected = _explorerState.value.selectedFiles
        return _explorerState.value.currentFiles.filter { selected.contains(it.path) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val curr = _explorerState.value.currentPath
            if (curr.isNotEmpty()) {
                fileRepository.createDirectory(curr, name)
                refreshCurrentDirectory()
            }
        }
    }

    fun createNewFile(name: String, content: String = "") {
        viewModelScope.launch {
            val curr = _explorerState.value.currentPath
            if (curr.isNotEmpty()) {
                fileRepository.createFile(curr, name, content)
                refreshCurrentDirectory()
            }
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            fileRepository.renameFile(oldPath, newName)
            refreshCurrentDirectory()
        }
    }

    fun batchRename(prefix: String, suffix: String, find: String, replace: String, numbering: Boolean) {
        viewModelScope.launch {
            val items = getSelectedFileItems()
            if (items.isNotEmpty()) {
                fileRepository.batchRename(items, prefix, suffix, find, replace, numbering)
                clearSelection()
                refreshCurrentDirectory()
            }
        }
    }

    fun deleteSelected(permanent: Boolean = false) {
        viewModelScope.launch {
            val items = getSelectedFileItems()
            if (items.isNotEmpty()) {
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
    }

    fun restoreFromTrash(trashId: String) {
        viewModelScope.launch {
            fileRepository.restoreFromTrash(trashId)
            refreshCurrentDirectory()
            refreshStorageAnalysis()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            fileRepository.emptyTrash()
            refreshStorageAnalysis()
        }
    }

    fun cleanAppCache() {
        viewModelScope.launch {
            val reclaimed = fileRepository.cleanAppCache()
            _explorerState.update {
                it.copy(statusMessage = "Optimized ${FileItem.formatFileSize(reclaimed)} of cache & junk")
            }
            refreshStorageAnalysis()
        }
    }

    fun compressSelectedToZip(zipName: String) {
        viewModelScope.launch {
            val items = getSelectedFileItems()
            val curr = _explorerState.value.currentPath
            if (items.isNotEmpty() && curr.isNotEmpty()) {
                fileRepository.createZipArchive(items, zipName, curr)
                clearSelection()
                refreshCurrentDirectory()
            }
        }
    }

    fun extractZip(zipFile: FileItem) {
        val target = _explorerState.value.currentPath
        val file = zipFile.file ?: return
        viewModelScope.launch {
            _explorerState.update { it.copy(isLoading = true) }
            val ok = fileRepository.extractZipArchive(file, target)
            _explorerState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = if (ok) "Extracted ${file.name}" else "Extraction failed"
                )
            }
            refreshCurrentDirectory()
        }
    }

    // Opening Files
    fun openFile(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            navigateToDirectory(fileItem.path)
            return
        }

        val ext = fileItem.extension.lowercase()
        when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> {
                activeImagePreviewFile.value = fileItem
            }
            "mp3", "flac", "wav", "m4a", "ogg", "aac", "mid" -> {
                playAudio(fileItem)
            }
            "txt", "md", "json", "xml", "kt", "java", "csv", "html", "css", "js", "log", "py", "sh" -> {
                openTextEditor(fileItem)
            }
            "zip" -> {
                activeZipFile.value = fileItem
            }
            "apk" -> {
                installApk(fileItem)
            }
            else -> {
                val f = fileItem.file ?: File(fileItem.path)
                if (f.exists()) {
                    fileRepository.openWithExternalApp(f)
                }
            }
        }
    }

    fun installApk(fileItem: FileItem) {
        val file = fileItem.file ?: File(fileItem.path)
        if (!file.exists()) return
        try {
            val app = getApplication<Application>()
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        } catch (e: Exception) {
            _explorerState.update { it.copy(statusMessage = "Cannot launch package installer: ${e.message}") }
        }
    }

    // Audio Player Controls
    fun playAudio(item: FileItem) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (item.file != null && item.file.exists()) {
                    setDataSource(item.file.path)
                } else if (item.uri != null) {
                    setDataSource(getApplication(), item.uri)
                } else {
                    setDataSource(item.path)
                }
                prepare()
                start()
                setOnCompletionListener {
                    isAudioPlaying.value = false
                }
            }
            activeAudioFile.value = item
            isAudioPlaying.value = true
        } catch (e: Exception) {
            _explorerState.update { it.copy(statusMessage = "Could not play audio: ${e.message}") }
        }
    }

    fun toggleAudioPlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isAudioPlaying.value = false
            } else {
                player.start()
                isAudioPlaying.value = true
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        activeAudioFile.value = null
        isAudioPlaying.value = false
    }

    // Text Editor
    fun openTextEditor(fileItem: FileItem) {
        val file = fileItem.file ?: File(fileItem.path)
        if (file.exists()) {
            activeTextEditorFile = fileItem
            viewModelScope.launch {
                textEditorContent.value = fileRepository.readTextContent(file)
            }
        }
    }

    fun saveTextEditor(newContent: String) {
        val file = activeTextEditorFile?.file ?: return
        viewModelScope.launch {
            fileRepository.writeTextContent(file, newContent)
            activeTextEditorFile = null
            refreshCurrentDirectory()
        }
    }

    // Details & Checksum calculation
    fun showFileDetails(fileItem: FileItem) {
        activeDetailsFile = fileItem
        activeFileChecksums.value = emptyMap()
        viewModelScope.launch {
            val file = fileItem.file ?: File(fileItem.path)
            if (file.exists() && file.isFile) {
                val md5 = withContext(Dispatchers.IO) { VaultCrypto.calculateChecksum(file, "MD5") }
                val sha1 = withContext(Dispatchers.IO) { VaultCrypto.calculateChecksum(file, "SHA-1") }
                val sha256 = withContext(Dispatchers.IO) { VaultCrypto.calculateChecksum(file, "SHA-256") }
                activeFileChecksums.value = mapOf(
                    "MD5" to md5,
                    "SHA-1" to sha1,
                    "SHA-256" to sha256
                )
            }
        }
    }

    // Share File
    fun shareFile(fileItem: FileItem) {
        val file = fileItem.file ?: File(fileItem.path)
        if (!file.exists()) return
        try {
            val app = getApplication<Application>()
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = fileItem.mimeType.ifEmpty { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share ${fileItem.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Share Multiple Selected Files
    fun shareSelectedFiles() {
        val selectedPaths = _explorerState.value.selectedFiles.toList()
        if (selectedPaths.isEmpty()) return
        val app = getApplication<Application>()
        val uris = ArrayList<Uri>()
        for (path in selectedPaths) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                try {
                    val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
                    uris.add(uri)
                } catch (e: Exception) {
                    // Ignore individual file provider failure
                }
            }
        }
        if (uris.isEmpty()) return
        try {
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val chooser = Intent.createChooser(intent, "Share ${uris.size} files").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Secure Vault Operations
    fun refreshVaultStatus() {
        val configured = vaultRepository.isVaultConfigured()
        val unlocked = _vaultState.value.isUnlocked
        val bioAvail = com.example.data.crypto.BiometricAuthHelper.isBiometricAvailable(getApplication())
        val bioEnabled = vaultRepository.isBiometricUnlockEnabled()
        _vaultState.update {
            it.copy(
                isConfigured = configured,
                isUnlocked = unlocked,
                isBiometricAvailable = bioAvail,
                isBiometricEnabled = bioEnabled
            )
        }
    }

    fun setupVaultPasscode(passcode: String) {
        val ok = vaultRepository.setupVaultPasscode(passcode)
        if (ok) {
            val bioAvail = com.example.data.crypto.BiometricAuthHelper.isBiometricAvailable(getApplication())
            _vaultState.update {
                it.copy(
                    isConfigured = true,
                    isUnlocked = true,
                    currentPasscode = passcode,
                    isBiometricAvailable = bioAvail,
                    isBiometricEnabled = true,
                    errorMessage = null
                )
            }
        } else {
            _vaultState.update { it.copy(errorMessage = "Failed to initialize vault") }
        }
    }

    fun unlockVault(passcode: String) {
        val success = vaultRepository.verifyPasscode(passcode)
        if (success) {
            val bioAvail = com.example.data.crypto.BiometricAuthHelper.isBiometricAvailable(getApplication())
            val bioEnabled = vaultRepository.isBiometricUnlockEnabled()
            _vaultState.update {
                it.copy(
                    isUnlocked = true,
                    currentPasscode = passcode,
                    isBiometricAvailable = bioAvail,
                    isBiometricEnabled = bioEnabled,
                    errorMessage = null
                )
            }
        } else {
            _vaultState.update { it.copy(errorMessage = "Incorrect passcode") }
        }
    }

    fun unlockVaultWithBiometrics(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val passcode = vaultRepository.getPasscodeFromBiometrics()
        if (passcode != null) {
            unlockVault(passcode)
            onSuccess()
        } else {
            val msg = "Biometric credentials not available. Please enter your passcode."
            _vaultState.update { it.copy(errorMessage = msg) }
            onError(msg)
        }
    }

    fun toggleBiometricUnlock(enabled: Boolean) {
        if (enabled) {
            val passcode = _vaultState.value.currentPasscode
            if (passcode.isNotEmpty()) {
                val ok = vaultRepository.enableBiometricUnlock(passcode)
                _vaultState.update { it.copy(isBiometricEnabled = ok) }
            }
        } else {
            vaultRepository.disableBiometricUnlock()
            _vaultState.update { it.copy(isBiometricEnabled = false) }
        }
    }

    fun lockVault() {
        _vaultState.update { it.copy(isUnlocked = false, currentPasscode = "") }
    }

    fun importFileToVault(fileItem: FileItem) {
        val file = fileItem.file ?: File(fileItem.path)
        val passcode = _vaultState.value.currentPasscode
        if (file.exists() && passcode.isNotEmpty()) {
            viewModelScope.launch {
                vaultRepository.encryptFileToVault(file, passcode, deleteOriginal = true)
                refreshCurrentDirectory()
            }
        }
    }

    fun exportVaultItem(item: VaultItemEntity, targetDir: String) {
        val passcode = _vaultState.value.currentPasscode
        if (passcode.isNotEmpty()) {
            viewModelScope.launch {
                vaultRepository.decryptFileFromVault(item.id, File(targetDir), passcode)
                refreshCurrentDirectory()
            }
        }
    }

    fun decryptVaultItem(item: VaultItemEntity) {
        exportVaultItem(item, defaultRoot.path)
    }

    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            vaultRepository.deleteVaultItemPermanently(item.id)
        }
    }

    // SAF Document Tree
    fun handleSafTreeSelected(uri: Uri) {
        viewModelScope.launch {
            safRepository.takePersistablePermissions(uri)
            refreshStorageVolumes()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
