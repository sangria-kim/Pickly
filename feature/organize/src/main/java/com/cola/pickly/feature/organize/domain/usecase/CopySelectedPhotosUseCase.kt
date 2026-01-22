package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import com.cola.pickly.core.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CopySelectedPhotosUseCase @Inject constructor(
    private val photoActionRepository: PhotoActionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        photoIds: List<Long>,
        destinationRelativePath: String
    ): PhotoActionReport {
        if (photoIds.isEmpty()) return PhotoActionReport(successCount = 0)

        val settings = settingsRepository.settings.first()

        val report = photoActionRepository.copyPhotos(
            photoIds = photoIds,
            destinationRelativePath = destinationRelativePath,
            policy = settings.duplicateFilenamePolicy
        )

        return report
    }
}
