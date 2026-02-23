package com.cola.pickly.core.data.analyzer

import com.cola.pickly.core.data.settings.SmartDiscardThresholds
import com.cola.pickly.core.model.RejectReason
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PhotoQualityAnalyzer의 인스턴스를 생성하는 Factory.
 *
 * 런타임에 사용자 설정(SmartDiscardThresholds)을 적용한 analyzer를 생성할 수 있습니다.
 */
@Singleton
class PhotoQualityAnalyzerFactory @Inject constructor(
    private val faceDetectorHelper: FaceDetectorHelper,
    private val poseDetectorHelper: PoseDetectorHelper
) {
    /**
     * 주어진 임계값 설정으로 PhotoQualityAnalyzer 인스턴스를 생성합니다.
     */
    fun create(
        thresholds: SmartDiscardThresholds,
        enabledCriteria: Set<RejectReason> = RejectReason.entries.toSet()
    ): PhotoQualityAnalyzer {
        return PhotoQualityAnalyzer(faceDetectorHelper, poseDetectorHelper, thresholds, enabledCriteria)
    }
}
