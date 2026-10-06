package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.FileItem
import com.example.data.model.StorageVolume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafRepository(private val context: Context) {

    fun takePersistablePermissions(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // Might already be held or not supported
        }
    }

    fun getPersistedUriVolumes(): List<StorageVolume> {
        val list = mutableListOf<StorageVolume>()
        val persistedList = context.contentResolver.persistedUriPermissions
        for (perm in persistedList) {
            val doc = DocumentFile.fromTreeUri(context, perm.uri)
            if (doc != null && doc.canRead()) {
                val displayName = doc.name ?: "SAF External Volume"
                list.add(
                    StorageVolume(
                        name = displayName,
                        path = perm.uri.toString(),
                        treeUri = perm.uri,
                        totalBytes = 128L * 1024 * 1024 * 1024,
                        freeBytes = 64L * 1024 * 1024 * 1024,
                        usedBytes = 64L * 1024 * 1024 * 1024,
                        isPrimary = false,
                        isSafMounted = true
                    )
                )
            }
        }
        return list
    }

    suspend fun listSafTreeFiles(treeUri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val results = mutableListOf<FileItem>()
        val files = root.listFiles()
        for (doc in files) {
            results.add(
                FileItem(
                    uri = doc.uri,
                    name = doc.name ?: "Unknown",
                    path = doc.uri.toString(),
                    size = doc.length(),
                    isDirectory = doc.isDirectory,
                    lastModified = doc.lastModified(),
                    extension = (doc.name ?: "").substringAfterLast(".", "").lowercase(),
                    mimeType = doc.type ?: "*/*",
                    isSafDocument = true
                )
            )
        }
        results.sortedWith(Comparator { a, b ->
            if (a.isDirectory && !b.isDirectory) -1
            else if (!a.isDirectory && b.isDirectory) 1
            else a.name.compareTo(b.name, ignoreCase = true)
        })
    }

    suspend fun createSafDirectory(treeUri: Uri, dirName: String): Uri? = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
        val created = root.createDirectory(dirName)
        created?.uri
    }

    suspend fun deleteSafFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromSingleUri(context, uri) ?: return@withContext false
        doc.delete()
    }
}
