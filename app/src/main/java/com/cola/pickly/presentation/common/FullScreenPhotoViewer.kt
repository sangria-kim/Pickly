package com.cola.pickly.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.cola.pickly.R
import com.cola.pickly.domain.model.WeeklyPhoto

/**
 * 사진을 전체 화면으로 보여주는 공용 뷰어입니다.
 * 좌우 스와이프를 통해 여러 사진을 탐색할 수 있습니다.
 *
 * @param photos 뷰어에서 보여줄 사진의 전체 목록
 * @param initialIndex 뷰어를 시작할 사진의 인덱스
 * @param onDismiss 뷰어를 닫을 때의 콜백
 * @param isRecommended 현재 사진이 추천 사진인지 확인하는 람다
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPhotoViewer(
    photos: List<WeeklyPhoto>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    isRecommended: (WeeklyPhoto) -> Boolean = { false }
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) {
        photos.size
    }

    var isBarVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    isBarVisible = !isBarVisible
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val photo = photos[pageIndex]
            
            Box(modifier = Modifier.fillMaxSize()) {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp
                
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.filePath)
                        .size(screenWidth.coerceAtMost(screenHeight) * 2) // 화면 크기의 2배로 제한 (고해상도 대응)
                        .build(),
                    contentDescription = "Full screen photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // 이미지가 잘리지 않고 화면에 맞게 표시
                )
                
                // 디버그 오버레이 (상단바와 함께 표시/숨김)
                AnimatedVisibility(
                    visible = isBarVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    DebugOverlay(
                        photo = photo,
                        debugOptions = AppDebugConfig
                    )
                }
            }
        }

        // 상단 메뉴바
        AnimatedVisibility(
            visible = isBarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { /* Empty Title */ },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    val currentPhoto = photos[pagerState.currentPage]
                    if (isRecommended(currentPhoto)) {
                        Box(
                            modifier = Modifier.size(48.dp), // IconButton과 동일한 크기
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_badge_heart),
                                contentDescription = "Recommended Photo",
                                tint = Color.Unspecified
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    }
}