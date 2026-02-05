package com.cola.pickly.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.ThemeMode
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.ui.theme.DebugPinkBackground
import com.cola.pickly.core.ui.theme.DebugPinkOutline
import com.cola.pickly.core.ui.theme.PicklyTheme
import com.cola.pickly.feature.settings.components.SettingsActionItem
import com.cola.pickly.feature.settings.components.SettingsCardHeader
import com.cola.pickly.feature.settings.components.SettingsCardSection
import com.cola.pickly.feature.settings.components.SettingsCardTuningHeader
import com.cola.pickly.feature.settings.components.SettingsDebugSectionTitle
import com.cola.pickly.feature.settings.components.SettingsExpandableChecklist
import com.cola.pickly.feature.settings.components.SettingsGroupLabel
import com.cola.pickly.feature.settings.components.SettingsRadioItem
import com.cola.pickly.feature.settings.components.SettingsSectionHeader
import com.cola.pickly.feature.settings.components.SettingsSliderItem
import com.cola.pickly.feature.settings.components.SettingsSwitchItem
import com.cola.pickly.feature.settings.components.SettingsTextItem

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        uiState = uiState,
        onDuplicatePolicyChanged = viewModel::setDuplicateFilenamePolicy,
        onThemeChanged = viewModel::setThemeMode,
        onSmartDiscardCriterionToggle = viewModel::toggleSmartDiscardCriterion,
        onClearCache = viewModel::clearCache,
        onVersionTap = viewModel::onVersionTap,
        onDebugOptionChanged = viewModel::setDebugOption,
        onThresholdChanged = viewModel::setThreshold,
        onResetThresholds = viewModel::resetSmartDiscardThresholds
    )
}

