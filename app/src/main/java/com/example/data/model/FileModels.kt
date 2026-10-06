package com.example.data.model

import android.net.Uri

enum class FileCategory(val title: String, val iconName: String) {
    ALL("All Files", "folder"),
    IMAGES("Images", "image"),
    VIDEOS("Videos", "video"),
    AUDIO("Audio", "music"),
    DOCUMENTS("Documents", "doc"),
    DOWNLOADS("Downloads", "download"),
    ARCHIVES("Archives", "archive"),
    APKS("APKs", "apk"),
    TRASH("Recycle Bin", "delete"),
    VAULT("Secure Vault", "lock")
}

enum class SortField {
    NAME, SIZE, DATE, TYPE
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

data class SortOption(
    val field: SortField = SortField.NAME,
    val direction: SortDirection = SortDirection.ASCENDING
)

enum class ViewLayout {
    LIST, GRID, COMPACT
}

enum class ClipboardOp {
    COPY, CUT
}

data class FileTag(
    val tag: String,
    val colorHex: String
)

data class FileItem(
    val file: java.io.File? = null,
    val uri: Uri? = null,
    val name: String,
    val path: String,
    val size: Long = 0L,
    val isDirectory: Boolean = false,
    val lastModified: Long = 0L,
    val extension: String = "",
    val mimeType: String = "*/*",
    val isHidden: Boolean = false,
    val isBookmarked: Boolean = false,
    val tags: List<FileTag> = emptyList(),
    val itemCount: Int = 0,
    val isSafDocument: Boolean = false
) {
    val formattedSize: String
        get() = formatFileSize(size)

    val sizeBytes: Long
        get() = size

    companion object {
        fun fromFile(file: java.io.File): FileItem {
            val ext = file.extension.lowercase()
            return FileItem(
                file = file,
                name = file.name,
                path = file.path,
                size = if (file.isDirectory) 0L else file.length(),
                isDirectory = file.isDirectory,
                lastModified = file.lastModified(),
                extension = ext,
                mimeType = "*/*",
                isHidden = file.name.startsWith("."),
                isBookmarked = false,
                itemCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0
            )
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val safeIndex = digitGroups.coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, safeIndex.toDouble())
            return if (safeIndex == 0) "$bytes B" else String.format("%.1f %s", value, units[safeIndex])
        }
    }
}

data class StorageVolume(
    val name: String,
    val path: String,
    val treeUri: Uri? = null,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val isPrimary: Boolean = false,
    val isSafMounted: Boolean = false
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    
    val formattedTotal: String
        get() = FileItem.formatFileSize(totalBytes)

    val formattedUsed: String
        get() = FileItem.formatFileSize(usedBytes)

    val formattedFree: String
        get() = FileItem.formatFileSize(freeBytes)
}

data class CategoryStat(
    val category: FileCategory,
    val fileCount: Int,
    val totalBytes: Long
) {
    val formattedSize: String
        get() = FileItem.formatFileSize(totalBytes)
}

data class StorageAnalysisResult(
    val totalStorageBytes: Long,
    val usedStorageBytes: Long,
    val freeStorageBytes: Long,
    val categoryStats: List<CategoryStat>,
    val largeFiles: List<FileItem>, // Files > 50MB
    val duplicateCandidates: List<List<FileItem>>, // Files sharing same size & name or hash
    val oldestFiles: List<FileItem>
)

data class FilterCriteria(
    val searchQuery: String = "",
    val selectedCategory: FileCategory = FileCategory.ALL,
    val showHidden: Boolean = false,
    val minSizeBytes: Long? = null,
    val maxSizeBytes: Long? = null,
    val extensions: Set<String> = emptySet(),
    val isRegexSearch: Boolean = false
)
