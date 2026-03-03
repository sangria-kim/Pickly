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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
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
import com.cola.pickly.core.ui.components.PhotoSelectionBadge
import com.cola.pickly.core.ui.components.BurstRecommendBadge
import com.cola.pickly.core.ui.components.BurstRecommendRank
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
    isSmartOrganizing: Boolean = false,
    rejectReason: RejectReason? = null,
    burstRecommendRank: BurstRecommendRank? = null,
    burstDebugInfo: BurstDebugInfo? = null,
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
                    // 스마트 정리 중일 때 롱프레스 제한
                    if (isSmartOrganizing) {
                        Toast.makeText(context, "스마트 정리가 끝난 뒤에 선택할 수 있어요.", Toast.LENGTH_SHORT).show()
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
            PhotoSelectionBadge(
                selectionState = state,
                backgroundAlpha = 0.9f,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
        }

        // 우하단 pill 배지 (제외 뱃지 우선, 없으면 추천 뱃지)
        when {
            rejectReason != null -> RejectReasonBadge(
                reason = rejectReason,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
            burstRecommendRank != null -> BurstRecommendBadge(
                rank = burstRecommendRank,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }

        // 연사 그룹 디버그 오버레이 (보더 + 라벨)
        burstDebugInfo?.let { info ->
            val groupColor = BurstDebugColors.forIndex(info.groupIndex)
            // 그룹 색상 보더
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, groupColor)
            )
            // 그룹 라벨 pill (우상단)
            val label = if (info.isBest) "${info.groupLabel}\u2605" else "${info.groupLabel}-${info.rankInGroup}"
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        color = groupColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp
                )
            }
        }
    }
}
