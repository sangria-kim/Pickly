package com.cola.pickly.feature.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.feature.archive.components.ArchiveEmptyScreen
import com.cola.pickly.feature.archive.components.ArchiveSection
import com.cola.pickly.feature.archive.components.ArchiveTopBar
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.ui.theme.BackgroundWhite

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
    globalSelectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>, Boolean) -> Unit,
    onNavigateToOrganize: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 전역 selectionMap이 변경될 때마다 아카이브 업데이트
    // 화면이 처음 표시될 때도 업데이트
    LaunchedEffect(globalSelectionMap) {
        // 전역 selectionMap을 기반으로 모든 폴더의 채택된 사진을 조회
        viewModel.loadArchive(globalSelectionMap = globalSelectionMap)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                        // 아카이브는 읽기 전용이므로 전역 selectionMap 사용
                                        // 모든 아카이브 사진을 Selected 상태로 표시하기 위해
                                        // 전역 selectionMap을 그대로 전달
                                        photo.bucketId?.let { bucketId ->
                                            onNavigateToPhotoDetail(
                                                bucketId,
                                                photo.id,
                                                globalSelectionMap,
                                                true
                                            )
                                        }
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

