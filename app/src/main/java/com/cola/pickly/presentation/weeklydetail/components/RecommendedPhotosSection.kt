package com.cola.pickly.presentation.weeklydetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.cola.pickly.domain.model.WeeklyPhoto

/**
 * 주 상세 화면의 추천 사진 2장을 보여주는 섹션입니다.
 *
 * @param photos 추천된 사진 2장의 리스트
 * @param onPhotoClick 사진을 클릭했을 때의 콜백. 클릭된 [WeeklyPhoto] 객체를 전달합니다.
 */
@Composable
fun RecommendedPhotosSection(
    photos: List<WeeklyPhoto>,
    onPhotoClick: (WeeklyPhoto) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photos.take(2).forEach { photo ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(photo) } // photo 객체 전체를 전달
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.filePath)
                        .size(512) // 추천 사진 썸네일 크기 제한
                        .build(),
                    contentDescription = "Recommended Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}