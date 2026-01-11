package com.cola.pickly.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cola.pickly.domain.model.RecommendationScore

@Entity(tableName = "photo_scores")
data class PhotoScoreEntity(
    @PrimaryKey
    val photoId: Long,          // MediaStore ID
    val score: RecommendationScore,
    val analyzedAt: Long        // 분석 시각
)