package com.cola.pickly.feature.organize.domain.usecase

import android.content.IntentSender
import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import javax.inject.Inject

class SoftDeleteSelectedPhotosUseCase @Inject constructor(
    private val photoActionRepository: PhotoActionRepository
) {
    suspend fun createWriteRequestIntentSender(photoIds: List<Long>): IntentSender? {
        return photoActionRepository.createWriteRequestIntentSender(photoIds)
    }

    suspend operator fun invoke(photoIds: List<Long>): PhotoActionReport {
        if (photoIds.isEmpty()) return PhotoActionReport(successCount = 0)
        return photoActionRepository.softDeletePhotos(photoIds)
    }
}
