package com.toimg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.toimg.app.data.local.entity.RecognitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecognitionDao {

    @Query("SELECT * FROM recognition_results ORDER BY recognizedAt DESC")
    fun getAllRecognitions(): Flow<List<RecognitionEntity>>

    @Query("SELECT * FROM recognition_results WHERE imageId = :imageId LIMIT 1")
    suspend fun getRecognitionByImageId(imageId: String): RecognitionEntity?

    @Query("SELECT * FROM recognition_results WHERE status = 'SUCCESS' ORDER BY recognizedAt DESC")
    fun getSuccessfulRecognitions(): Flow<List<RecognitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecognitions(items: List<RecognitionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecognition(item: RecognitionEntity): Long

    @Query("UPDATE recognition_results SET keywords = :keywords WHERE imageId = :imageId")
    suspend fun updateKeywords(imageId: String, keywords: String): Int

    @Query("SELECT * FROM recognition_results WHERE keywords LIKE '%' || :keyword || '%'")
    suspend fun getRecognitionsWithKeyword(keyword: String): List<RecognitionEntity>

    @Query("DELETE FROM recognition_results WHERE imageId = :imageId")
    suspend fun deleteByImageId(imageId: String): Int

    @Query("DELETE FROM recognition_results")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(imageId) FROM recognition_results WHERE status = 'SUCCESS'")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(imageId) FROM recognition_results WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>
}
