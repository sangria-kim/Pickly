package com.cola.pickly.core.data.cache

/**
 * 앱 캐시(임시 데이터) 관리용 계약.
 *
 * - 사진 원본(MediaStore)에는 영향을 주지 않습니다.
 * - cacheDir(및 Coil 캐시 등)만 대상으로 합니다.
 */
interface CacheRepository {
    /**
     * 현재 앱 캐시 용량(bytes)
     */
    suspend fun getCacheSizeBytes(): Long

    /**
     * 앱 캐시 삭제
     */
    suspend fun clearCache()
}


