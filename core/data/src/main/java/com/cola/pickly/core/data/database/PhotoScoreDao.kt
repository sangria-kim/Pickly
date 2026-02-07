package com.cola.pickly.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(photoScore: PhotoScoreEntity)

    @Query("SELECT * FROM photo_scores WHERE photoId = :photoId")
    suspend fun getScore(photoId: Long): PhotoScoreEntity?

    @Query("SELECT * FROM photo_scores WHERE photoId IN (:photoIds)")
    suspend fun getScoresList(photoIds: List<Long>): List<PhotoScoreEntity>

    suspend fun getScores(photoIds: List<Long>): Map<Long, PhotoScoreEntity> {
        return getScoresList(photoIds).associateBy { it.photoId }
    }

    @Query("DELETE FROM photo_scores WHERE schemaVersion != :currentVersion")
    suspend fun deleteOutdatedSchemas(currentVersion: Int)
}