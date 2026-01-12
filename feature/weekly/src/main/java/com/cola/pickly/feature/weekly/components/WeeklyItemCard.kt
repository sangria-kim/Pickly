package com.cola.pickly.feature.weekly.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.cola.pickly.core.model.WeekId
import com.cola.pickly.feature.weekly.WeekUiModel

/**
 * 주차별 베스트 사진을 보여주는 카드 컴포저블입니다.
 *
 * @param weekUiModel 표시할 주차의 UI 모델
 * @param onNavigateToDetail 카드를 클릭했을 때의 콜백 (WeekId 전달)
 */
@Composable
fun WeeklyItemCard(
    weekUiModel: WeekUiModel,
    onNavigateToDetail: (WeekId) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail(weekUiModel.weekId) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = weekUiModel.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = weekUiModel.periodText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (weekUiModel.bestPhotos.isEmpty()) {
                Text(
                    text = "추천된 사진이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    weekUiModel.bestPhotos.take(2).forEach { path ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(path)
                                .size(192) // 썸네일 크기 제한 (96.dp * 2 = 192px)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}