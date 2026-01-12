package com.cola.pickly.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cola.pickly.core.model.RecommendationScore

@Entity(tableName = "photo_scores")
data class PhotoScoreEntity(
    @PrimaryKey
    val photoId: Long,          // MediaStore ID
    val score: RecommendationScore,
    val analyzedAt: Long        // 분석 시각
)