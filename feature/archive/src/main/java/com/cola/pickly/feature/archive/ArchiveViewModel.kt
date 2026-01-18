package com.cola.pickly.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.model.PhotoSelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * S-06 아카이브 화면의 ViewModel
 * 
 * Wireframe.md S-06 참고:
 * - 채택된 사진(PhotoSelectionState.Selected)만 필터링
 * - 폴더별로 그룹핑
 * - 폴더는 가장 최근 채택된 사진 기준 내림차순 정렬 (V1에서는 간단히 구현)
 * - 폴더 내 사진은 촬영일 기준 정렬
 */
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArchiveUiState>(ArchiveUiState.LoadingArchive)
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    /**
     * 전역 선택 상태 맵을 받아서 아카이브 데이터를 생성
     * 모든 폴더의 사진을 조회하여 채택된 사진만 필터링합니다.
     * 
     * @param globalSelectionMap 전역 사진 ID와 선택 상태의 맵 (모든 폴더의 사진 상태 포함)
     *                          PhotoSelectionState.Selected인 사진만 필터링
     */
    fun loadArchive(
        globalSelectionMap: Map<Long, PhotoSelectionState>
    ) {
        viewModelScope.launch {
            _uiState.value = ArchiveUiState.LoadingArchive

            try {
                // 모든 폴더의 사진 조회
                val allPhotos = photoRepository.getAllPhotos()
                
                // 채택된 사진만 필터링 (전역 selectionMap 사용)
                val acceptedPhotos = allPhotos.filter { photo ->
                    globalSelectionMap[photo.id] == PhotoSelectionState.Selected
                }

                if (acceptedPhotos.isEmpty()) {
                    _uiState.value = ArchiveUiState.EmptyArchive
                    return@launch
                }

                // 폴더별로 그룹핑
                val folderGroups = groupPhotosByFolder(acceptedPhotos)

                // 폴더 정렬 (V1: 폴더명 기준 정렬)
                val sortedFolderGroups = folderGroups.toSortedMap(compareBy { it })

                // 각 폴더 내 사진 정렬 (촬영일 기준 내림차순 - 최신순)
                val sortedGroups = sortedFolderGroups.mapValues { (_, photos) ->
                    photos.sortedByDescending { it.takenAt }
                }

                _uiState.value = ArchiveUiState.ArchiveReady(
                    folderGroups = sortedGroups
                )
            } catch (e: Exception) {
                // 에러 발생 시 Empty 상태로 처리
                _uiState.value = ArchiveUiState.EmptyArchive
            }
        }
    }

    /**
     * 사진 목록을 폴더별로 그룹핑
     * 
     * @param photos 그룹핑할 사진 목록
     * @return 폴더명을 키로 하는 Map
     */
    private fun groupPhotosByFolder(photos: List<Photo>): Map<String, List<Photo>> {
        return photos.groupBy { photo ->
            extractFolderName(photo.filePath)
        }
    }

    /**
     * 파일 경로에서 폴더명 추출
     * 
     * 예시:
     * - `/storage/emulated/0/DCIM/Camera/photo.jpg` → `Camera`
     * - `/storage/emulated/0/Pictures/Screenshots/screenshot.png` → `Screenshots`
     * 
     * @param filePath 파일의 전체 경로
     * @return 폴더명 (경로의 마지막에서 두 번째 디렉토리명)
     */
    private fun extractFolderName(filePath: String): String {
        val file = File(filePath)
        val parent = file.parentFile ?: return "Unknown"
        
        // 부모 디렉토리의 이름을 폴더명으로 사용
        // 예: /storage/emulated/0/DCIM/Camera -> Camera
        return parent.name.takeIf { it.isNotEmpty() } ?: "Unknown"
    }
}

