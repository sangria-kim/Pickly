package com.cola.pickly.feature.archive

sealed interface ArchiveUiState {
    data object LoadingArchive : ArchiveUiState

    data class ArchiveReady(
        val folderSections: List<ArchiveFolderSection>,
        val selectedIds: Set<Long> = emptySet()
    ) : ArchiveUiState {
        val folderCount: Int
            get() = folderSections.size

        val totalPhotoCount: Int
            get() = folderSections.sumOf { it.photos.size }

        val isMultiSelectMode: Boolean
            get() = selectedIds.isNotEmpty()

        val selectedCount: Int
            get() = selectedIds.size

        val allPhotoIds: Set<Long>
            get() = folderSections.flatMap { s -> s.photos.map { it.id } }.toSet()

        val isAllSelected: Boolean
            get() = allPhotoIds.isNotEmpty() && selectedIds.containsAll(allPhotoIds)

        fun isAllInFolderSelected(sectionId: String): Boolean {
            val folderIds = folderSections.find { it.sectionId == sectionId }
                ?.photos?.map { it.id }?.toSet() ?: return false
            return folderIds.isNotEmpty() && selectedIds.containsAll(folderIds)
        }
    }

    data object EmptyArchive : ArchiveUiState
}
