package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileManagerRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val bookmarkDao = database.bookmarkDao()
    private val fileTagDao = database.fileTagDao()
    private val trashDao = database.trashDao()
    private val auditLogDao = database.auditLogDao()
    private val mediaStoreHelper = MediaStoreHelper(context)

    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val allTags: Flow<List<FileTagEntity>> = fileTagDao.getAllTags()
    val trashItems: Flow<List<TrashItemEntity>> = trashDao.getAllTrashItems()
    val auditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogs()

    private val trashDir: File by lazy {
        File(context.filesDir, "libre_trash").apply { if (!exists()) mkdirs() }
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun getManageStorageIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun getPrimaryStorageRoot(): File {
        val ext = Environment.getExternalStorageDirectory()
        return if (ext != null && ext.exists()) {
            ext
        } else {
            context.filesDir
        }
    }

    suspend fun getStorageVolumes(): List<StorageVolume> = withContext(Dispatchers.IO) {
        val volumes = mutableListOf<StorageVolume>()

        // 1. Primary Device Storage (/storage/emulated/0)
        try {
            val root = Environment.getExternalStorageDirectory()
            if (root.exists()) {
                val stat = StatFs(root.path)
                val totalBytes = stat.blockCountLong * stat.blockSizeLong
                val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
                val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

                volumes.add(
                    StorageVolume(
                        name = "Internal Storage",
                        path = root.path,
                        totalBytes = totalBytes,
                        freeBytes = freeBytes,
                        usedBytes = usedBytes,
                        isPrimary = true
                    )
                )
            }
        } catch (e: Exception) {
            val fallback = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = StatFs(fallback.path)
            volumes.add(
                StorageVolume(
                    name = "Internal Storage",
                    path = fallback.path,
                    totalBytes = stat.blockCountLong * stat.blockSizeLong,
                    freeBytes = stat.availableBlocksLong * stat.blockSizeLong,
                    usedBytes = (stat.blockCountLong - stat.availableBlocksLong) * stat.blockSizeLong,
                    isPrimary = true
                )
            )
        }

        // 2. Secondary SD card / External storage if present
        try {
            val dirs = context.getExternalFilesDirs(null)
            if (dirs.size > 1 && dirs[1] != null) {
                val sdDir = dirs[1]
                val stat = StatFs(sdDir.path)
                val totalBytes = stat.blockCountLong * stat.blockSizeLong
                val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
                volumes.add(
                    StorageVolume(
                        name = "SD Card",
                        path = sdDir.path,
                        totalBytes = totalBytes,
                        freeBytes = freeBytes,
                        usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L),
                        isPrimary = false
                    )
                )
            }
        } catch (e: Exception) {
            // No secondary SD card
        }

        volumes
    }

    suspend fun listFiles(
        directoryPath: String,
        filter: FilterCriteria = FilterCriteria(),
        sortOption: SortOption = SortOption()
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val rawFiles = dir.listFiles() ?: return@withContext emptyList()

        val filtered = rawFiles.filter { file ->
            if (!filter.showHidden && file.name.startsWith(".")) return@filter false

            if (filter.searchQuery.isNotBlank()) {
                val matches = if (filter.isRegexSearch) {
                    try {
                        Regex(filter.searchQuery, RegexOption.IGNORE_CASE).containsMatchIn(file.name)
                    } catch (e: Exception) {
                        file.name.contains(filter.searchQuery, ignoreCase = true)
                    }
                } else {
                    file.name.contains(filter.searchQuery, ignoreCase = true)
                }
                if (!matches) return@filter false
            }

            if (filter.selectedCategory != FileCategory.ALL && !file.isDirectory) {
                val cat = getFileCategory(file)
                if (cat != filter.selectedCategory) return@filter false
            }

            if (filter.minSizeBytes != null && file.length() < filter.minSizeBytes) return@filter false
            if (filter.maxSizeBytes != null && file.length() > filter.maxSizeBytes) return@filter false

            true
        }

        val items = filtered.map { file ->
            val ext = file.extension.lowercase()
            val mime = getMimeType(file)
            val itemCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0

            FileItem(
                file = file,
                name = file.name,
                path = file.path,
                size = if (file.isDirectory) calculateDirectorySize(file) else file.length(),
                isDirectory = file.isDirectory,
                lastModified = file.lastModified(),
                extension = ext,
                mimeType = mime,
                isHidden = file.name.startsWith("."),
                itemCount = itemCount
            )
        }

        items.sortedWith(Comparator { a, b ->
            if (a.isDirectory && !b.isDirectory) return@Comparator -1
            if (!a.isDirectory && b.isDirectory) return@Comparator 1

            val comparison = when (sortOption.field) {
                SortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortField.SIZE -> a.size.compareTo(b.size)
                SortField.DATE -> a.lastModified.compareTo(b.lastModified)
                SortField.TYPE -> a.extension.compareTo(b.extension, ignoreCase = true)
            }

            if (sortOption.direction == SortDirection.ASCENDING) comparison else -comparison
        })
    }

    suspend fun getRecentFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val fromMediaStore = mediaStoreHelper.queryRecentFiles(limit = 20)
        if (fromMediaStore.isNotEmpty()) {
            return@withContext fromMediaStore
        }

        // Fallback: scan primary storage root
        val root = getPrimaryStorageRoot()
        val scanned = mutableListOf<FileItem>()
        root.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.take(15)?.forEach { f ->
            scanned.add(
                FileItem(
                    file = f,
                    name = f.name,
                    path = f.path,
                    size = f.length(),
                    isDirectory = false,
                    lastModified = f.lastModified(),
                    extension = f.extension.lowercase(),
                    mimeType = getMimeType(f)
                )
            )
        }
        scanned.sortedByDescending { it.lastModified }
    }

    suspend fun getCategoryFiles(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        mediaStoreHelper.queryCategory(category)
    }

    suspend fun analyzeStorage(): StorageAnalysisResult = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        var usedBytes = 0L
        val catCounts = mutableMapOf<FileCategory, Pair<Int, Long>>()
        val allFiles = mutableListOf<FileItem>()
        val emptyDirs = mutableListOf<FileItem>()

        FileCategory.entries.forEach {
            catCounts[it] = Pair(0, 0L)
        }

        val root = getPrimaryStorageRoot()
        try {
            val stat = StatFs(root.path)
            totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        } catch (e: Exception) {
            val stat = StatFs(context.filesDir.path)
            totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        }

        // Quick scan of main user directories for analyzer
        val scanRoots = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        ).filter { it.exists() }

        fun scanDir(dir: File, depth: Int = 0) {
            if (depth > 3) return
            val children = dir.listFiles() ?: return
            if (children.isEmpty()) {
                emptyDirs.add(
                    FileItem(
                        file = dir,
                        name = dir.name,
                        path = dir.path,
                        size = 0L,
                        isDirectory = true,
                        lastModified = dir.lastModified()
                    )
                )
                return
            }

            children.take(60).forEach { file ->
                if (file.isDirectory) {
                    scanDir(file, depth + 1)
                } else {
                    val size = file.length()
                    val cat = getFileCategory(file)
                    val current = catCounts[cat] ?: Pair(0, 0L)
                    catCounts[cat] = Pair(current.first + 1, current.second + size)

                    allFiles.add(
                        FileItem(
                            file = file,
                            name = file.name,
                            path = file.path,
                            size = size,
                            isDirectory = false,
                            lastModified = file.lastModified(),
                            extension = file.extension.lowercase(),
                            mimeType = getMimeType(file)
                        )
                    )
                }
            }
        }

        scanRoots.forEach { scanDir(it) }

        val categoryStats = catCounts.map { (cat, data) ->
            CategoryStat(category = cat, fileCount = data.first, totalBytes = data.second)
        }.filter { it.category != FileCategory.ALL }

        val largeFiles = allFiles.filter { it.size > 15L * 1024 * 1024 }
            .sortedByDescending { it.size }.take(20)

        val oldestFiles = allFiles.sortedBy { it.lastModified }.take(10)

        // Find duplicates by size + name
        val grouped = allFiles.groupBy { "${it.size}_${it.name.lowercase()}" }
        val duplicates = grouped.values.filter { it.size > 1 }

        StorageAnalysisResult(
            totalStorageBytes = totalBytes,
            usedStorageBytes = usedBytes,
            freeStorageBytes = (totalBytes - usedBytes).coerceAtLeast(0L),
            categoryStats = categoryStats,
            largeFiles = if (largeFiles.isNotEmpty()) largeFiles else allFiles.sortedByDescending { it.size }.take(10),
            duplicateCandidates = duplicates,
            oldestFiles = oldestFiles
        )
    }

    suspend fun cleanAppCache(): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L
        val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
        cacheDirs.forEach { cDir ->
            cDir.listFiles()?.forEach { file ->
                reclaimed += if (file.isDirectory) calculateDirectorySize(file) else file.length()
                file.deleteRecursively()
            }
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "CLEAN_CACHE",
                target = "App Cache",
                details = "Cleaned ${FileItem.formatFileSize(reclaimed)} of temporary data"
            )
        )
        reclaimed
    }

    suspend fun createDirectory(parentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(parentPath, name)
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (created) {
                auditLogDao.insertLog(
                    AuditLogEntity(action = "CREATE_DIR", target = dir.path, details = "Directory created: $name")
                )
            }
            created
        } else false
    }

    suspend fun createFile(parentPath: String, name: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        val file = File(parentPath, name)
        if (!file.exists()) {
            val created = file.createNewFile()
            if (created) {
                if (content.isNotEmpty()) file.writeText(content)
                auditLogDao.insertLog(
                    AuditLogEntity(action = "CREATE_FILE", target = file.path, details = "File created: $name")
                )
            }
            created
        } else false
    }

    suspend fun renameFile(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(oldPath)
        val target = File(file.parentFile, newName)
        if (file.exists() && !target.exists()) {
            val renamed = file.renameTo(target)
            if (renamed) {
                auditLogDao.insertLog(
                    AuditLogEntity(action = "RENAME", target = target.path, details = "Renamed from ${file.name} to $newName")
                )
            }
            renamed
        } else false
    }

    suspend fun batchRename(
        items: List<FileItem>,
        prefix: String,
        suffix: String,
        findText: String,
        replaceText: String,
        sequentialNumbering: Boolean
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        items.forEachIndexed { index, item ->
            val file = item.file ?: return@forEachIndexed
            val ext = if (item.isDirectory || item.extension.isEmpty()) "" else ".${item.extension}"
            val baseName = if (item.isDirectory) item.name else item.name.substringBeforeLast(".")

            var newBaseName = baseName
            if (findText.isNotEmpty()) {
                newBaseName = newBaseName.replace(findText, replaceText)
            }
            val numStr = if (sequentialNumbering) "_%03d".format(index + 1) else ""
            val finalName = "$prefix$newBaseName$suffix$numStr$ext"

            val target = File(file.parentFile, finalName)
            if (file.exists() && file != target && !target.exists()) {
                if (file.renameTo(target)) count++
            }
        }
        if (count > 0) {
            auditLogDao.insertLog(
                AuditLogEntity(action = "BATCH_RENAME", target = "Multiple", details = "Renamed $count items")
            )
        }
        count
    }

    suspend fun moveToTrash(items: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var count = 0
        items.forEach { item ->
            val file = item.file ?: return@forEach
            val trashId = UUID.randomUUID().toString()
            val trashDest = File(trashDir, "${trashId}_${file.name}")
            if (file.renameTo(trashDest)) {
                trashDao.insertTrash(
                    TrashItemEntity(
                        id = trashId,
                        originalPath = file.path,
                        fileName = file.name,
                        trashFileName = trashDest.name,
                        sizeBytes = item.size
                    )
                )
                count++
            }
        }
        if (count > 0) {
            auditLogDao.insertLog(
                AuditLogEntity(action = "MOVE_TO_TRASH", target = "Recycle Bin", details = "Moved $count items to trash")
            )
        }
        count
    }

    suspend fun restoreFromTrash(trashId: String): Boolean = withContext(Dispatchers.IO) {
        val matched = trashDir.listFiles()?.firstOrNull { it.name.startsWith(trashId) } ?: return@withContext false
        val originalName = matched.name.substringAfter("${trashId}_")
        val target = File(getPrimaryStorageRoot(), originalName)
        if (matched.renameTo(target)) {
            trashDao.deleteTrash(trashId)
            auditLogDao.insertLog(
                AuditLogEntity(action = "RESTORE_TRASH", target = target.path, details = "Restored $originalName")
            )
            true
        } else false
    }

    suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { it.deleteRecursively() }
        trashDao.clearTrash()
        auditLogDao.insertLog(
            AuditLogEntity(action = "EMPTY_TRASH", target = "Recycle Bin", details = "Permanently cleared recycle bin")
        )
        true
    }

    suspend fun deletePermanently(items: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var count = 0
        items.forEach { item ->
            val file = item.file ?: return@forEach
            if (file.deleteRecursively()) count++
        }
        if (count > 0) {
            auditLogDao.insertLog(
                AuditLogEntity(action = "DELETE_PERM", target = "Multiple", details = "Permanently deleted $count items")
            )
        }
        count
    }

    suspend fun batchCopy(items: List<FileItem>, destinationDir: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        val dest = File(destinationDir)
        if (!dest.exists() || !dest.isDirectory) return@withContext 0

        items.forEach { item ->
            val file = item.file ?: return@forEach
            val target = File(dest, file.name)
            try {
                if (file.isDirectory) {
                    file.copyRecursively(target, overwrite = true)
                } else {
                    file.copyTo(target, overwrite = true)
                }
                count++
            } catch (e: Exception) {
                // Ignore failure
            }
        }
        if (count > 0) {
            auditLogDao.insertLog(
                AuditLogEntity(action = "BATCH_COPY", target = destinationDir, details = "Copied $count items")
            )
        }
        count
    }

    suspend fun batchMove(items: List<FileItem>, destinationDir: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        val dest = File(destinationDir)
        if (!dest.exists() || !dest.isDirectory) return@withContext 0

        items.forEach { item ->
            val file = item.file ?: return@forEach
            val target = File(dest, file.name)
            if (file.renameTo(target)) count++
        }
        if (count > 0) {
            auditLogDao.insertLog(
                AuditLogEntity(action = "BATCH_MOVE", target = destinationDir, details = "Moved $count items")
            )
        }
        count
    }

    suspend fun createZipArchive(items: List<FileItem>, zipName: String, outputDir: String): File? = withContext(Dispatchers.IO) {
        val destFile = File(outputDir, if (zipName.endsWith(".zip")) zipName else "$zipName.zip")
        try {
            ZipOutputStream(FileOutputStream(destFile)).use { zos ->
                items.forEach { item ->
                    val file = item.file ?: return@forEach
                    compressFileToZip(file, file.name, zos)
                }
            }
            auditLogDao.insertLog(
                AuditLogEntity(action = "ZIP_CREATE", target = destFile.path, details = "Compressed ${items.size} items to ${destFile.name}")
            )
            destFile
        } catch (e: Exception) {
            null
        }
    }

    private fun compressFileToZip(file: File, baseName: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            if (children.isEmpty()) {
                zos.putNextEntry(ZipEntry("$baseName/"))
                zos.closeEntry()
            } else {
                children.forEach { child ->
                    compressFileToZip(child, "$baseName/${child.name}", zos)
                }
            }
        } else {
            FileInputStream(file).use { fis ->
                zos.putNextEntry(ZipEntry(baseName))
                fis.copyTo(zos, bufferSize = 4096)
                zos.closeEntry()
            }
        }
    }

    suspend fun extractZipArchive(zipFile: File, outputDir: String): Boolean = withContext(Dispatchers.IO) {
        val outDir = File(outputDir)
        if (!outDir.exists()) outDir.mkdirs()

        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(outDir, entry.name)
                    if (!newFile.canonicalPath.startsWith(outDir.canonicalPath)) {
                        throw SecurityException("Zip Slip detected: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos, bufferSize = 4096)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            auditLogDao.insertLog(
                AuditLogEntity(action = "ZIP_EXTRACT", target = outputDir, details = "Extracted ${zipFile.name}")
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openWithExternalApp(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val mime = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // No app found or permission error
        }
    }

    suspend fun readTextContent(file: File): String = withContext(Dispatchers.IO) {
        try {
            file.readText()
        } catch (e: Exception) {
            "Unable to read file: ${e.message}"
        }
    }

    suspend fun writeTextContent(file: File, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            file.writeText(content)
            auditLogDao.insertLog(
                AuditLogEntity(action = "EDIT_FILE", target = file.path, details = "Saved edits to ${file.name}")
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleBookmark(fileItem: FileItem): Boolean = withContext(Dispatchers.IO) {
        if (fileItem.isBookmarked) {
            bookmarkDao.deleteBookmark(fileItem.path)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    path = fileItem.path,
                    name = fileItem.name,
                    isDirectory = fileItem.isDirectory
                )
            )
            true
        }
    }

    suspend fun addTag(path: String, tag: String, colorHex: String) = withContext(Dispatchers.IO) {
        fileTagDao.insertTag(FileTagEntity(path = path, tag = tag, colorHex = colorHex))
    }

    suspend fun removeTag(path: String, tag: String) = withContext(Dispatchers.IO) {
        fileTagDao.deleteTag(path, tag)
    }

    fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.take(50)?.forEach { file ->
            size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
        }
        return size
    }

    fun getFileCategory(file: File): FileCategory {
        val ext = file.extension.lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileCategory.IMAGES
            "mp4", "mkv", "mov", "avi", "3gp", "webm" -> FileCategory.VIDEOS
            "mp3", "flac", "wav", "m4a", "ogg", "aac", "mid" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml", "kt", "html", "epub" -> FileCategory.DOCUMENTS
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> FileCategory.ARCHIVES
            "apk", "xapk", "apks" -> FileCategory.APKS
            else -> FileCategory.ALL
        }
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "md", "kt", "csv" -> "text/plain"
            "json" -> "application/json"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }
}
