package com.cola.pickly.presentation.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.cola.pickly.domain.model.WeeklyPhoto
import com.cola.pickly.presentation.viewer.components.ViewerBottomOverlay
import com.cola.pickly.presentation.viewer.components.ViewerTopOverlay
import com.cola.pickly.presentation.viewer.components.ZoomableImage

@Composable
fun ViewerScreen(
    photos: List<WeeklyPhoto>,
    initialIndex: Int,
    selectionMap: Map<Long, PhotoSelectionState>,
    onBackClick: () -> Unit,
    onSelectClick: (Long) -> Unit,
    onRejectClick: (Long) -> Unit
) {
    var isOverlayVisible by remember { mutableStateOf(true) }
    var isZoomed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex,
            pageCount = { photos.size }
        )

        val currentPhoto = photos.getOrNull(pagerState.currentPage)
        val currentSelectionState = currentPhoto?.let { selectionMap[it.id] } ?: PhotoSelectionState.None
        
        val isSelected = currentSelectionState == PhotoSelectionState.Selected
        val isRejected = currentSelectionState == PhotoSelectionState.Rejected

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed // 확대 중일 때는 스와이프 비활성화
        ) { page ->
            val photo = photos[page]
            
            ZoomableImage(
                imagePath = photo.filePath,
                onZoomStateChanged = { scale ->
                    isZoomed = scale > 1f
                    isOverlayVisible = scale == 1f
                }
            )
        }

        // Navigation Handles (Visual only)
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .width(4.dp)
                    .height(64.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }

        // Right Handle
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .width(4.dp)
                    .height(64.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }

        // Top Overlay
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ViewerTopOverlay(
                currentIndex = pagerState.currentPage,
                totalCount = photos.size,
                onBackClick = onBackClick
            )
        }

        // Bottom Overlay
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ViewerBottomOverlay(
                isSelected = isSelected,
                isRejected = isRejected,
                onSelectClick = { 
                    currentPhoto?.let { onSelectClick(it.id) } 
                },
                onRejectClick = { 
                    currentPhoto?.let { onRejectClick(it.id) } 
                }
            )
        }
    }
}