@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onDuplicatePolicyChanged: (DuplicateFilenamePolicy) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onSmartDiscardCriterionToggle: (RejectReason) -> Unit,
    onClearCache: () -> Unit,
    onVersionTap: () -> Unit = {},
    onDebugOptionChanged: (DebugOptionType, Boolean) -> Unit = { _, _ -> },
    onThresholdChanged: (ThresholdType, Float) -> Unit = { _, _ -> },
    onResetThresholds: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // 최상단 제목
            androidx.compose.material3.Text(
                "설정",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // 1. 사진 정리
            SettingsSectionHeader(title = "사진 정리")
            SettingsCardSection {
                SettingsCardHeader(title = "동일 파일명 처리", description = "같은 이름의 파일이 있을 때 처리 방법이에요.")
                SettingsRadioItem(
                    title = "덮어쓰기",
                    selected = uiState.duplicateFilenamePolicy == DuplicateFilenamePolicy.Overwrite,
                    onClick = { onDuplicatePolicyChanged(DuplicateFilenamePolicy.Overwrite) }
                )
                SettingsRadioItem(
                    title = "건너뛰기",
                    selected = uiState.duplicateFilenamePolicy == DuplicateFilenamePolicy.Skip,
                    onClick = { onDuplicatePolicyChanged(DuplicateFilenamePolicy.Skip) }
                )
            }

            // 2. 스마트 제외
            SettingsSectionHeader(title = "스마트 제외")
            val criteriaList = (RejectReason.entries - RejectReason.NO_FACE).map { reason ->
                reason to (reason in uiState.smartDiscardCriteria)
            }
            SettingsCardSection {
                SettingsExpandableChecklist(
                    title = "후보 기준",
                    subtitle = "이 기준에 걸린 사진은 제외해요.",
                    items = criteriaList.map { (reason, checked) -> reason.label to checked },
                    onItemCheckedChange = { index, _ ->
                        onSmartDiscardCriterionToggle(criteriaList[index].first)
                    },
                    trailingLabel = "${uiState.smartDiscardCriteria.count { it != RejectReason.NO_FACE }}개 선택됨"
                )
            }

            // 3. 화면 표시
            SettingsSectionHeader(title = "화면 표시")
            SettingsCardSection {
                SettingsCardHeader(title = "테마", description = "Pickly의 화면 테마를 바꾸요.")
                SettingsRadioItem(
                    title = "시스템 설정 따름",
                    selected = uiState.themeMode == ThemeMode.System,
                    onClick = { onThemeChanged(ThemeMode.System) }
                )
                SettingsRadioItem(
                    title = "라이트 모드",
                    selected = uiState.themeMode == ThemeMode.Light,
                    onClick = { onThemeChanged(ThemeMode.Light) }
                )
                SettingsRadioItem(
                    title = "다크 모드",
                    selected = uiState.themeMode == ThemeMode.Dark,
                    onClick = { onThemeChanged(ThemeMode.Dark) }
                )
            }

            // 4. 데이터 관리
            SettingsSectionHeader(title = "데이터 관리")
            SettingsCardSection {
                SettingsTextItem(
                    title = "캐시 용량",
                    value = uiState.cacheSizeBytes?.let { formatFileSize(it) } ?: "계산 중..."
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsActionItem(
                    title = "캐시 삭제",
                    onClick = onClearCache,
                    subtitle = "임시 데이터를 정리해요. 사진 원본은 그대로예요.",
                    enabled = !uiState.isClearingCache,
                    trailingText = if (uiState.isClearingCache) "삭제 중..." else null
                )
            }

            // 5. 앱 정보
            SettingsSectionHeader(title = "앱 정보")
            SettingsCardSection {
                SettingsActionItem(
                    title = "앱 버전",
                    onClick = onVersionTap,
                    trailingText = uiState.appVersion
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsActionItem(
                    title = "개인정보 처리방침",
                    onClick = { /* TODO: Navigation */ },
                    showChevron = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsActionItem(
                    title = "오픈소스 라이선스",
                    onClick = { /* TODO: Navigation */ },
                    showChevron = true
                )
            }

            // 6. 디버그 메뉴 (개발자 전용)
            if (uiState.isDebugBuild && uiState.isDebugMenuVisible) {
                SettingsDebugSectionTitle()

                // 분석/시각화 카드
                SettingsCardSection(borderColor = DebugPinkOutline) {
                    SettingsGroupLabel(text = "분석/시각화")
                    SettingsSwitchItem(
                        title = "얼굴 박스 표시",
                        checked = uiState.debugOptions.showFaceBoundingBox,
                        onCheckedChange = { onDebugOptionChanged(DebugOptionType.SHOW_FACE_BOX, it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSwitchItem(
                        title = "스마트 제외 사유 표시",
                        checked = uiState.debugOptions.showRejectReasonOverlay,
                        onCheckedChange = { onDebugOptionChanged(DebugOptionType.SHOW_REJECT_REASON, it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSwitchItem(
                        title = "사진 평가 점수 표시",
                        checked = uiState.debugOptions.showScoreOverlay,
                        onCheckedChange = { onDebugOptionChanged(DebugOptionType.SHOW_SCORE, it) }
                    )
                }

                // 튜닝 카드
                SettingsCardSection(modifier = Modifier.padding(top = 20.dp), borderColor = DebugPinkOutline) {
                    SettingsCardTuningHeader(
                        title = "분석 파라미터 튜닝",
                        actionText = "기본값으로 재설정",
                        onActionClick = onResetThresholds
                    )
                    SettingsSliderItem(
                        title = "흔들림 임계값",
                        value = uiState.smartDiscardThresholds.blurThreshold,
                        onValueChange = { onThresholdChanged(ThresholdType.BLUR, it) },
                        valueRange = 30f..250f,
                        steps = 44,
                        valueFormatter = { "%.1f".format(it) },
                        defaultValue = 100.0f,
                        subtitle = "값을 올리면 더 많이 '흔들림'으로 잡고, 내리면 심하게 흔들린 사진만 잡아요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "얼굴 최소 크기",
                        value = uiState.smartDiscardThresholds.minFaceSize,
                        onValueChange = { onThresholdChanged(ThresholdType.MIN_FACE_SIZE, it) },
                        valueRange = 0.02f..0.20f,
                        steps = 36,
                        valueFormatter = { "%.1f%%".format(it * 100) },
                        defaultValue = 0.05f,
                        subtitle = "사진에서 얼굴이 차지하는 비율(%)이에요. 값을 올리면 얼굴이 작은 사진을 더 많이 '작음'으로 잡고, 내리면 작은 얼굴도 더 통과해요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "고개 각도 제한",
                        value = uiState.smartDiscardThresholds.headAngleLimit,
                        onValueChange = { onThresholdChanged(ThresholdType.HEAD_ANGLE, it) },
                        valueRange = 10f..60f,
                        steps = 50,
                        valueFormatter = { "%.1f°".format(it) },
                        defaultValue = 30.0f,
                        subtitle = "값을 올리면 고개를 더 돌려도 통과하고, 내리면 조금만 돌아도 '고개돌림'으로 잡아요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "눈 뜸 임계값",
                        value = uiState.smartDiscardThresholds.eyeOpenThreshold,
                        onValueChange = { onThresholdChanged(ThresholdType.EYE_OPEN, it) },
                        valueRange = 0.10f..0.90f,
                        steps = 16,
                        valueFormatter = { "%.2f".format(it) },
                        defaultValue = 0.50f,
                        subtitle = "값을 올리면 살짝 감긴 눈도 '눈감음'으로 잡고, 내리면 확실히 감긴 경우만 잡아요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "눈웃음 예외 임계값",
                        value = uiState.smartDiscardThresholds.smileExceptionThreshold,
                        onValueChange = { onThresholdChanged(ThresholdType.SMILE_EXCEPTION, it) },
                        valueRange = 0.00f..1.00f,
                        steps = 20,
                        valueFormatter = { "%.2f".format(it) },
                        defaultValue = 0.70f,
                        subtitle = "값을 올리면 웃고 있어도 '눈감음'으로 잡힐 수 있고, 내리면 웃는 사진은 더 많이 통과해요."
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    PicklyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                cacheSizeBytes = 12 * 1024 * 1024
            ),
            onDuplicatePolicyChanged = {},
            onThemeChanged = {},
            onSmartDiscardCriterionToggle = {},
            onClearCache = {}
        )
    }
}
