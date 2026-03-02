package com.cola.pickly.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cola.pickly.core.data.settings.DebugOptions
import com.cola.pickly.core.data.settings.SmartDiscardThresholds
import com.cola.pickly.core.model.Photo

@Composable
fun DebugOverlay(
    photo: Photo,
    debugOptions: DebugOptions,
    thresholds: SmartDiscardThresholds = SmartDiscardThresholds(),
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val score = photo.recommendationScore

        // 중요: PhotoQualityAnalyzer가 분석할 때 사용한 '회전된 이미지 크기'를 우선 사용해야 함.
        // photo.width/height는 MediaStore 원본 크기라 EXIF 회전이 반영 안 되어 있을 수 있음.
        val analyzedWidth = score?.analyzedWidth?.toFloat() ?: 0f
        val analyzedHeight = score?.analyzedHeight?.toFloat() ?: 0f

        val photoWidth = if (analyzedWidth > 0) analyzedWidth else photo.width?.toFloat() ?: 0f
        val photoHeight = if (analyzedHeight > 0) analyzedHeight else photo.height?.toFloat() ?: 0f

        if (photoWidth <= 0 || photoHeight <= 0) return@BoxWithConstraints

        // 1. 화면에 표시되는 실제 이미지 영역(Fit) 계산
        val viewWidth = maxWidth
        val viewHeight = maxHeight
        
        val imageAspect = photoWidth / photoHeight
        val viewAspect = viewWidth / viewHeight

        // ContentScale.Fit 로직에 따라 이미지의 표시 크기(Dp) 계산
        val (displayedWidth, displayedHeight) = if (imageAspect > viewAspect) {
            // 이미지가 뷰보다 가로 비율이 큼 -> 가로를 꽉 채움 (View Width 기준)
            viewWidth to (viewWidth / imageAspect)
        } else {
            // 이미지가 뷰보다 세로 비율이 큼 -> 세로를 꽉 채움 (View Height 기준)
            (viewHeight * imageAspect) to viewHeight
        }

        // 2. 실제 이미지 영역에 딱 맞는 컨테이너 Box 생성 (중앙 정렬) -> 얼굴 박스 표시용
        Box(
            modifier = Modifier
                .size(displayedWidth, displayedHeight)
                .align(Alignment.Center)
        ) {
            // 3. 얼굴 박스 그리기
            if (debugOptions.showFaceBoundingBox) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 이 Canvas는 displayedWidth/Height 크기를 가짐 (Box 크기와 동일)
                    // photoWidth/Height -> Canvas Size로 스케일링
                    // 가로/세로 비율이 미세하게 다를 수 있으므로 각각 계산하여 적용
                    val scaleX = size.width / photoWidth
                    val scaleY = size.height / photoHeight

                    score?.let { s ->
                        // 모든 얼굴 그리기 (녹색)
                        s.allFaceBoundingBoxes.forEach { box ->
                            drawRect(
                                color = Color.Green,
                                topLeft = Offset(box.left * scaleX, box.top * scaleY),
                                size = Size((box.right - box.left) * scaleX, (box.bottom - box.top) * scaleY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // 대표 얼굴 그리기 (노란색, 조금 더 두껍게) - 점수 계산의 기준
                        s.faceBoundingBox?.let { box ->
                            drawRect(
                                color = Color.Yellow,
                                topLeft = Offset(box.left * scaleX, box.top * scaleY),
                                size = Size((box.right - box.left) * scaleX, (box.bottom - box.top) * scaleY),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // 4. 점수 및 메타 정보 표시 (중앙 정렬, 2열 구조)
        if (debugOptions.showScoreOverlay && score != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 좌측 열: 점수 요약 / 판단 결과
                    Column {
                        Text(
                            text = buildString {
                                appendLine("ID: ${photo.id}")
                                appendLine("Total: %.1f".format(score.totalScore))
                                appendLine("----------")
                                appendLine("Sharpness: %.1f".format(score.sharpnessScore))
                                appendLine("Expression: %.1f".format(score.expressionScore))
                                appendLine("Lighting: %.1f".format(score.lightingScore))
                                appendLine("Composition: %.1f".format(score.compositionScore))
                                appendLine("Background: %.1f".format(score.backgroundScore))
                                appendLine("----------")
                                if (score.isCutoff) {
                                    append("❌ Cutoff: ${score.cutoffReason}")
                                } else {
                                    append("✅ Pass")
                                }
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    // 우측 열: 실제 분석 값 / 기준값
                    Column {
                        Text(
                            text = buildString {
                                appendLine("[Actual]")
                                appendLine("Blur(Var): %.1f".format(score.rawSharpness))
                                appendLine("Eye(Avg): %.2f".format(score.eyeOpenProb))
                                appendLine("Eye(L/R): %.2f / %.2f".format(score.leftEyeOpenProb, score.rightEyeOpenProb))
                                appendLine("Smile: %.2f".format(score.smileProb))
                                appendLine("----------")
                                appendLine("[Thresholds]")
                                appendLine("Blur < ${"%.1f".format(thresholds.blurThreshold)}")
                                appendLine("Eye > ${"%.2f".format(thresholds.eyeOpenThreshold)}")
                                appendLine("Smile > ${"%.2f".format(thresholds.smileExceptionThreshold)}")
                                appendLine("MinFace > ${"%.1f".format(thresholds.minFaceSize * 100)}%")
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}