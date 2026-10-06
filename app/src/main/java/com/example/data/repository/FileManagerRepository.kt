package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.example.data.crypto.VaultCrypto
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

    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val allTags: Flow<List<FileTagEntity>> = fileTagDao.getAllTags()
    val trashItems: Flow<List<TrashItemEntity>> = trashDao.getAllTrashItems()
    val auditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogs()

    private val trashDir: File by lazy {
        File(context.filesDir, "libre_trash").apply { if (!exists()) mkdirs() }
    }

    private val sampleDataDir: File by lazy {
        File(context.filesDir, "Local_Storage").apply { if (!exists()) mkdirs() }
    }

    suspend fun getStorageVolumes(): List<StorageVolume> = withContext(Dispatchers.IO) {
        val volumes = mutableListOf<StorageVolume>()

        // Primary Storage (App sandbox / external files)
        try {
            val extDir = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = StatFs(extDir.path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            volumes.add(
                StorageVolume(
                    name = "Main Storage",
                    path = extDir.path,
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    usedBytes = usedBytes,
                    isPrimary = true
                )
            )
        } catch (e: Exception) {
            val stat = StatFs(context.filesDir.path)
            volumes.add(
                StorageVolume(
                    name = "Internal Storage",
                    path = context.filesDir.path,
                    totalBytes = stat.blockCountLong * stat.blockSizeLong,
                    freeBytes = stat.availableBlocksLong * stat.blockSizeLong,
                    usedBytes = (stat.blockCountLong - stat.availableBlocksLong) * stat.blockSizeLong,
                    isPrimary = true
                )
            )
        }

        // Secondary / App Workspace Volume
        val localStat = StatFs(sampleDataDir.path)
        volumes.add(
            StorageVolume(
                name = "Libre Workspace",
                path = sampleDataDir.path,
                totalBytes = localStat.blockCountLong * localStat.blockSizeLong,
                freeBytes = localStat.availableBlocksLong * localStat.blockSizeLong,
                usedBytes = (localStat.blockCountLong - localStat.availableBlocksLong) * localStat.blockSizeLong,
                isPrimary = false
            )
        )

        volumes
    }

    suspend fun initializeSampleFilesIfEmpty() = withContext(Dispatchers.IO) {
        if (sampleDataDir.listFiles().isNullOrEmpty()) {
            val docs = File(sampleDataDir, "Documents").apply { mkdirs() }
            val photos = File(sampleDataDir, "Photos").apply { mkdirs() }
            val projects = File(sampleDataDir, "Projects").apply { mkdirs() }
            val archives = File(sampleDataDir, "Archives").apply { mkdirs() }

            File(docs, "Project_Manifesto.md").writeText(
                """# LibreFiles — Open Source CX File Explorer
### Privacy & Freedom Manifesto
- Zero analytics, zero telemetry, zero data harvesting.
- 100% Offline-first local data sovereignty.
- Built-in AES-256-GCM encrypted vault.
- Modern Material 3 expressive UI with gesture navigation.
- Licensed under GPL v3 for community auditing and F-Droid packaging.
""".trimIndent()
            )

            File(docs, "Q3_Financial_Budget.csv").writeText(
                """Category,Allocated,Spent,Remaining
Engineering,50000,42000,8000
Infrastructure,12000,9800,2200
Security Audit,15000,15000,0
Marketing,0,0,0
""".trimIndent()
            )

            File(projects, "config_prod.json").writeText(
                """{
  "app": "LibreFiles",
  "version": "1.0.0",
  "telemetry_enabled": false,
  "crypto_cipher": "AES-256-GCM",
  "storage_access_framework": true
}
""".trimIndent()
            )

            File(photos, "sample_landscape.txt").writeText("Sample image placeholder simulation for test preview.")
            File(photos, "sunset_beach.txt").writeText("Vacation photo metadata and preview note.")
            File(archives, "backup_notes.txt").writeText("Offline personal scratchpad.")

            // Log initialization
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "INITIALIZE",
                    target = sampleDataDir.path,
                    details = "Initialized LibreFiles workspace structure"
                )
            )
        }
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

        // Sorting: Folders always on top, then by sortOption
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

    suspend fun getCategoryFiles(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        val root = sampleDataDir
        val results = mutableListOf<FileItem>()

        fun scan(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { scan(it) }
            } else {
                if (getFileCategory(file) == category) {
                    results.add(
                        FileItem(
                            file = file,
                            name = file.name,
                            path = file.path,
                            size = file.length(),
                            isDirectory = false,
                            lastModified = file.lastModified(),
                            extension = file.extension.lowercase(),
                            mimeType = getMimeType(file),
                            isHidden = file.name.startsWith(".")
                        )
                    )
                }
            }
        }

        scan(root)
        results.sortedByDescending { it.lastModified }
    }

    suspend fun analyzeStorage(): StorageAnalysisResult = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        var usedBytes = 0L
        val catCounts = mutableMapOf<FileCategory, Pair<Int, Long>>()
        val allFiles = mutableListOf<FileItem>()

        FileCategory.values().forEach {
            catCounts[it] = Pair(0, 0L)
        }

        try {
            val stat = StatFs(sampleDataDir.path)
            totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            usedBytes = totalBytes - freeBytes
        } catch (e: Exception) {
            totalBytes = 64L * 1024 * 1024 * 1024
            usedBytes = 22L * 1024 * 1024 * 1024
        }

        fun scanDir(dir: File) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    scanDir(file)
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

        scanDir(sampleDataDir)

        val categoryStats = catCounts.map { (cat, data) ->
            CategoryStat(category = cat, fileCount = data.first, totalBytes = data.second)
        }.filter { it.category != FileCategory.ALL }

        val largeFiles = allFiles.sortedByDescending { it.size }.take(10)
        val oldestFiles = allFiles.sortedBy { it.lastModified }.take(10)

        // Find duplicates by size + name
        val grouped = allFiles.groupBy { "${it.size}_${it.name.lowercase()}" }
        val duplicates = grouped.values.filter { it.size > 1 }

        StorageAnalysisResult(
            totalStorageBytes = totalBytes,
            usedStorageBytes = usedBytes,
            freeStorageBytes = (totalBytes - usedBytes).coerceAtLeast(0L),
            categoryStats = categoryStats,
            largeFiles = largeFiles,
            duplicateCandidates = duplicates,
            oldestFiles = oldestFiles
        )
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
        val target = File(sampleDataDir, originalName)
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
                // ignore failed
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
                    // Protect against Zip Slip vulnerability
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
        dir.listFiles()?.forEach { file ->
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
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml", "kt", "html" -> FileCategory.DOCUMENTS
            "zip", "rar", "7z", "tar", "gz", "bz2" -> FileCategory.ARCHIVES
            "apk", "xapk", "apks" -> FileCategory.APKS
            else -> FileCategory.ALL
        }
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "md", "kt", "csv" -> "text/plain"
            "json" -> "application/json"
            else -> "*/*"
        }
    }
}
