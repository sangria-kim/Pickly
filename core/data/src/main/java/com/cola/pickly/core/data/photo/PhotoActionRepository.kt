package com.cola.pickly.core.data.photo

import android.content.IntentSender
import android.net.Uri
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy

/**
 * 사진 이동/복사/삭제/공유 등 쓰기 동작을 담당하는 계약.
 */
interface PhotoActionRepository {

    suspend fun getShareUris(photoIds: List<Long>): List<Uri>

    /**
     * 특정 미디어(사진)에 대한 '수정' 사용자 승인이 필요한 경우, 승인 다이얼로그를 띄우기 위한 IntentSender를 반환합니다.
     * - Android 11(API 30)+ 에서 Scoped Storage 정책으로 인해 필요할 수 있습니다.
     */
    suspend fun createWriteRequestIntentSender(photoIds: List<Long>): IntentSender?

    /**
     * 특정 미디어(사진)에 대한 '삭제' 사용자 승인이 필요한 경우, 승인 다이얼로그를 띄우기 위한 IntentSender를 반환합니다.
     * - Android 11(API 30)+ 에서 Scoped Storage 정책으로 인해 필요할 수 있습니다.
     */
    suspend fun createDeleteRequestIntentSender(photoIds: List<Long>): IntentSender?

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
    val errors: List<String> = emptyList(),
    val successIds: List<Long> = emptyList(),
    val skippedIds: List<Long> = emptyList(),
    val failedIds: List<Long> = emptyList()
)
