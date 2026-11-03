package com.toimg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.toimg.app.data.local.entity.FolderEntity
import com.toimg.app.data.local.entity.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY dateModified DESC")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE folderName = :folderName ORDER BY dateModified DESC")
    fun getImagesByFolder(folderName: String): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE sha256 IS NULL")
    suspend fun getImagesWithoutHash(): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ImageEntity>): List<Long>

    @Query("UPDATE images SET sha256 = :sha256 WHERE id = :id")
    suspend fun updateSha256(id: String, sha256: String): Int

    @Query("UPDATE images SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String): Int

    @Query("UPDATE images SET uriString = :uriString, name = :name, folderName = :folderName, status = :status WHERE id = :id")
    suspend fun updateImageLocation(id: String, uriString: String, name: String, folderName: String, status: String): Int

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    suspend fun getImagesByIds(ids: List<String>): List<ImageEntity>

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: String): Int

    @Query("DELETE FROM images")
    suspend fun clearAllImages(): Int

    @Query("SELECT * FROM images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: String): ImageEntity?

    @Query("SELECT * FROM images WHERE status = :status")
    suspend fun getImagesByStatus(status: String): List<ImageEntity>

    @Query("SELECT COUNT(id) FROM images WHERE status = :status")
    fun countImagesByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(id) FROM images")
    fun getImageCount(): Flow<Int>

    @Query("SELECT COUNT(id) FROM images WHERE sha256 IS NOT NULL")
    fun getHashedCount(): Flow<Int>
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>): List<Long>

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String): Int
}
