package com.cola.photoanalyzer

import android.util.Log
import com.cola.photoanalyzer.detection.DetectedFace
import com.cola.photoanalyzer.detection.FaceDetector
import com.cola.photoanalyzer.detection.PoseDetector
import com.cola.photoanalyzer.detection.mlkit.MlKitFaceDetector
import com.cola.photoanalyzer.detection.mlkit.MlKitPoseDetector
import com.cola.photoanalyzer.internal.BitmapLoader
import com.cola.photoanalyzer.internal.DefectChecker
import com.cola.photoanalyzer.internal.ImageMetrics
import com.cola.photoanalyzer.internal.ScoreCalculator
import com.cola.photoanalyzer.model.AnalysisConfig
import com.cola.photoanalyzer.model.AnalysisInput
import com.cola.photoanalyzer.model.AnalysisOutput
import com.cola.photoanalyzer.model.BoundingBox
import com.cola.photoanalyzer.model.DefectType
import com.cola.photoanalyzer.model.FaceInfo
import com.cola.photoanalyzer.model.PhotoCategory
import com.cola.photoanalyzer.model.QualityScore
import com.cola.photoanalyzer.model.RawMetrics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * 사진 품질 분석 엔진.
 *
 * 사용법:
 * ```
 * val analyzer = PhotoAnalyzer.create()
 * val result = analyzer.analyze(AnalysisInput(id=1, filePath="..."))
 * ```
 */
