package com.cola.pickly.presentation.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.domain.model.WeeklyPhoto
import com.cola.pickly.domain.repository.PhotoRepository
import com.cola.pickly.presentation.archive.components.ArchiveEmptyScreen
import com.cola.pickly.presentation.archive.components.ArchiveSection
import com.cola.pickly.presentation.archive.components.ArchiveTopBar
import com.cola.pickly.presentation.organize.OrganizeViewModel
import com.cola.pickly.presentation.viewer.PhotoSelectionState
import com.cola.pickly.ui.theme.BackgroundWhite

/**
 * S-06 아카이브 화면
 * 
 * Wireframe.md S-06 참고:
 * - 채택된 사진만 표시 (읽기 전용)
 * - 폴더별 섹션 구성
 * - 사진 탭 시 S-04 풀스크린 뷰어 진입 (읽기 전용 모드)
 */
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel = hiltViewModel(),
    organizeViewModel: OrganizeViewModel = hiltViewModel(),
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>) -> Unit,
    onNavigateToOrganize: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val organizeUiState by organizeViewModel.uiState.collectAsStateWithLifecycle()
    val globalSelectionMap by organizeViewModel.globalSelectionMap.collectAsStateWithLifecycle()

    // 전역 selectionMap이 변경될 때마다 아카이브 업데이트
    // 화면이 처음 표시될 때도 업데이트
    LaunchedEffect(globalSelectionMap) {
        // 전역 selectionMap을 기반으로 모든 폴더의 채택된 사진을 조회
        viewModel.loadArchive(globalSelectionMap = globalSelectionMap)
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            ArchiveTopBar()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ArchiveUiState.LoadingArchive -> {
                    CircularProgressIndicator()
                }
                is ArchiveUiState.ArchiveReady -> {
                    if (state.folderGroups.isEmpty()) {
                        ArchiveEmptyScreen(
                            onNavigateToOrganize = onNavigateToOrganize
                        )
                    } else {
                        // 폴더별 섹션 표시
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.folderGroups.entries.toList(),
                                key = { it.key }
                            ) { (folderName, photos) ->
                                ArchiveSection(
                                    folderName = folderName,
                                    photos = photos,
                                    onPhotoClick = { photo ->
                                        // 사진 탭 시 풀스크린 뷰어로 이동
                                        // 해당 사진이 속한 폴더의 ID를 찾기
                                        // V1에서는 OrganizeViewModel의 현재 선택된 폴더 ID를 사용
                                        // 향후 개선: 사진의 실제 폴더 ID를 추출하여 사용
                                        val folderId = when (val orgState = organizeUiState) {
                                            is com.cola.pickly.presentation.organize.OrganizeUiState.GridReady -> orgState.folderId
                                            else -> ""
                                        }
                                        
                                        // 아카이브는 읽기 전용이므로 전역 selectionMap 사용
                                        // 모든 아카이브 사진을 Selected 상태로 표시하기 위해
                                        // 전역 selectionMap을 그대로 전달
                                        onNavigateToPhotoDetail(
                                            folderId,
                                            photo.id,
                                            globalSelectionMap
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                is ArchiveUiState.EmptyArchive -> {
                    ArchiveEmptyScreen(
                        onNavigateToOrganize = onNavigateToOrganize
                    )
                }
            }
        }
    }
}

