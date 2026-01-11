package com.cola.pickly.presentation.weeklydetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cola.pickly.domain.model.WeeklyPhoto

/**
 * 주 상세 화면의 전체 사진을 3열 그리드로 보여주는 섹션입니다.
 *
 * @param photos 해당 주의 모든 사진 리스트
 * @param onPhotoClick 사진을 클릭했을 때의 콜백. 클릭된 [WeeklyPhoto] 객체를 전달합니다.
 */
@Composable
fun AllPhotosSection(
    photos: List<WeeklyPhoto>,
    onPhotoClick: (WeeklyPhoto) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3) // 3열 그리드
    ) {
        items(photos) { photo ->
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(photo) } // photo 객체 전체를 전달
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.filePath)
                        .size(512) // 그리드 아이템 썸네일 크기 제한
                        .build(),
                    contentDescription = "Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}