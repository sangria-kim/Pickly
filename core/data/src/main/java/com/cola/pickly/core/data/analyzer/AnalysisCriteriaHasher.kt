package com.cola.pickly.core.data.analyzer

import com.cola.photoanalyzer.model.AnalysisConfig
import com.cola.pickly.core.data.settings.SmartDiscardThresholds
import com.cola.pickly.core.model.RejectReason
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * 분석 기준의 SHA-256 해시를 생성하여 캐시 유효성 검증에 사용합니다.
 */
object AnalysisCriteriaHasher {
    /**
     * 분석 기준(criteria + thresholds + algorithmVersion)의 SHA-256 해시를 생성합니다.
     */
    fun computeHash(
        criteria: Set<RejectReason>,
        thresholds: SmartDiscardThresholds
    ): String {
        val criteriaStr = criteria.map { it.name }.sorted().joinToString(",")

        val thresholdsStr = listOf(
            "blur=${thresholds.blurThreshold}",
            "faceSize=${thresholds.minFaceSize}",
            "eyeOpen=${thresholds.eyeOpenThreshold}",
            "smile=${thresholds.smileExceptionThreshold}"
        ).joinToString(";")

        val algorithmVersion = AnalysisConfig.CURRENT_ALGORITHM_VERSION
        val input = "v:$algorithmVersion|criteria:$criteriaStr|thresholds:$thresholdsStr"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
