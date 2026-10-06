package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY dateAdded DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE path = :path)")
    fun isBookmarked(path: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE path = :path")
    suspend fun deleteBookmark(path: String)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY dateEncrypted DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: String)

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: String): VaultItemEntity?
}

@Dao
interface FileTagDao {
    @Query("SELECT * FROM file_tags WHERE path = :path")
    fun getTagsForPath(path: String): Flow<List<FileTagEntity>>

    @Query("SELECT * FROM file_tags")
    fun getAllTags(): Flow<List<FileTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: FileTagEntity)

    @Query("DELETE FROM file_tags WHERE path = :path AND tag = :tag")
    suspend fun deleteTag(path: String, tag: String)

    @Query("DELETE FROM file_tags WHERE path = :path")
    suspend fun deleteAllTagsForPath(path: String)
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedDate DESC")
    fun getAllTrashItems(): Flow<List<TrashItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(item: TrashItemEntity)

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrash(id: String)

    @Query("DELETE FROM trash_items")
    suspend fun clearTrash()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}

@Database(
    entities = [
        BookmarkEntity::class,
        VaultItemEntity::class,
        FileTagEntity::class,
        TrashItemEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun vaultDao(): VaultDao
    abstract fun fileTagDao(): FileTagDao
    abstract fun trashDao(): TrashDao
    abstract fun auditLogDao(): AuditLogDao
}
