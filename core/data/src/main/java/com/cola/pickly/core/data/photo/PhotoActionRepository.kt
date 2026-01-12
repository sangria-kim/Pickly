package com.cola.pickly.core.data.photo

import android.net.Uri
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy

/**
 * 사진 이동/복사/삭제/공유 등 쓰기 동작을 담당하는 계약.
 */
interface PhotoActionRepository {

    suspend fun getShareUris(photoIds: List<Long>): List<Uri>

    suspend fun movePhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport

    suspend fun copyPhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport

    suspend fun softDeletePhotos(photoIds: List<Long>): PhotoActionReport
}

/**
 * 일괄 작업 결과 요약.
 */
data class PhotoActionReport(
    val successCount: Int,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList()
)
