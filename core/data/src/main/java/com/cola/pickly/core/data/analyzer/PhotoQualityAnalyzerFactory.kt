package com.cola.pickly.core.data.analyzer

import com.cola.photoanalyzer.PhotoAnalyzer
import com.cola.pickly.core.data.settings.SmartDiscardThresholds
import com.cola.pickly.core.model.RejectReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PhotoQualityAnalyzer의 인스턴스를 생성하는 Factory.
 */
@Singleton
class PhotoQualityAnalyzerFactory @Inject constructor() {

    fun create(
        thresholds: SmartDiscardThresholds,
        enabledCriteria: Set<RejectReason> = RejectReason.entries.toSet(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): PhotoQualityAnalyzer {
        val config = thresholds.toAnalysisConfig(enabledCriteria)
        val analyzer = PhotoAnalyzer.create(config, dispatcher = dispatcher)
        return PhotoQualityAnalyzer(analyzer, enabledCriteria)
    }
}
