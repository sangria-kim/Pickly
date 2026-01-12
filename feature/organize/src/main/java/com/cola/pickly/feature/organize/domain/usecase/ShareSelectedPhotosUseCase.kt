package com.cola.pickly.feature.organize.domain.usecase

import android.net.Uri
import com.cola.pickly.core.data.photo.PhotoActionRepository
import javax.inject.Inject

class ShareSelectedPhotosUseCase @Inject constructor(
    private val photoActionRepository: PhotoActionRepository
) {
    suspend operator fun invoke(photoIds: List<Long>): List<Uri> {
        if (photoIds.isEmpty()) return emptyList()
        return photoActionRepository.getShareUris(photoIds)
    }
}
