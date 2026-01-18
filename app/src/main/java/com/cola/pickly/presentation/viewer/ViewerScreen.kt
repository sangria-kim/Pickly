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
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.ViewerContext
import com.cola.pickly.core.model.Photo
import com.cola.pickly.presentation.viewer.components.ViewerBottomOverlay
import com.cola.pickly.presentation.viewer.components.ViewerTopOverlay
import com.cola.pickly.presentation.viewer.components.ZoomableImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ViewerScreen(
    photos: List<Photo>,
    initialIndex: Int,
    selectionMap: Map<Long, PhotoSelectionState>,
    viewerContext: ViewerContext,
    onBackClick: () -> Unit,
    onSelectClick: (Long) -> Unit = {},
    onRejectClick: (Long) -> Unit = {}
) {
    var isOverlayVisible by remember { mutableStateOf(true) }
    var isInfoVisible by remember { mutableStateOf(false) } // State for info overlay
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
                    if (isZoomed) isInfoVisible = false // Hide info when zoomed
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
                isInfoVisible = isInfoVisible,
                onBackClick = onBackClick,
                onInfoClick = { isInfoVisible = !isInfoVisible }
            )
        }

        // Info Overlay
        if (isInfoVisible && isOverlayVisible && currentPhoto != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp) // Position below top overlay
                    .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                 androidx.compose.foundation.layout.Column(
                     horizontalAlignment = Alignment.End
                 ) {
                     val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm") }
                     val dateString = remember(currentPhoto.takenAt) {
                         java.time.Instant.ofEpochMilli(currentPhoto.takenAt)
                             .atZone(java.time.ZoneId.systemDefault())
                             .format(formatter)
                     }
                     val filename = remember(currentPhoto.filePath) {
                         currentPhoto.filePath.substringAfterLast("/")
                     }

                     androidx.compose.material3.Text(
                         text = dateString,
                         style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                         color = Color.White
                     )
                     androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                     androidx.compose.material3.Text(
                         text = filename,
                         style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                         color = Color.Gray
                     )
                 }
            }
        }

        // Bottom Overlay - SELECT Context에서만 표시
        if (viewerContext == ViewerContext.SELECT) {
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
}
