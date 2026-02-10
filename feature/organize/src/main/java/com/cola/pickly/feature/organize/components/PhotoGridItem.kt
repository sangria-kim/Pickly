package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.ui.components.RejectReasonBadge
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.transition.ContainerTransformSpec
import com.cola.pickly.core.ui.transition.LocalAnimatedVisibilityScope
import com.cola.pickly.core.ui.transition.LocalSharedTransitionScope
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoGridItem(
    photo: Photo,
    isSelected: Boolean,
    selectionState: PhotoSelectionState? = null,
    isMultiSelectMode: Boolean = false,
    isAnalyzing: Boolean = false,
    autoRejectReason: RejectReason? = null,
    onClick: () -> Unit,
    onToggleSelection: (() -> Unit)? = null
) {
    // 콜백을 안전하게 참조하기 위해 rememberUpdatedState 사용
    val onClickState = rememberUpdatedState(onClick)
    val onToggleSelectionState = rememberUpdatedState(onToggleSelection)
    val context = LocalContext.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

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
                    // 분석 중일 때 롱프레스 제한
                    if (isAnalyzing) {
                        Toast.makeText(context, "분석이 끝난 뒤에 선택할 수 있어요.", Toast.LENGTH_SHORT).show()
                        return@combinedClickable
                    }

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
                .crossfade(false)
                .size(512)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "photo_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = ContainerTransformSpec.PhotoContainerTransform
                            )
                        }
                    } else Modifier
                )
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(TealAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accepted",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                PhotoSelectionState.Rejected -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(Color(0xFFFF5252), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Rejected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                PhotoSelectionState.None -> {
                    // 아이콘 표시 없음
                }
            }
        }

        // 스마트 제외 후보 pill 배지 (우하단)
        autoRejectReason?.let { reason ->
            RejectReasonBadge(
                reason = reason,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
    }
}
