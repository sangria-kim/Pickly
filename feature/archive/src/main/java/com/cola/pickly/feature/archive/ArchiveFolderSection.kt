package com.cola.pickly.feature.archive

import com.cola.pickly.core.model.Photo

data class ArchiveFolderSection(
    val sectionId: String,
    val bucketId: String?,
    val folderName: String,
    val photos: List<Photo>
)
