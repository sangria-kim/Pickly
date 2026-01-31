package com.cola.pickly.feature.organize

import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason

sealed interface OrganizeUiState {
    // 초기 상태: 폴더가 선택되지 않음
    data object NoFolderSelected : OrganizeUiState

    // 로딩 중
    data object Loading : OrganizeUiState

    // 폴더는 선택되었으나 사진이 없는 경우
    data class EmptyFolder(
        val folderId: String,
        val folderName: String
    ) : OrganizeUiState

    // 사진 목록이 준비된 상태
    data class GridReady(
        val folderId: String,
        val folderName: String,
        val photos: List<Photo>,
        val selectedIds: Set<Long> = emptySet(),
        val selectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
        val autoRejectCandidates: Map<Long, RejectReason> = emptyMap(), // 스마트 제외 후보 ID와 제외 사유
        val isAnalyzing: Boolean = false // 스마트 제외 분석 진행 중 여부
    ) : OrganizeUiState {
        /**
         * Multi Select Mode 활성화 여부
         * 
         * Multi Select Mode는 다음 조건 중 하나라도 만족하면 활성화됩니다:
         * - selectedIds에 1장 이상의 사진이 선택된 경우 (수동 선택 또는 필터 선택)
         * 
         * Wireframe.md S-02 참고:
         * - Normal Mode: 선택된 사진이 없음
         * - Multi Select Mode: 선택된 사진이 1장 이상
         */
        val isMultiSelectMode: Boolean
            get() = selectedIds.isNotEmpty()
        
        /**
         * 선택된 사진 개수
         * Multi Select Mode에서 Top Bar에 표시할 개수
         */
        val selectedCount: Int
            get() = selectedIds.size
    }
}
