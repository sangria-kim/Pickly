package com.cola.pickly.presentation.weeklydetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cola.pickly.domain.model.WeekId
import com.cola.pickly.presentation.common.FullScreenPhotoViewer
import com.cola.pickly.presentation.weeklylist.WeeklyListViewModel
import com.cola.pickly.presentation.weeklydetail.components.AllPhotosSection
import com.cola.pickly.presentation.weeklydetail.components.RecommendedPhotosSection

/**
 * 특정 주차의 상세 정보를 보여주는 화면입니다.
 * 추천 사진, 전체 사진 보기 토글, 모든 사진 그리드로 구성됩니다.
 *
 * @param viewModel UI 상태와 비즈니스 로직을 관리하는 공유 [WeeklyListViewModel].
 * @param weekId 표시할 주차의 고유 ID. 이 ID를 사용하여 ViewModel에 데이터 로드를 요청합니다.
 * @param onNavigateBack 이전 화면(주차 리스트)으로 돌아가기 위한 콜백.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyDetailScreen(
    viewModel: WeeklyListViewModel,
    weekId: WeekId,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(weekId) {
        viewModel.onWeekSelected(weekId)
    }

    val uiState by viewModel.detailState.collectAsState()

    // 전체 화면 뷰어가 활성화되어 있을 때 Back 키 동작을 가로채서 뷰어만 닫습니다.
    BackHandler(enabled = uiState.fullScreenState != null) {
        viewModel.onFullScreenDismissed()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = uiState.weekTitle, style = MaterialTheme.typography.titleMedium)
                            Text(text = uiState.weekPeriod, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                RecommendedPhotosSection(
                    photos = uiState.recommendedPhotos,
                    onPhotoClick = { photo -> viewModel.onPhotoTapped(photo) }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.toggleAllPhotosGrid() }) {
                        Text(if (uiState.isAllPhotosExpanded) "접기" else "전체 사진 보기")
                        Icon(
                            imageVector = if (uiState.isAllPhotosExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }

                if (uiState.isAllPhotosExpanded) {
                    AllPhotosSection(
                        photos = uiState.allPhotos,
                        onPhotoClick = { photo -> viewModel.onPhotoTapped(photo) }
                    )
                }
            }
        }

        // 전체 화면 뷰어 (오버레이)
        uiState.fullScreenState?.let {
            FullScreenPhotoViewer(
                photos = it.photos,
                initialIndex = it.initialIndex,
                onDismiss = { viewModel.onFullScreenDismissed() },
                isRecommended = { photo -> uiState.recommendedPhotos.contains(photo) }
            )
        }
    }
}