package com.cola.pickly.core.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cola.pickly.core.model.RecommendationScore

@Entity(
    tableName = "photo_scores",
    indices = [
        Index(value = ["photoId"], unique = true),
        Index(value = ["schemaVersion"]),
        Index(value = ["analyzedAt"])
    ]
)
data class PhotoScoreEntity(
    @PrimaryKey
    val photoId: Long,          // MediaStore ID
    val score: RecommendationScore,
    val analyzedAt: Long,       // 분석 시각

    // 캐시 유효성 검증 필드
    val analysisCriteriaHash: String,  // SHA-256 해시 (분석 기준 식별)
    val modifiedAt: Long,              // 사진 파일 수정 시각 (MediaStore)
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION  // 분석 로직 버전
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
    }
}