package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import com.cola.pickly.core.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class MoveSelectedPhotosUseCase @Inject constructor(
    private val photoActionRepository: PhotoActionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(photoIds: List<Long>): PhotoActionReport {
        if (photoIds.isEmpty()) return PhotoActionReport(successCount = 0)

        val settings = settingsRepository.settings.first()
        val destination = DESTINATION_RELATIVE_PATH

        val report = photoActionRepository.movePhotos(
            photoIds = photoIds,
            destinationRelativePath = destination,
            policy = settings.duplicateFilenamePolicy
        )

        return report
    }

    private companion object {
        const val DESTINATION_RELATIVE_PATH = "DCIM/Pickly"
    }
}
