package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreHelper(private val context: Context) {

    suspend fun queryRecentFiles(limit: Int = 25): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT $limit"
        try {
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""

                    val file = if (!rawPath.isNullOrEmpty()) File(rawPath) else null
                    if (name.isNotEmpty() && !name.startsWith(".")) {
                        val contentUri = ContentUris.withAppendedId(uri, id)
                        items.add(
                            FileItem(
                                file = file,
                                uri = contentUri,
                                name = name,
                                path = rawPath ?: contentUri.toString(),
                                size = if (file != null && file.exists()) file.length() else size,
                                isDirectory = false,
                                lastModified = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                                extension = name.substringAfterLast('.', "").lowercase(),
                                mimeType = mime
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore failure
        }
        items
    }

    suspend fun queryCategory(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        when (category) {
            FileCategory.IMAGES -> queryMedia(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                items = items
            )
            FileCategory.VIDEOS -> queryMedia(
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                items = items
            )
            FileCategory.AUDIO -> queryMedia(
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                items = items
            )
            FileCategory.DOWNLOADS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        queryMedia(
                            uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            items = items
                        )
                    } catch (e: Exception) {
                        scanDirectoryFallback(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            items
                        )
                    }
                } else {
                    scanDirectoryFallback(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        items
                    )
                }
            }
            FileCategory.DOCUMENTS -> {
                queryFilesByExtensions(
                    extensions = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml", "kt", "html", "epub"),
                    items = items
                )
            }
            FileCategory.ARCHIVES -> {
                queryFilesByExtensions(
                    extensions = listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz"),
                    items = items
                )
            }
            FileCategory.APKS -> {
                queryFilesByExtensions(
                    extensions = listOf("apk", "xapk", "apks"),
                    items = items
                )
            }
            FileCategory.TRASH, FileCategory.VAULT, FileCategory.ALL -> {
                // Handled separately
            }
        }

        // If MediaStore returned empty (e.g. fresh phone/test container), scan standard directories directly
        if (items.isEmpty()) {
            val publicDir = when (category) {
                FileCategory.IMAGES -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                FileCategory.VIDEOS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                FileCategory.AUDIO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                FileCategory.DOWNLOADS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                FileCategory.DOCUMENTS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                else -> null
            }
            publicDir?.let { scanDirectoryFallback(it, items, category) }
        }

        items.distinctBy { it.path }.sortedByDescending { it.lastModified }
    }

    private fun queryMedia(
        uri: Uri,
        items: MutableList<FileItem>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        try {
            context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""

                    val file = if (!rawPath.isNullOrEmpty()) File(rawPath) else null
                    val contentUri = ContentUris.withAppendedId(uri, id)

                    if (name.isNotEmpty() && !name.startsWith(".")) {
                        items.add(
                            FileItem(
                                file = file,
                                uri = contentUri,
                                name = name,
                                path = rawPath ?: contentUri.toString(),
                                size = if (file != null && file.exists()) file.length() else size,
                                isDirectory = false,
                                lastModified = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                                extension = name.substringAfterLast('.', "").lowercase(),
                                mimeType = mime
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cursor error
        }
    }

    private fun queryFilesByExtensions(
        extensions: List<String>,
        items: MutableList<FileItem>
    ) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = extensions.joinToString(separator = " OR ") {
            "${MediaStore.Files.FileColumns.DATA} LIKE '%.${it}'"
        }
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""

                    val file = if (!rawPath.isNullOrEmpty()) File(rawPath) else null
                    val contentUri = ContentUris.withAppendedId(uri, id)

                    if (name.isNotEmpty() && !name.startsWith(".")) {
                        items.add(
                            FileItem(
                                file = file,
                                uri = contentUri,
                                name = name,
                                path = rawPath ?: contentUri.toString(),
                                size = if (file != null && file.exists()) file.length() else size,
                                isDirectory = false,
                                lastModified = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                                extension = name.substringAfterLast('.', "").lowercase(),
                                mimeType = mime
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun scanDirectoryFallback(dir: File, items: MutableList<FileItem>, targetCat: FileCategory? = null) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                val ext = file.extension.lowercase()
                items.add(
                    FileItem(
                        file = file,
                        name = file.name,
                        path = file.path,
                        size = file.length(),
                        isDirectory = false,
                        lastModified = file.lastModified(),
                        extension = ext,
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                    )
                )
            }
        }
    }
}
