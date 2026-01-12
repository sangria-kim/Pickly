package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cola.pickly.core.model.WeeklyPhoto
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.ui.theme.TealAccent
import java.io.File

@Composable
fun PhotoGridItem(
    photo: WeeklyPhoto,
    isSelected: Boolean,
    selectionState: PhotoSelectionState? = null,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onToggleSelection: (() -> Unit)? = null
) {
    // 콜백을 안전하게 참조하기 위해 rememberUpdatedState 사용
    val onClickState = rememberUpdatedState(onClick)
    val onToggleSelectionState = rememberUpdatedState(onToggleSelection)
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = {
                    // Multi Select Mode에서는 탭 시 선택/해제, Normal Mode에서는 풀스크린 뷰어로 이동
                    if (isMultiSelectMode) {
                        onToggleSelectionState.value?.invoke()
                    } else {
                        onClickState.value()
                    }
                },
                onLongClick = {
                    // Normal Mode에서 롱프레스 시 Multi Select Mode 진입 및 선택
                    if (!isMultiSelectMode) {
                        onToggleSelectionState.value?.invoke()
                    }
                }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(photo.filePath))
                .crossfade(true)
                .size(512) // 이미지 크기를 512x512로 제한하여 메모리 사용량 감소
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelected && isMultiSelectMode) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = TealAccent,
                            shape = RoundedCornerShape(0.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        )

        // 선택된 사진에 대한 약한 강조 효과 (오버레이)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }

        // Multi Select Mode에서만 체크박스 표시 (좌상단)
        if (isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(14.dp)
                    .background(
                        color = if (isSelected) TealAccent else Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .border(
                        width = if (isSelected) 0.dp else 1.5.dp,
                        color = Color.Gray.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .clickable(
                        enabled = true,
                        onClick = { onToggleSelection?.invoke() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // 사진 선택 결과 표시 아이콘 (좌하단)
        selectionState?.let { state ->
            when (state) {
                PhotoSelectionState.Selected -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accepted",
                        tint = TealAccent,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
                PhotoSelectionState.Rejected -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Rejected",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
                PhotoSelectionState.None -> {
                    // 아이콘 표시 없음
                }
            }
        }
    }
}
