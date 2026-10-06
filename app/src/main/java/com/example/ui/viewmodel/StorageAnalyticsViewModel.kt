package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryStat
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.StorageAnalysisResult
import com.example.data.repository.DiskCategoryUsage
import com.example.data.repository.DiskUsageHelper
import com.example.data.repository.DiskUsageSummary
import com.example.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUsageSegment(
    val category: FileCategory,
    val fileCount: Int,
    val sizeBytes: Long,
    val formattedSize: String,
    val percentageOfUsed: Float
)

data class StorageAnalyticsUiState(
    val isLoading: Boolean = true,
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedPercentage: Float = 0f,
    val categorySegments: List<CategoryUsageSegment> = emptyList(),
    val diskCategories: List<DiskCategoryUsage> = emptyList(),
    val diskSummary: DiskUsageSummary? = null,
    val largeFiles: List<FileItem> = emptyList(),
    val duplicateCandidates: List<List<FileItem>> = emptyList(),
    val oldestFiles: List<FileItem> = emptyList(),
    val reclaimedCacheBytes: Long? = null,
    val errorMessage: String? = null
)

class StorageAnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "libre_files.db"
    ).fallbackToDestructiveMigration().build()

    val fileRepository = FileManagerRepository(application, database)

    private val _uiState = MutableStateFlow(StorageAnalyticsUiState())
    val uiState: StateFlow<StorageAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadStorageAnalytics()
    }

    fun loadStorageAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Scan disk usage categorizing by Images, Video, Documents, Others
                val diskSummary = DiskUsageHelper.scanDiskUsage(getApplication())
                val result = fileRepository.analyzeStorage()

                val used = if (diskSummary.usedStorageBytes > 0) diskSummary.usedStorageBytes else result.usedStorageBytes
                val total = if (diskSummary.totalStorageBytes > 0) diskSummary.totalStorageBytes else result.totalStorageBytes
                val usedPct = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

                val segments = result.categoryStats.map { stat ->
                    val catPct = if (used > 0) (stat.totalBytes.toFloat() / used.toFloat()).coerceIn(0f, 1f) else 0f
                    CategoryUsageSegment(
                        category = stat.category,
                        fileCount = stat.fileCount,
                        sizeBytes = stat.totalBytes,
                        formattedSize = stat.formattedSize,
                        percentageOfUsed = catPct
                    )
                }.sortedByDescending { it.sizeBytes }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalBytes = total,
                        usedBytes = used,
                        freeBytes = diskSummary.freeStorageBytes,
                        usedPercentage = usedPct,
                        categorySegments = segments,
                        diskCategories = diskSummary.categories,
                        diskSummary = diskSummary,
                        largeFiles = if (diskSummary.largestFiles.isNotEmpty()) diskSummary.largestFiles else result.largeFiles,
                        duplicateCandidates = result.duplicateCandidates,
                        oldestFiles = result.oldestFiles
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to analyze storage")
                }
            }
        }
    }

    fun cleanAppCache() {
        viewModelScope.launch {
            val reclaimed = fileRepository.cleanAppCache()
            _uiState.update { it.copy(reclaimedCacheBytes = reclaimed) }
            loadStorageAnalytics()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            fileRepository.emptyTrash()
            loadStorageAnalytics()
        }
    }

    fun openFile(fileItem: FileItem) {
        val f = fileItem.file ?: java.io.File(fileItem.path)
        if (f.exists()) {
            fileRepository.openWithExternalApp(f)
        }
    }
}
