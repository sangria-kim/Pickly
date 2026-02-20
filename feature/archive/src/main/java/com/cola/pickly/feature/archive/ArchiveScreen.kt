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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.cola.pickly.feature.archive.components.ArchiveEmptyScreen
import com.cola.pickly.feature.archive.components.ArchiveSection
import com.cola.pickly.feature.archive.components.ArchiveTopBar
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason

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
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>, Boolean, Map<Long, RejectReason>, List<Long>?) -> Unit,
    onNavigateToOrganize: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 전역 selectionMap이 변경될 때마다 아카이브 업데이트
    // 화면이 처음 표시될 때도 업데이트
    LaunchedEffect(globalSelectionMap) {
        // 전역 selectionMap을 기반으로 모든 폴더의 채택된 사진을 조회
        viewModel.loadArchive(globalSelectionMap = globalSelectionMap)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val hasAcceptedPhotos = (uiState as? ArchiveUiState.ArchiveReady)?.totalPhotoCount?.let { it > 0 } ?: false
            ArchiveTopBar(
                hasAcceptedPhotos = hasAcceptedPhotos
            )
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
                    if (state.folderSections.isEmpty()) {
                        ArchiveEmptyScreen(
                            onNavigateToOrganize = onNavigateToOrganize
                        )
                    } else {
                        // 폴더별 섹션 표시
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.folderSections,
                                key = { it.sectionId }
                            ) { section ->
                                ArchiveSection(
                                    folderName = section.folderName,
                                    photos = section.photos,
                                    onPhotoClick = { photo ->
                                        val bucketId = photo.bucketId ?: section.bucketId
                                        if (bucketId == null) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("사진 폴더 정보를 찾을 수 없어요.")
                                            }
                                        } else {
                                            onNavigateToPhotoDetail(
                                                bucketId,
                                                photo.id,
                                                globalSelectionMap,
                                                true,
                                                emptyMap(),
                                                section.photos.map { it.id }
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

