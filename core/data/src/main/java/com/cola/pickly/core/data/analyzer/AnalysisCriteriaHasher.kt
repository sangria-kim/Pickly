package com.cola.pickly.core.data.analyzer

import com.cola.pickly.core.data.settings.SmartDiscardThresholds
import com.cola.pickly.core.model.RejectReason
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * 분석 기준의 SHA-256 해시를 생성하여 캐시 유효성 검증에 사용합니다.
 */
object AnalysisCriteriaHasher {
    /**
     * 분석 기준(criteria + thresholds)의 SHA-256 해시를 생성합니다.
     *
     * @param criteria 제외 후보 기준 (Set 순서 무관)
     * @param thresholds 임계값 설정
     * @return 64자리 16진수 해시 문자열
     */
    fun computeHash(
        criteria: Set<RejectReason>,
        thresholds: SmartDiscardThresholds
    ): String {
        // 1. 정렬된 문자열 생성 (Set 순서 보장)
        val criteriaStr = criteria.map { it.name }.sorted().joinToString(",")

        // 2. Thresholds 문자열화
        val thresholdsStr = listOf(
            "blur=${thresholds.blurThreshold}",
            "faceSize=${thresholds.minFaceSize}",
            "headAngle=${thresholds.headAngleLimit}",
            "eyeOpen=${thresholds.eyeOpenThreshold}",
            "smile=${thresholds.smileExceptionThreshold}"
        ).joinToString(";")

        // 3. SHA-256 해싱
        val input = "criteria:$criteriaStr|thresholds:$thresholdsStr"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