class PhotoAnalyzer(
    private val config: AnalysisConfig = AnalysisConfig(),
    private val faceDetector: FaceDetector,
    private val poseDetector: PoseDetector,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    maxConcurrency: Int = DEFAULT_CONCURRENCY
) {
    private val semaphore = Semaphore(maxConcurrency)

    companion object {
        const val DEFAULT_CONCURRENCY = 4
        private const val TAG = "PhotoAnalyzer"

        fun create(
            config: AnalysisConfig = AnalysisConfig(),
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            maxConcurrency: Int = DEFAULT_CONCURRENCY
        ): PhotoAnalyzer {
            return PhotoAnalyzer(
                config = config,
                faceDetector = MlKitFaceDetector(),
                poseDetector = MlKitPoseDetector(),
                dispatcher = dispatcher,
                maxConcurrency = maxConcurrency
            )
        }
    }

    /** 단일 사진 분석 */
    suspend fun analyze(input: AnalysisInput): AnalysisOutput =
        withContext(dispatcher) {
            semaphore.withPermit {
                analyzeInternal(input)
            }
        }

    /** 배치 분석 */
    suspend fun analyzeBatch(inputs: List<AnalysisInput>): List<AnalysisOutput> =
        withContext(dispatcher) {
            inputs.map { input ->
                async {
                    semaphore.withPermit {
                        analyzeInternal(input)
                    }
                }
            }.awaitAll()
        }

    private suspend fun analyzeInternal(input: AnalysisInput): AnalysisOutput {
        return try {
            // 1. 비트맵 로드
            val loadResult = BitmapLoader.loadBitmap(input.filePath, config.maxBitmapSize)
                ?: return errorOutput(input.id, "Image load failed")

            val bitmap = loadResult.first
            val originalSize = loadResult.second

            // 2. 전체 선명도
            val sharpnessRaw = ImageMetrics.computeLaplacianVariance(bitmap)

            // 3. 얼굴 감지
            val allDetectedFaces = faceDetector.detect(bitmap)

            // 4. 유효 얼굴 필터링
            val validFaces = allDetectedFaces.filter {
                (it.boundingBox.right - it.boundingBox.left) >= bitmap.width * config.minFaceRatio
            }.takeIf { it.isNotEmpty() }

            val bestFace = validFaces?.maxByOrNull {
                val w = it.boundingBox.right - it.boundingBox.left
                val h = it.boundingBox.bottom - it.boundingBox.top
                w * h
            }

            // 5. 결함 체크
            val defect = DefectChecker.check(
                allDetectedFaces = allDetectedFaces,
                validFaces = validFaces,
                bestFace = bestFace,
                bitmap = bitmap,
                sharpnessRaw = sharpnessRaw,
                config = config,
                poseDetector = poseDetector
            )

            // 6. 카테고리 판별
            val category = when {
                validFaces == null || validFaces.isEmpty() -> PhotoCategory.LANDSCAPE
                validFaces.size >= 2 -> PhotoCategory.GROUP_PORTRAIT
                else -> PhotoCategory.SOLO_PORTRAIT
            }

            // 7. 스케일 계산
            val scaleX = originalSize.width.toDouble() / bitmap.width
            val scaleY = originalSize.height.toDouble() / bitmap.height

            // 8. 얼굴 정보 구성
            val primaryBox = bestFace?.let { scaleBoundingBox(it.boundingBox, scaleX, scaleY) }
            val allBoxes = (validFaces ?: allDetectedFaces).map {
                scaleBoundingBox(it.boundingBox, scaleX, scaleY)
            }

            // 9. Raw metrics 수집
            val leftEyeProb = bestFace?.leftEyeOpenProb?.toDouble() ?: 0.0
            val rightEyeProb = bestFace?.rightEyeOpenProb?.toDouble() ?: 0.0
            val eyeOpenProb = (leftEyeProb + rightEyeProb) / 2.0
            val smileProb = bestFace?.smileProb?.toDouble() ?: 0.0

            val allEyesOpenProbs = validFaces?.map { face ->
                val l = face.leftEyeOpenProb?.toDouble() ?: 0.0
                val r = face.rightEyeOpenProb?.toDouble() ?: 0.0
                (l + r) / 2.0
            } ?: emptyList()
            val allSmileProbs = validFaces?.map { face ->
                face.smileProb?.toDouble() ?: 0.0
            } ?: emptyList()
            val minEyeOpen = if (allEyesOpenProbs.isNotEmpty()) allEyesOpenProbs.min() else 0.0
            val avgEyeOpen = if (allEyesOpenProbs.isNotEmpty()) allEyesOpenProbs.average() else 0.0

            // 10. 얼굴 영역 선명도
            val faceSharpnessRaw = if (bestFace != null) {
                ImageMetrics.computeLaplacianVariance(bitmap, bestFace.boundingBox)
            } else 0.0

            // 11. 점수 계산
            val score = when {
                // NO_FACE defect인데 포즈가 감지된 경우 (defect == null && allDetectedFaces.isEmpty())
                allDetectedFaces.isEmpty() && defect == null -> {
                    ScoreCalculator.calculateLandscapeScore(bitmap, sharpnessRaw)
                }
                // NO_FACE defect (포즈도 없음) → 풍경 점수는 계산하되 defect 표시
                defect == DefectType.NO_FACE -> {
                    ScoreCalculator.calculateLandscapeScore(bitmap, sharpnessRaw)
                }
                // 결함이 있지만 NO_FACE가 아닌 경우 → 점수 계산
                defect != null && bestFace != null -> {
                    calculateScoreForCategory(bitmap, category, bestFace, validFaces!!, sharpnessRaw, faceSharpnessRaw, smileProb, eyeOpenProb, minEyeOpen)
                }
                // 결함 없음 → 정상 점수 계산
                bestFace != null && validFaces != null -> {
                    calculateScoreForCategory(bitmap, category, bestFace, validFaces, sharpnessRaw, faceSharpnessRaw, smileProb, eyeOpenProb, minEyeOpen)
                }
                // TOO_SMALL 등 validFaces가 없는 defect
                else -> QualityScore()
            }

            val rawMetrics = RawMetrics(
                laplacianVariance = sharpnessRaw,
                faceLaplacianVariance = faceSharpnessRaw,
                eyeOpenProb = eyeOpenProb,
                leftEyeOpenProb = leftEyeProb,
                rightEyeOpenProb = rightEyeProb,
                smileProb = smileProb,
                allEyesOpenProbs = allEyesOpenProbs,
                allSmileProbs = allSmileProbs,
                minEyeOpenProb = minEyeOpen,
                avgEyeOpenProb = avgEyeOpen
            )

            val faceInfo = FaceInfo(
                count = (validFaces?.size ?: 0).coerceAtLeast(allDetectedFaces.size.coerceAtMost(if (validFaces == null) allDetectedFaces.size else validFaces.size)),
                primaryBox = primaryBox,
                allBoxes = allBoxes,
                analyzedImageWidth = originalSize.width,
                analyzedImageHeight = originalSize.height
            )

            Log.d(TAG, "analyze: ID=${input.id}, defect=$defect, category=$category, " +
                "score=${score.total}")

            AnalysisOutput(
                inputId = input.id,
                hasDefect = defect != null,
                defectType = defect,
                category = category,
                score = score,
                faces = faceInfo,
                metrics = rawMetrics
            )
        } catch (e: Exception) {
            Log.e(TAG, "analyze: ID=${input.id}, error=${e.message}", e)
            errorOutput(input.id, "Analysis error: ${e.message}")
        }
    }

    private fun calculateScoreForCategory(
        bitmap: android.graphics.Bitmap,
        category: PhotoCategory,
        bestFace: DetectedFace,
        validFaces: List<DetectedFace>,
        rawSharpness: Double,
        faceSharpnessRaw: Double,
        smileProb: Double,
        eyeOpenProb: Double,
        minEyeOpen: Double
    ): QualityScore {
        return when (category) {
            PhotoCategory.SOLO_PORTRAIT -> ScoreCalculator.calculateSoloScore(
                bitmap, bestFace, faceSharpnessRaw, smileProb, eyeOpenProb
            )
            PhotoCategory.GROUP_PORTRAIT -> ScoreCalculator.calculateGroupScore(
                bitmap, bestFace, validFaces, rawSharpness, minEyeOpen
            )
            PhotoCategory.LANDSCAPE -> ScoreCalculator.calculateLandscapeScore(
                bitmap, rawSharpness
            )
        }
    }

    private fun scaleBoundingBox(box: BoundingBox, scaleX: Double, scaleY: Double): BoundingBox {
        return BoundingBox(
            left = (box.left * scaleX).toInt(),
            top = (box.top * scaleY).toInt(),
            right = (box.right * scaleX).toInt(),
            bottom = (box.bottom * scaleY).toInt()
        )
    }

    private fun errorOutput(id: Long, reason: String): AnalysisOutput {
        return AnalysisOutput(
            inputId = id,
            hasDefect = true,
            defectType = null,
            category = PhotoCategory.LANDSCAPE,
            score = QualityScore(),
            faces = FaceInfo(),
            metrics = RawMetrics()
        )
    }
}
