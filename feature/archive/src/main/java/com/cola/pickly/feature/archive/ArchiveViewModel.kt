package com.cola.pickly.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoDisplayOrderComparator
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.model.PhotoSelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
                val (allPhotos, folders) = coroutineScope {
                    val photos = async {
                        photoRepository.getAllPhotos().sortedWith(PhotoDisplayOrderComparator)
                    }
                    val folderList = async {
                        photoRepository.getFolders()
                    }
                    photos.await() to folderList.await()
                }
                
                // 채택된 사진만 필터링 (전역 selectionMap 사용)
                val selectedIds = globalSelectionMap
                    .filterValues { it == PhotoSelectionState.Selected }
                    .keys
                val acceptedPhotos = allPhotos.filter { photo -> selectedIds.contains(photo.id) }

                if (acceptedPhotos.isEmpty()) {
                    _uiState.value = ArchiveUiState.EmptyArchive
                    return@launch
                }

                val folderNameMap = folders.associate { folder ->
                    folder.id to folder.name
                }
                val sections = acceptedPhotos
                    .groupBy { photo ->
                        photo.bucketId ?: "unknown:${extractFolderName(photo.filePath)}"
                    }
                    .map { (groupKey, photos) ->
                        val bucketId = photos.firstOrNull()?.bucketId
                        val folderName = when {
                            bucketId != null -> folderNameMap[bucketId] ?: extractFolderName(photos.first().filePath)
                            else -> extractFolderName(photos.first().filePath)
                        }
                        ArchiveFolderSection(
                            sectionId = bucketId ?: groupKey,
                            bucketId = bucketId,
                            folderName = folderName,
                            photos = photos
                        )
                    }
                    .sortedWith(
                        compareBy<ArchiveFolderSection> { it.folderName.lowercase() }
                            .thenBy { it.sectionId }
                    )

                _uiState.value = ArchiveUiState.ArchiveReady(
                    folderSections = sections
                )
            } catch (e: Exception) {
                // 에러 발생 시 Empty 상태로 처리
                _uiState.value = ArchiveUiState.EmptyArchive
            }
        }
    }

    private fun extractFolderName(filePath: String): String {
        val normalizedPath = filePath.trimEnd('/')
        val folderName = normalizedPath.substringBeforeLast("/", missingDelimiterValue = "")
            .substringAfterLast("/", missingDelimiterValue = "")
        return folderName.ifEmpty { "Unknown" }
    }
}

