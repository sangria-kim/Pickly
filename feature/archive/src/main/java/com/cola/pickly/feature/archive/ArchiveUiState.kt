package com.cola.pickly.feature.archive

/**
 * S-06 아카이브 화면의 UI 상태
 * 
 * Wireframe.md S-06 참고:
 * - 채택된 사진만 표시 (읽기 전용)
 * - 폴더별 섹션 구성
 * - 각 섹션에 "폴더명 N Picks" 형식의 헤더
 */
sealed interface ArchiveUiState {
    /**
     * 아카이브 데이터 로딩 중
     */
    data object LoadingArchive : ArchiveUiState

    /**
     * 아카이브 데이터가 준비된 상태
     * 
     * @param folderSections bucketId 기준으로 그룹핑된 채택된 사진 섹션
     */
    data class ArchiveReady(
        val folderSections: List<ArchiveFolderSection>
    ) : ArchiveUiState {
        /**
         * 폴더 개수
         */
        val folderCount: Int
            get() = folderSections.size

        /**
         * 전체 채택된 사진 개수
         */
        val totalPhotoCount: Int
            get() = folderSections.sumOf { it.photos.size }
    }

    /**
     * 채택된 사진이 없는 상태 (Empty State)
     */
    data object EmptyArchive : ArchiveUiState
}

