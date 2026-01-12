package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import javax.inject.Inject

class SoftDeleteSelectedPhotosUseCase @Inject constructor(
    private val photoActionRepository: PhotoActionRepository
) {
    suspend operator fun invoke(photoIds: List<Long>): PhotoActionReport {
        if (photoIds.isEmpty()) return PhotoActionReport(successCount = 0)
        return photoActionRepository.softDeletePhotos(photoIds)
    }
}
