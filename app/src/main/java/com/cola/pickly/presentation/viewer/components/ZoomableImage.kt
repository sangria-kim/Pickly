package com.cola.pickly.presentation.viewer.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ZoomableImage(
    imagePath: String,
    onZoomStateChanged: (Float) -> Unit, // Boolean -> Float
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        // 확대되지 않은 상태에서 단일 손가락 드래그는 무시 (HorizontalPager가 처리하도록)
        // 핀치 줌(두 손가락)만 처리
        if (zoomChange != 1f || scale > 1f) {
            scale *= zoomChange
            // Scale 범위 제한 (예: 0.5f ~ 4f)
            scale = scale.coerceIn(1f, 4f)

            // 확대/축소 상태 변경 콜백 호출
            onZoomStateChanged(scale)

            // 화면 이동 로직 (확대되었을 때만)
            if (scale > 1f) {
                offset += offsetChange
            } else {
                // 확대되지 않았을 때는 offset을 리셋
                offset = Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(
                state = transformableState,
                lockRotationOnZoomPan = true,
                canPan = { scale > 1f } // 확대되었을 때만 패닝 허용
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imagePath)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
