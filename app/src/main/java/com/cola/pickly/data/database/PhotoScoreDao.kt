package com.cola.pickly.data.database

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
}