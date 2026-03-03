package com.cola.pickly.core.model

/**
 * 연속 촬영(버스트) 그룹을 나타내는 데이터 클래스.
 *
 * 시간 기반(2초 이내) + dHash 유사도 기반으로 감지된
 * 연사 사진들의 그룹 정보를 담습니다.
 */
data class BurstGroup(
    val groupId: String,
    val groupIndex: Int,
    val photoIds: List<Long>,
    val bestPhotoId: Long,
    val recommendedPhotoIds: List<Long>,
    val bestScore: Double
) {
    fun rankOf(photoId: Long): Int? {
        val idx = photoIds.indexOf(photoId)
        return if (idx >= 0) idx + 1 else null
    }
}
