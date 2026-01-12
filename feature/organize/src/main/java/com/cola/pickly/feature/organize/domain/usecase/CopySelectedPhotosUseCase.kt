package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import com.cola.pickly.core.data.settings.ResultSaveLocationPolicy
import com.cola.pickly.core.data.settings.Settings
import com.cola.pickly.core.data.settings.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CopySelectedPhotosUseCase @Inject constructor(
    private val photoActionRepository: PhotoActionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(photoIds: List<Long>): PhotoActionReport {
        if (photoIds.isEmpty()) return PhotoActionReport(successCount = 0)

        val settings = settingsRepository.settings.first()
        val destination = resolveDestination(settings)

        val report = photoActionRepository.copyPhotos(
            photoIds = photoIds,
            destinationRelativePath = destination,
            policy = settings.duplicateFilenamePolicy
        )

        settingsRepository.setLastUsedSaveFolder(destination)
        return report
    }

    private fun resolveDestination(settings: Settings): String {
        return when (settings.resultSaveLocationPolicy) {
            ResultSaveLocationPolicy.RememberLastUsedFolder ->
                settings.lastUsedSaveFolder ?: DEFAULT_BASE_PATH
            ResultSaveLocationPolicy.AlwaysCreateNewFolder ->
                buildTimestampPath()
        }
    }

    private fun buildTimestampPath(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        val now = formatter.format(Date())
        return "$DEFAULT_BASE_PATH/$now"
    }

    private companion object {
        const val DEFAULT_BASE_PATH = "DCIM/Pickly"
    }
}
