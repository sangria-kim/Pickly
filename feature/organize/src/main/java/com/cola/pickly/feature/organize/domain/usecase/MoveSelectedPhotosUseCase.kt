package com.cola.pickly.feature.organize.domain.usecase

import android.content.IntentSender
import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import com.cola.pickly.core.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class MoveSelectedPhotosUseCase @Inject constructor(
    private val photoActionRepository: PhotoActionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun createDeleteRequestIntentSender(photoIds: List<Long>): IntentSender? {
        return photoActionRepository.createDeleteRequestIntentSender(photoIds)
    }

    suspend operator fun invoke(
        photoIds: List<Long>,
        destinationRelativePath: String
    ): PhotoActionReport {
        if (photoIds.isEmpty()) return PhotoActionReport(successCount = 0)

        val settings = settingsRepository.settings.first()

        val report = photoActionRepository.movePhotos(
            photoIds = photoIds,
            destinationRelativePath = destinationRelativePath,
            policy = settings.duplicateFilenamePolicy
        )

        return report
    }
}
