package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String, // UUID
    val originalName: String,
    val encryptedFileName: String,
    val originalPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val ivBase64: String,
    val saltBase64: String,
    val dateEncrypted: Long = System.currentTimeMillis()
)

@Entity(tableName = "file_tags")
data class FileTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val tag: String,
    val colorHex: String
)

@Entity(tableName = "trash_items")
data class TrashItemEntity(
    @PrimaryKey val id: String,
    val originalPath: String,
    val fileName: String,
    val trashFileName: String,
    val sizeBytes: Long,
    val deletedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String, // CREATE, RENAME, DELETE, BATCH_MOVE, ENCRYPT, ZIP, etc.
    val target: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

@Entity(tableName = "search_history")
data class SearchQueryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
