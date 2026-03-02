package com.cola.pickly.core.data.analyzer

import com.cola.photoanalyzer.PhotoAnalyzer
import com.cola.photoanalyzer.model.AnalysisInput
import com.cola.photoanalyzer.model.AnalysisOutput
import com.cola.photoanalyzer.model.DefectType
import com.cola.photoanalyzer.model.PhotoCategory
import com.cola.pickly.core.model.FaceBoundingBox
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoType
import com.cola.pickly.core.model.RecommendationScore
import com.cola.pickly.core.model.RejectReason

/**
 * Pickly 전용 어댑터: PhotoAnalyzer → RecommendationScore 변환.
 */
class PhotoQualityAnalyzer(
    private val analyzer: PhotoAnalyzer,
    private val enabledCriteria: Set<RejectReason>
) {
    suspend fun analyze(photo: Photo): RecommendationScore {
        val input = AnalysisInput(photo.id, photo.filePath, photo.takenAt)
        val output = analyzer.analyze(input)
        return output.toRecommendationScore()
    }
}

// ─── 변환 함수들 ───

fun AnalysisOutput.toRecommendationScore(): RecommendationScore {
    return RecommendationScore(
        totalScore = score.total,
        sharpnessScore = score.sharpness,
        expressionScore = score.expression,
        lightingScore = score.lighting,
        compositionScore = score.composition,
        photoType = category.toPhotoType(),
        faceCount = faces.count,
        isCutoff = hasDefect,
        cutoffReason = defectType?.label,
        rejectReason = defectType?.toRejectReason(),
        rawSharpness = metrics.laplacianVariance,
        eyeOpenProb = metrics.eyeOpenProb,
        leftEyeOpenProb = metrics.leftEyeOpenProb,
        rightEyeOpenProb = metrics.rightEyeOpenProb,
        smileProb = metrics.smileProb,
        faceBoundingBox = faces.primaryBox?.let {
            FaceBoundingBox(it.left, it.top, it.right, it.bottom)
        },
        allFaceBoundingBoxes = faces.allBoxes.map {
            FaceBoundingBox(it.left, it.top, it.right, it.bottom)
        },
        analyzedWidth = faces.analyzedImageWidth,
        analyzedHeight = faces.analyzedImageHeight,
        allEyesOpenProbs = metrics.allEyesOpenProbs,
        allSmileProbs = metrics.allSmileProbs,
        minEyeOpenProb = metrics.minEyeOpenProb,
        avgEyeOpenProb = metrics.avgEyeOpenProb,
        faceSharpness = metrics.faceLaplacianVariance
    )
}

fun PhotoCategory.toPhotoType(): PhotoType = when (this) {
    PhotoCategory.SOLO_PORTRAIT -> PhotoType.SOLO_PORTRAIT
    PhotoCategory.GROUP_PORTRAIT -> PhotoType.GROUP_PORTRAIT
    PhotoCategory.LANDSCAPE -> PhotoType.LANDSCAPE
}

fun DefectType.toRejectReason(): RejectReason = when (this) {
    DefectType.NO_FACE -> RejectReason.NO_FACE
    DefectType.BLURRY -> RejectReason.BLURRY
    DefectType.TOO_SMALL -> RejectReason.TOO_SMALL
    DefectType.OCCLUDED -> RejectReason.OCCLUDED
    DefectType.EYES_CLOSED -> RejectReason.EYES_CLOSED
    DefectType.CROPPED -> RejectReason.CROPPED
}

fun RejectReason.toDefectType(): DefectType = when (this) {
    RejectReason.NO_FACE -> DefectType.NO_FACE
    RejectReason.BLURRY -> DefectType.BLURRY
    RejectReason.TOO_SMALL -> DefectType.TOO_SMALL
    RejectReason.OCCLUDED -> DefectType.OCCLUDED
    RejectReason.EYES_CLOSED -> DefectType.EYES_CLOSED
    RejectReason.CROPPED -> DefectType.CROPPED
}

fun com.cola.pickly.core.data.settings.SmartDiscardThresholds.toAnalysisConfig(
    criteria: Set<RejectReason>
): com.cola.photoanalyzer.model.AnalysisConfig {
    return com.cola.photoanalyzer.model.AnalysisConfig(
        blurThreshold = blurThreshold,
        minFaceRatio = minFaceSize,
        eyeOpenThreshold = eyeOpenThreshold,
        smileExceptionThreshold = smileExceptionThreshold,
        enabledDefects = criteria.map { it.toDefectType() }.toSet()
    )
}
