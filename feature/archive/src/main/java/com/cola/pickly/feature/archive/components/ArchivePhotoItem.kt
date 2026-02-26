package com.cola.pickly.feature.archive.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.transition.ContainerTransformSpec
import com.cola.pickly.core.ui.transition.LocalAnimatedVisibilityScope
import com.cola.pickly.core.ui.transition.LocalSharedTransitionScope
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ArchivePhotoItem(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onToggleSelection: (() -> Unit)? = null
) {
    val onClickState = rememberUpdatedState(onClick)
    val onToggleSelectionState = rememberUpdatedState(onToggleSelection)
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) {
                        onToggleSelectionState.value?.invoke()
                    } else {
                        onClickState.value()
                    }
                },
                onLongClick = {
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

        // 선택된 사진에 대한 약한 강조 효과
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
    }
}
