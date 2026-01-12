package com.cola.pickly.feature.archive

import com.cola.pickly.core.model.WeeklyPhoto

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
     * @param folderGroups 폴더별로 그룹핑된 채택된 사진 리스트
     *                     키: 폴더명, 값: 해당 폴더의 채택된 사진 리스트
     */
    data class ArchiveReady(
        val folderGroups: Map<String, List<WeeklyPhoto>>
    ) : ArchiveUiState {
        /**
         * 폴더 개수
         */
        val folderCount: Int
            get() = folderGroups.size

        /**
         * 전체 채택된 사진 개수
         */
        val totalPhotoCount: Int
            get() = folderGroups.values.sumOf { it.size }
    }

    /**
     * 채택된 사진이 없는 상태 (Empty State)
     */
    data object EmptyArchive : ArchiveUiState
}

