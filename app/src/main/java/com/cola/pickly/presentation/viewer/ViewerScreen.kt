package com.cola.pickly.presentation.viewer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.cola.pickly.core.data.settings.Settings
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.ViewerContext
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.ui.transition.ContainerTransformSpec
import com.cola.pickly.core.ui.transition.LocalAnimatedVisibilityScope
import com.cola.pickly.core.ui.transition.LocalSharedTransitionScope
import com.cola.pickly.core.ui.util.ViewerSystemBarsPolicy
import com.cola.pickly.presentation.common.DebugOverlay
import com.cola.pickly.presentation.viewer.components.ViewerBottomOverlay
import com.cola.pickly.presentation.viewer.components.ViewerTopOverlay
import com.cola.pickly.presentation.viewer.components.ZoomableImage
import com.cola.pickly.presentation.viewer.components.ZoomableImageMetrics
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ViewerScreen(
    photos: List<Photo>,
    initialIndex: Int,
    selectionMap: Map<Long, PhotoSelectionState>,
    viewerContext: ViewerContext,
    exitRequestId: Int = 0,
    autoAdvanceEvent: Flow<AutoAdvanceEvent> = emptyFlow(),
    onBackClick: () -> Unit,
    onSelectClick: (Long) -> Unit = {},
    onRejectClick: (Long) -> Unit = {},
    settingsRepository: SettingsRepository
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = Settings()
    )
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var isOverlayVisible by remember { mutableStateOf(true) }
    var isInfoVisible by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    var overlayStateBeforeZoom by remember { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var isButtonLocked by remember { mutableStateOf(false) }
    val metricsByPhotoId = remember { mutableStateMapOf<Long, ZoomableImageMetrics>() }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val isTransitionActive = sharedTransitionScope?.isTransitionActive == true

    ViewerSystemBarsPolicy()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { rootSize = it }
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

        LaunchedEffect(Unit) {
            autoAdvanceEvent.collect { event ->
                when (event) {
                    AutoAdvanceEvent.Advance -> {
                        val nextPage = pagerState.currentPage + 1
                        if (nextPage < photos.size) {
                            isButtonLocked = true
                            pagerState.animateScrollToPage(nextPage)
                            isButtonLocked = false
                        }
                    }
                    AutoAdvanceEvent.AtLastPage -> {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "마지막 사진이에요", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val currentPhoto = photos.getOrNull(pagerState.currentPage)
        val currentSelectionState = currentPhoto?.let { selectionMap[it.id] } ?: PhotoSelectionState.None

        val isSelected = currentSelectionState == PhotoSelectionState.Selected
        val isRejected = currentSelectionState == PhotoSelectionState.Rejected
        val rejectReason = currentPhoto?.recommendationScore?.rejectReason
        val currentMetrics = currentPhoto?.let { metricsByPhotoId[it.id] } ?: ZoomableImageMetrics()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed
        ) { page ->
            val photo = photos[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (page == pagerState.currentPage
                            && sharedTransitionScope != null
                            && animatedVisibilityScope != null
                        ) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "photo_${photo.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = ContainerTransformSpec.PhotoContainerTransform
                                )
                            }
                        } else Modifier
                    )
            ) {
                ZoomableImage(
                    imagePath = photo.filePath,
                    photoId = photo.id,
                    onZoomStateChanged = { scale ->
                        val wasZoomed = isZoomed
                        isZoomed = scale > 1f

                        if (!wasZoomed && isZoomed) {
                            overlayStateBeforeZoom = isOverlayVisible
                            isOverlayVisible = false
                            isInfoVisible = false
                        } else if (wasZoomed && !isZoomed) {
                            isOverlayVisible = overlayStateBeforeZoom
                        }
                    },
                    resetSignal = exitRequestId,
                    onMetricsChanged = { metrics ->
                        metricsByPhotoId[photo.id] = metrics
                    }
                )

                val shouldShowDebugOverlay = isOverlayVisible
                    && !isZoomed
                    && !isTransitionActive
                    && viewerContext == ViewerContext.SELECT
                    && settings.debugOptions.showDebugOverlay
                    && (settings.debugOptions.showFaceBoundingBox || settings.debugOptions.showScoreOverlay)

                AnimatedVisibility(
                    visible = shouldShowDebugOverlay,
                    enter = ContainerTransformSpec.overlayEnter(),
                    exit = ContainerTransformSpec.overlayExit()
                ) {
                    DebugOverlay(
                        photo = photo,
                        debugOptions = settings.debugOptions,
                        thresholds = settings.smartDiscardThresholds
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isOverlayVisible && !isTransitionActive,
            enter = ContainerTransformSpec.overlayEnter(),
            exit = ContainerTransformSpec.overlayExit(),
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

        if (isInfoVisible && isOverlayVisible && !isTransitionActive && currentPhoto != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 72.dp, end = 16.dp)
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

        if (viewerContext == ViewerContext.SELECT) {
            AnimatedVisibility(
                visible = isOverlayVisible && !isTransitionActive,
                enter = ContainerTransformSpec.overlayEnter(),
                exit = ContainerTransformSpec.overlayExit(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ViewerBottomOverlay(
                    isSelected = isSelected,
                    isRejected = isRejected,
                    rejectReason = rejectReason,
                    isButtonLocked = isButtonLocked,
                    onSelectClick = {
                        if (!isButtonLocked) currentPhoto?.let { onSelectClick(it.id) }
                    },
                    onRejectClick = {
                        if (!isButtonLocked) currentPhoto?.let { onRejectClick(it.id) }
                    }
                )
            }
        }
    }
}
