package com.cola.pickly.feature.archive.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cola.pickly.core.model.Photo
import java.io.File

/**
 * S-06 아카이브 화면의 사진 아이템
 * 
 * Wireframe.md S-06 참고:
 * - 체크박스/선택 UI 없음 (읽기 전용)
 * - 채택/제외 아이콘 없음 (모두 채택된 사진만 표시)
 * - 탭 시 풀스크린 뷰어 진입
 */
@Composable
fun ArchivePhotoItem(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 콜백을 안전하게 참조하기 위해 rememberUpdatedState 사용
    val onClickState = rememberUpdatedState(onClick)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = { onClickState.value() })
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(photo.filePath))
                .crossfade(true)
                .size(512) // 이미지 크기를 512x512로 제한하여 메모리 사용량 감소
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

