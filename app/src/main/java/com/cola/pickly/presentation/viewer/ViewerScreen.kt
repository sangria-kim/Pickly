package com.cola.pickly.presentation.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.data.settings.Settings
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.ViewerContext
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.ui.util.ViewerSystemBarsPolicy
import com.cola.pickly.presentation.common.DebugOverlay
import com.cola.pickly.presentation.viewer.components.ViewerBottomOverlay
import com.cola.pickly.presentation.viewer.components.ViewerTopOverlay
import com.cola.pickly.presentation.viewer.components.ZoomableImage
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ViewerScreen(
    photos: List<Photo>,
    initialIndex: Int,
    selectionMap: Map<Long, PhotoSelectionState>,
    viewerContext: ViewerContext,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onSelectClick: (Long) -> Unit = {},
    onRejectClick: (Long) -> Unit = {},
    settingsRepository: SettingsRepository
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = Settings()
    )
    var isOverlayVisible by remember { mutableStateOf(true) }
    var isInfoVisible by remember { mutableStateOf(false) } // State for info overlay
    var isZoomed by remember { mutableStateOf(false) }
    var overlayStateBeforeZoom by remember { mutableStateOf(false) }

    ViewerSystemBarsPolicy()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isZoomed) {
                detectTapGestures(
                    onTap = {
                        if (!isZoomed) {
                            isOverlayVisible = !isOverlayVisible
                        }
                    }
                )
            },
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
        val rejectReason = currentPhoto?.recommendationScore?.rejectReason

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed // 확대 중일 때는 스와이프 비활성화
        ) { page ->
            val photo = photos[page]

            Box(modifier = Modifier.fillMaxSize()) {
                ZoomableImage(
                    imagePath = photo.filePath,
                    photoId = photo.id,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onZoomStateChanged = { scale ->
                        val wasZoomed = isZoomed
                        isZoomed = scale > 1f

                        if (!wasZoomed && isZoomed) {
                            // 줌 시작: 현재 오버레이 상태 저장 후 숨김
                            overlayStateBeforeZoom = isOverlayVisible
                            isOverlayVisible = false
                            isInfoVisible = false
                        } else if (wasZoomed && !isZoomed) {
                            // 줌 해제: 이전 오버레이 상태 복원
                            isOverlayVisible = overlayStateBeforeZoom
                        }
                    }
                )

                // DebugOverlay — 조건부 표시:
                // 1. 일반 오버레이 표시 상태 (isOverlayVisible)
                // 2. 확대 중이 아님 (!isZoomed)
                // 3. ViewerContext가 SELECT일 때만 (ARCHIVE에서는 숨김)
                // 4. DebugOptions 중 하나라도 활성화됨
                val shouldShowDebugOverlay = isOverlayVisible
                    && !isZoomed
                    && viewerContext == ViewerContext.SELECT
                    && (settings.debugOptions.showFaceBoundingBox || settings.debugOptions.showScoreOverlay)

                AnimatedVisibility(
                    visible = shouldShowDebugOverlay,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    DebugOverlay(
                        photo = photo,
                        debugOptions = settings.debugOptions,
                        thresholds = settings.smartDiscardThresholds,
                        isInfoVisible = isInfoVisible
                    )
                }
            }
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
                    .statusBarsPadding()
                    .padding(top = 72.dp, end = 16.dp) // Position below top overlay (approx 72dp height)
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
                    rejectReason = rejectReason,
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
