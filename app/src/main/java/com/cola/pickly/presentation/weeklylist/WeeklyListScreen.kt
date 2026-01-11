package com.cola.pickly.presentation.weeklylist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.domain.model.WeekId
import com.cola.pickly.presentation.weeklylist.components.WeeklyItemCard

/**
 * 주간 베스트 사진을 주차별로 묶어 리스트로 보여주는 메인 화면입니다.
 * @param viewModel UI 상태와 비즈니스 로직을 관리하는 [WeeklyListViewModel].
 * @param onNavigateToDetail 특정 주차 카드를 클릭했을 때 상세 화면으로 이동하는 콜백.
 */
@Composable
fun WeeklyListScreen(
    viewModel: WeeklyListViewModel,
    onNavigateToDetail: (WeekId) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면 진입 시 데이터가 없으면 로드
    LaunchedEffect(Unit) {
        if (uiState.weeklyList.isEmpty() && !uiState.isLoading) {
            viewModel.loadWeeklyBestPhotos()
        }
    }

    // ViewModel의 상태를 사용하여 표시할 리스트를 결정
    val displayedList = uiState.weeklyList.take(uiState.visibleWeekCount)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Best Photos",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "알 수 없는 오류가 발생했습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.isEmpty -> {
                    EmptyPhotosView()
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedList) { week ->
                            WeeklyItemCard(
                                weekUiModel = week,
                                onNavigateToDetail = onNavigateToDetail
                            )
                        }

                        // "더보기" 버튼 표시 여부를 ViewModel의 상태에 따라 결정
                        if (uiState.canLoadMore) {
                            item {
                                MoreWeeksButton(onClick = { viewModel.loadMoreWeeks() })
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 로드된 사진이 하나도 없을 때 표시되는 화면입니다.
 * 사용자에게 사진을 찍거나 폴더를 변경하도록 안내합니다.
 */
@Composable
fun EmptyPhotosView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "아직 사진이 없어요",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "카메라로 새로 찍거나\n설정에서 다른 폴더를 선택해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MoreWeeksButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.OutlinedButton(onClick = onClick) {
            Text(text = "이전 주 더 보기")
        }
    }
}