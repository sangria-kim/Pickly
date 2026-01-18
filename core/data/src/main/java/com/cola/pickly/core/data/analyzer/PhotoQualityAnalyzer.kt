package com.cola.pickly.core.data.analyzer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import com.cola.pickly.core.model.FaceBoundingBox
import com.cola.pickly.core.model.RecommendationScore
import com.cola.pickly.core.model.Photo
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 사진 품질을 분석하여 점수를 계산하는 분석기입니다.
 * 얼굴 인식, 선명도, 조명, 구도 등을 평가합니다.
 */
class PhotoQualityAnalyzer(
    private val faceDetectorHelper: FaceDetectorHelper
) {

    /**
     * 사진을 분석하여 품질 점수를 반환합니다.
     */
    suspend fun analyze(photo: Photo): RecommendationScore = withContext(Dispatchers.Default) {
        try {
            // 1. 비트맵 로드 (최적화를 위해 리사이징 + EXIF 회전 적용)
            val loadResult = loadBitmap(photo.filePath)
                ?: return@withContext RecommendationScore(isCutoff = true, cutoffReason = "Image load failed")
            
            val bitmap = loadResult.first
            val originalSize = loadResult.second

            // 2. 선명도 계산 (가장 먼저 체크하여 심각한 블러 제거)
            val sharpnessRaw = calculateSharpness(bitmap)
            if (sharpnessRaw < SHAKE_THRESHOLD) {
                Log.d(TAG, "analyze: ID=${photo.id}, Too blurry (var=$sharpnessRaw)")
                return@withContext RecommendationScore(
                    isCutoff = true,
                    cutoffReason = "Severe shaking (Blurry)",
                    rawSharpness = sharpnessRaw // Raw 값 저장
                )
            }

            // 3. 얼굴 감지 (작은 얼굴까지 모두 포함)
            val allDetectedFaces = faceDetectorHelper.detectFaces(bitmap)

            // 4. 컷오프: 얼굴 미검출
            if (allDetectedFaces.isEmpty()) {
                Log.d(TAG, "analyze: ID=${photo.id}, No face detected")
                return@withContext RecommendationScore(
                    faceCount = 0,
                    isCutoff = true,
                    cutoffReason = "No face detected",
                    rawSharpness = sharpnessRaw
                )
            }
            
            // 5. 유효 얼굴 필터링 (너비가 이미지의 5% 이상인 얼굴만)
            val validFaces = allDetectedFaces.filter { 
                it.boundingBox.width() >= bitmap.width * 0.05f
            }
            
            // 6. 컷오프: 유효한 크기의 얼굴이 없음
            if (validFaces.isEmpty()) {
                Log.d(TAG, "analyze: ID=${photo.id}, Faces too small")
                return@withContext RecommendationScore(
                    faceCount = allDetectedFaces.size, // 검출은 됐으나 너무 작음
                    isCutoff = true,
                    cutoffReason = "Faces too small ( < 5% )",
                    rawSharpness = sharpnessRaw,
                    // 작은 얼굴들이라도 박스는 표시해줌
                    allFaceBoundingBoxes = getScaledFaceBoundingBoxes(allDetectedFaces, originalSize, bitmap.width, bitmap.height)
                )
            }

            // 7. 대표 얼굴 선정 (유효 얼굴 중 가장 큰 얼굴)
            val bestFace = validFaces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!

            // Raw 값 추출
            val smileProb = bestFace.smilingProbability ?: 0.0f
            val leftEyeOpen = bestFace.leftEyeOpenProbability ?: 0.0f
            val rightEyeOpen = bestFace.rightEyeOpenProbability ?: 0.0f
            val eyeOpenProb = (leftEyeOpen + rightEyeOpen) / 2.0
            val headEulerX = bestFace.headEulerAngleX
            val headEulerY = bestFace.headEulerAngleY

            // 8. 컷오프: 얼굴 잘림 확인 (Face Crop)
            if (isFaceCropped(bestFace, bitmap.width, bitmap.height)) {
                Log.d(TAG, "analyze: ID=${photo.id}, Face cropped")
                return@withContext RecommendationScore(
                    faceCount = validFaces.size,
                    isCutoff = true,
                    cutoffReason = "Face cropped at edges",
                    rawSharpness = sharpnessRaw,
                    eyeOpenProb = eyeOpenProb.toDouble(),
                    leftEyeOpenProb = leftEyeOpen.toDouble(),
                    rightEyeOpenProb = rightEyeOpen.toDouble(),
                    smileProb = smileProb.toDouble(),
                    headEulerAngleX = headEulerX,
                    headEulerAngleY = headEulerY,
                    faceBoundingBox = getScaledFaceBoundingBox(bestFace.boundingBox, originalSize, bitmap.width, bitmap.height),
                    allFaceBoundingBoxes = getScaledFaceBoundingBoxes(validFaces, originalSize, bitmap.width, bitmap.height)
                )
            }

            // 9. 컷오프: 주요 부위 가림 확인 (Occlusion)
            if (isFaceOccluded(bestFace)) {
                Log.d(TAG, "analyze: ID=${photo.id}, Face occluded")
                return@withContext RecommendationScore(
                    faceCount = validFaces.size,
                    isCutoff = true,
                    cutoffReason = "Face occluded (Nose/Mouth hidden)",
                    rawSharpness = sharpnessRaw,
                    eyeOpenProb = eyeOpenProb.toDouble(),
                    leftEyeOpenProb = leftEyeOpen.toDouble(),
                    rightEyeOpenProb = rightEyeOpen.toDouble(),
                    smileProb = smileProb.toDouble(),
                    headEulerAngleX = headEulerX,
                    headEulerAngleY = headEulerY,
                    faceBoundingBox = getScaledFaceBoundingBox(bestFace.boundingBox, originalSize, bitmap.width, bitmap.height),
                    allFaceBoundingBoxes = getScaledFaceBoundingBoxes(validFaces, originalSize, bitmap.width, bitmap.height)
                )
            }

            // 10. 컷오프: 눈 감음 여부 (웃고 있지 않은데 눈을 감은 경우)
            val isEyesClosed = (leftEyeOpen < EYES_OPEN_THRESHOLD || rightEyeOpen < EYES_OPEN_THRESHOLD)
            val isSmiling = smileProb > SMILE_THRESHOLD

            if (isEyesClosed && !isSmiling) {
                Log.d(TAG, "analyze: ID=${photo.id}, Eyes closed (L=$leftEyeOpen, R=$rightEyeOpen)")
                return@withContext RecommendationScore(
                    faceCount = validFaces.size,
                    isCutoff = true,
                    cutoffReason = "Eyes closed",
                    rawSharpness = sharpnessRaw,
                    eyeOpenProb = eyeOpenProb.toDouble(),
                    leftEyeOpenProb = leftEyeOpen.toDouble(),
                    rightEyeOpenProb = rightEyeOpen.toDouble(),
                    smileProb = smileProb.toDouble(),
                    headEulerAngleX = headEulerX,
                    headEulerAngleY = headEulerY,
                    faceBoundingBox = getScaledFaceBoundingBox(bestFace.boundingBox, originalSize, bitmap.width, bitmap.height),
                    allFaceBoundingBoxes = getScaledFaceBoundingBoxes(validFaces, originalSize, bitmap.width, bitmap.height)
                )
            }
            
            // 11. 컷오프: 과도한 고개 돌림 (Head Pose)
            if (isHeadTurnedTooMuch(bestFace)) {
                Log.d(TAG, "analyze: ID=${photo.id}, Head turned too much (X=$headEulerX, Y=$headEulerY)")
                return@withContext RecommendationScore(
                    faceCount = validFaces.size,
                    isCutoff = true,
                    cutoffReason = "Head turned too much",
                    rawSharpness = sharpnessRaw,
                    eyeOpenProb = eyeOpenProb.toDouble(),
                    leftEyeOpenProb = leftEyeOpen.toDouble(),
                    rightEyeOpenProb = rightEyeOpen.toDouble(),
                    smileProb = smileProb.toDouble(),
                    headEulerAngleX = headEulerX,
                    headEulerAngleY = headEulerY,
                    faceBoundingBox = getScaledFaceBoundingBox(bestFace.boundingBox, originalSize, bitmap.width, bitmap.height),
                    allFaceBoundingBoxes = getScaledFaceBoundingBoxes(validFaces, originalSize, bitmap.width, bitmap.height)
                )
            }

            // 12. 좌표 변환 비율 계산
            val scaleX = originalSize.width.toDouble() / bitmap.width
            val scaleY = originalSize.height.toDouble() / bitmap.height

            // 13. 점수 계산
            val score = calculateScore(
                bitmap, bestFace, validFaces, scaleX, scaleY, originalSize, 
                sharpnessRaw, smileProb.toDouble(), eyeOpenProb.toDouble(), 
                leftEyeOpen.toDouble(), rightEyeOpen.toDouble(), 
                headEulerX, headEulerY
            )
            
            Log.d(TAG, "analyze: ID=${photo.id}, Score=${score.totalScore} " +
                    "(S=${score.sharpnessScore}, E=${score.expressionScore}, " +
                    "L=${score.lightingScore}, C=${score.compositionScore})")
            
            score

        } catch (e: Exception) {
            e.printStackTrace()
            RecommendationScore(isCutoff = true, cutoffReason = "Analysis error: ${e.message}")
        }
    }

    // 좌표 변환 헬퍼 함수
    private fun getScaledFaceBoundingBox(box: Rect, originalSize: Size, bitmapWidth: Int, bitmapHeight: Int): FaceBoundingBox {
        val scaleX = originalSize.width.toDouble() / bitmapWidth
        val scaleY = originalSize.height.toDouble() / bitmapHeight
        return FaceBoundingBox(
            left = (box.left * scaleX).toInt(),
            top = (box.top * scaleY).toInt(),
            right = (box.right * scaleX).toInt(),
            bottom = (box.bottom * scaleY).toInt()
        )
    }

    private fun getScaledFaceBoundingBoxes(faces: List<Face>, originalSize: Size, bitmapWidth: Int, bitmapHeight: Int): List<FaceBoundingBox> {
        return faces.map { getScaledFaceBoundingBox(it.boundingBox, originalSize, bitmapWidth, bitmapHeight) }
    }
    
    // ... helper functions ... (isFaceCropped, isFaceOccluded, isHeadTurnedTooMuch are same) ...

    private fun isFaceCropped(face: Face, imgWidth: Int, imgHeight: Int): Boolean {
        val box = face.boundingBox
        val margin = 0
        return box.left <= margin || box.top <= margin || 
               box.right >= imgWidth - margin || box.bottom >= imgHeight - margin
    }

    private fun isFaceOccluded(face: Face): Boolean {
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
        return nose == null || mouthLeft == null || mouthRight == null || mouthBottom == null
    }

    private fun isHeadTurnedTooMuch(face: Face): Boolean {
        val pitch = abs(face.headEulerAngleX)
        val yaw = abs(face.headEulerAngleY)
        return pitch > HEAD_ROTATION_THRESHOLD || yaw > HEAD_ROTATION_THRESHOLD
    }

    private fun calculateScore(
        bitmap: Bitmap, 
        bestFace: Face,
        allValidFaces: List<Face>,
        scaleX: Double,
        scaleY: Double,
        originalSize: Size,
        sharpnessRaw: Double,
        smileProb: Double,
        eyeOpenProb: Double,
        leftEyeProb: Double,
        rightEyeProb: Double,
        headEulerX: Float,
        headEulerY: Float
    ): RecommendationScore {
        
        // 1. 선명도
        val sharpness = normalizeSharpness(sharpnessRaw)

        // 2. 표정 (웃음 + 눈 뜸)
        val expressionRaw = (smileProb * 0.6 + eyeOpenProb * 0.4) * 100
        val expression = expressionRaw.coerceIn(0.0, 100.0)

        // 3. 조명
        val lighting = calculateLighting(bitmap, bestFace)

        // 4. 구도
        val composition = calculateComposition(bitmap, bestFace)

        // 5. 배경
        val background = 50.0

        val total = (sharpness * 0.3) + 
                    (expression * 0.25) + 
                    (lighting * 0.20) + 
                    (composition * 0.15) + 
                    (background * 0.10)
        
        val bestBox = bestFace.boundingBox
        val bestFaceBoundingBox = FaceBoundingBox(
            left = (bestBox.left * scaleX).toInt(),
            top = (bestBox.top * scaleY).toInt(),
            right = (bestBox.right * scaleX).toInt(),
            bottom = (bestBox.bottom * scaleY).toInt()
        )

        val allFaceBoundingBoxes = allValidFaces.map { face ->
            val box = face.boundingBox
            FaceBoundingBox(
                left = (box.left * scaleX).toInt(),
                top = (box.top * scaleY).toInt(),
                right = (box.right * scaleX).toInt(),
                bottom = (box.bottom * scaleY).toInt()
            )
        }

        return RecommendationScore(
            totalScore = total.coerceIn(0.0, 100.0),
            sharpnessScore = sharpness,
            expressionScore = expression,
            lightingScore = lighting,
            compositionScore = composition,
            backgroundScore = background,
            faceCount = allValidFaces.size,
            isCutoff = false,
            faceBoundingBox = bestFaceBoundingBox,
            allFaceBoundingBoxes = allFaceBoundingBoxes,
            analyzedWidth = originalSize.width,
            analyzedHeight = originalSize.height,
            // Raw Data 저장
            rawSharpness = sharpnessRaw,
            eyeOpenProb = eyeOpenProb,
            leftEyeOpenProb = leftEyeProb,
            rightEyeOpenProb = rightEyeProb,
            smileProb = smileProb,
            headEulerAngleX = headEulerX,
            headEulerAngleY = headEulerY
        )
    }
    
    // ... existing helpers ...
    private fun calculateSharpness(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val grayPixels = pixels.map { color ->
            (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114).toInt()
        }

        var variance = 0.0
        var mean = 0.0
        val laplacianValues = mutableListOf<Int>()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = grayPixels[idx]
                val top = grayPixels[idx - width]
                val bottom = grayPixels[idx + width]
                val left = grayPixels[idx - 1]
                val right = grayPixels[idx + 1]
                val laplacian = top + bottom + left + right - (4 * center)
                laplacianValues.add(laplacian)
                mean += laplacian
            }
        }
        if (laplacianValues.isEmpty()) return 0.0
        mean /= laplacianValues.size
        for (valVal in laplacianValues) {
            variance += (valVal - mean).pow(2)
        }
        return variance / laplacianValues.size
    }

    private fun normalizeSharpness(variance: Double): Double {
        return (variance / 10.0).coerceIn(0.0, 100.0)
    }

    private fun calculateLighting(bitmap: Bitmap, face: Face): Double {
        val box = face.boundingBox
        val left = box.left.coerceAtLeast(0)
        val top = box.top.coerceAtLeast(0)
        val right = box.right.coerceAtMost(bitmap.width)
        val bottom = box.bottom.coerceAtMost(bitmap.height)
        if (left >= right || top >= bottom) return 0.0

        val width = right - left
        val height = bottom - top
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, left, top, width, height)
        var totalLuminance = 0.0
        for (pixel in pixels) {
             val r = Color.red(pixel)
             val g = Color.green(pixel)
             val b = Color.blue(pixel)
             totalLuminance += (0.299 * r + 0.587 * g + 0.114 * b)
        }
        val avgLuminance = totalLuminance / pixels.size
        val dist = abs(avgLuminance - 128)
        return ((1.0 - (dist / 128.0)) * 100).coerceIn(0.0, 100.0)
    }

    private fun calculateComposition(bitmap: Bitmap, face: Face): Double {
        val imgWidth = bitmap.width.toDouble()
        val imgHeight = bitmap.height.toDouble()
        val faceCenterX = face.boundingBox.centerX().toDouble()
        val faceCenterY = face.boundingBox.centerY().toDouble()
        val thirdsX = listOf(imgWidth / 3.0, imgWidth * 2.0 / 3.0)
        val thirdsY = listOf(imgHeight / 3.0, imgHeight * 2.0 / 3.0)
        var minDistance = Double.MAX_VALUE
        for (tx in thirdsX) {
            for (ty in thirdsY) {
                val dx = faceCenterX - tx
                val dy = faceCenterY - ty
                val distance = sqrt(dx*dx + dy*dy)
                if (distance < minDistance) {
                    minDistance = distance
                }
            }
        }
        val maxDist = sqrt(imgWidth*imgWidth + imgHeight*imgHeight)
        return ((1.0 - (minDistance / (maxDist * 0.5))) * 100).coerceIn(0.0, 100.0)
    }
    
    private fun loadBitmap(path: String): Pair<Bitmap, Size>? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        if (originalWidth <= 0 || originalHeight <= 0) return null
        val targetSize = 1024
        var sampleSize = 1
        while (options.outWidth / sampleSize > targetSize || options.outHeight / sampleSize > targetSize) {
            sampleSize *= 2
        }
        val finalOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeFile(path, finalOptions) ?: return null
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val rotatedBitmap = if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
             Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
        val rotatedOriginalSize = if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || 
                                      orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            Size(originalHeight, originalWidth)
        } else {
            Size(originalWidth, originalHeight)
        }
        return Pair(rotatedBitmap, rotatedOriginalSize)
    }

    companion object {
        private const val TAG = "PhotoQualityAnalyzer"
        private const val SHAKE_THRESHOLD = 100.0
        private const val EYES_OPEN_THRESHOLD = 0.5
        private const val SMILE_THRESHOLD = 0.7
        private const val HEAD_ROTATION_THRESHOLD = 30.0
    }
}