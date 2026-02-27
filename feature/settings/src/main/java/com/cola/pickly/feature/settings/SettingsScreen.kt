package com.cola.pickly.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.SmartDiscardResultMode
import com.cola.pickly.core.data.settings.ThemeMode
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.model.SensitivityLevel
import com.cola.pickly.core.ui.theme.DebugPinkBackground
import com.cola.pickly.core.ui.theme.DebugPinkOutline
import com.cola.pickly.core.ui.theme.PicklyTheme
import com.cola.pickly.core.ui.components.PicklySnackbarHost
import com.cola.pickly.feature.settings.components.AutoRejectWarningDialog
import com.cola.pickly.feature.settings.components.SettingsActionItem
import com.cola.pickly.feature.settings.components.SettingsCardHeader
import com.cola.pickly.feature.settings.components.SettingsCardSection
import com.cola.pickly.feature.settings.components.SettingsCardTuningHeader
import com.cola.pickly.feature.settings.components.SettingsRadioItem
import com.cola.pickly.feature.settings.components.SettingsSectionHeader
import com.cola.pickly.feature.settings.components.SettingsSliderItem
import com.cola.pickly.feature.settings.components.SmartDiscardCriterionItem
import com.cola.pickly.feature.settings.components.SettingsSwitchItem
import com.cola.pickly.feature.settings.components.SettingsTextItem

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToOpenSourceLicenses: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAutoRejectWarningDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar 메시지 수집
    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.ShowAutoRejectWarningDialog -> {
                    showAutoRejectWarningDialog = true
                }
            }
        }
    }

    // 다이얼로그 표시
    if (showAutoRejectWarningDialog) {
        AutoRejectWarningDialog(
            onConfirm = {
                viewModel.confirmAutoRejectMode()
                showAutoRejectWarningDialog = false
            },
            onDismiss = {
                viewModel.cancelAutoRejectMode()
                showAutoRejectWarningDialog = false
            }
        )
    }

    SettingsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDuplicatePolicyChanged = viewModel::setDuplicateFilenamePolicy,
        onThemeChanged = viewModel::setThemeMode,
        onSmartDiscardCriterionToggle = viewModel::toggleSmartDiscardCriterion,
        onSmartDiscardResultModeChanged = viewModel::setSmartDiscardResultMode,
        onSensitivityChanged = viewModel::setSensitivity,
        onResetSensitivities = viewModel::resetSensitivities,
        onClearCache = viewModel::clearCache,
        onVersionTap = viewModel::onVersionTap,
        onDebugOptionChanged = viewModel::setDebugOption,
        onThresholdChanged = viewModel::setThreshold,
        onResetThresholds = viewModel::resetSmartDiscardThresholds,
        onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
        onNavigateToOpenSourceLicenses = onNavigateToOpenSourceLicenses
    )
}

@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onDuplicatePolicyChanged: (DuplicateFilenamePolicy) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onSmartDiscardCriterionToggle: (RejectReason) -> Unit,
    onSmartDiscardResultModeChanged: (com.cola.pickly.core.data.settings.SmartDiscardResultMode) -> Unit = {},
    onSensitivityChanged: (RejectReason, SensitivityLevel) -> Unit = { _, _ -> },
    onResetSensitivities: () -> Unit = {},
    onClearCache: () -> Unit,
    onVersionTap: () -> Unit = {},
    onDebugOptionChanged: (DebugOptionType, Boolean) -> Unit = { _, _ -> },
    onThresholdChanged: (ThresholdType, Float) -> Unit = { _, _ -> },
    onResetThresholds: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToOpenSourceLicenses: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PicklySnackbarHost(hostState = snackbarHostState) }
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

            val orderedCriteria = listOf(
                RejectReason.BLURRY,
                RejectReason.EYES_CLOSED,
                RejectReason.HEAD_TURNED,
                RejectReason.TOO_SMALL,
                RejectReason.OCCLUDED,
                RejectReason.CROPPED,
            )
            val sensitivityLabels = mapOf(
                RejectReason.BLURRY to "흔들린 사진을 잡는 정도",
                RejectReason.EYES_CLOSED to "눈 감은 사진을 잡는 정도",
                RejectReason.HEAD_TURNED to "고개 돌린 사진을 잡는 정도",
                RejectReason.TOO_SMALL to "얼굴이 작은 사진을 잡는 정도",
            )
            var isCriteriaExpanded by rememberSaveable { mutableStateOf(false) }
            val selectedCriteriaCount = orderedCriteria.count { it in uiState.smartDiscardCriteria }

            // 카드 1: 후보 기준 + 민감도 초기화
            SettingsCardSection {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCriteriaExpanded = !isCriteriaExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            text = "후보 기준",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Text(
                                text = "${selectedCriteriaCount}개 선택됨",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (isCriteriaExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    androidx.compose.material3.Text(
                        text = "이 기준에 걸린 사진은 제외해요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (isCriteriaExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    orderedCriteria.forEachIndexed { index, reason ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        val sensitivityLevel: SensitivityLevel? = if (reason.hasSensitivity) {
                            when (reason) {
                                RejectReason.BLURRY -> uiState.smartDiscardSensitivities.blur
                                RejectReason.EYES_CLOSED -> uiState.smartDiscardSensitivities.eyeOpen
                                RejectReason.HEAD_TURNED -> uiState.smartDiscardSensitivities.headAngle
                                RejectReason.TOO_SMALL -> uiState.smartDiscardSensitivities.minFaceSize
                                else -> null
                            }
                        } else null

                        SmartDiscardCriterionItem(
                            label = reason.label,
                            isChecked = reason in uiState.smartDiscardCriteria,
                            onCheckedChange = { onSmartDiscardCriterionToggle(reason) },
                            sensitivityLevel = sensitivityLevel,
                            onSensitivityChange = if (reason.hasSensitivity) {
                                { level -> onSensitivityChanged(reason, level) }
                            } else null,
                            sensitivityDescription = sensitivityLabels[reason]
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.Text(
                            text = "민감도 초기화",
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.cola.pickly.core.ui.theme.TealAccent,
                            modifier = Modifier.clickable(onClick = onResetSensitivities)
                        )
                    }
                }
            }

            // 카드 2: 분석 결과 처리 방식 (독립 카드)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCardSection {
                SettingsCardHeader(
                    title = "분석 결과 처리 방식",
                    description = "분석된 사진을 어떻게 처리할지 선택하세요."
                )
                SettingsRadioItem(
                    title = "아쉬움 후보로 표시만 하기",
                    subtitle = "분석 결과를 뱃지로 표시하고, 직접 선택할 수 있어요.",
                    selected = uiState.smartDiscardResultMode == SmartDiscardResultMode.ShowAsCandidates,
                    onClick = { onSmartDiscardResultModeChanged(SmartDiscardResultMode.ShowAsCandidates) }
                )
                SettingsRadioItem(
                    title = "자동으로 제외하기",
                    subtitle = "분석 결과에 따라 자동으로 제외 상태로 변경돼요.",
                    selected = uiState.smartDiscardResultMode == SmartDiscardResultMode.AutoReject,
                    onClick = { onSmartDiscardResultModeChanged(SmartDiscardResultMode.AutoReject) }
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
                    onClick = onNavigateToPrivacyPolicy,
                    showChevron = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingsActionItem(
                    title = "오픈소스 라이선스",
                    onClick = onNavigateToOpenSourceLicenses,
                    showChevron = true
                )
            }

            // 6. 디버그 메뉴 (개발자 전용)
            if (uiState.isDebugBuild && uiState.isDebugMenuVisible) {
                // 섹션 1: 분석/시각화
                SettingsSectionHeader(title = "분석/시각화")
                SettingsCardSection(borderColor = DebugPinkOutline) {
                    SettingsSwitchItem(
                        title = "디버그 오버레이 표시",
                        subtitle = "ViewerScreen에서 분석 결과를 시각화합니다.",
                        checked = uiState.debugOptions.showDebugOverlay,
                        onCheckedChange = { onDebugOptionChanged(DebugOptionType.SHOW_DEBUG_OVERLAY, it) }
                    )

                    // 조건부 렌더링: ON일 때만 하위 옵션 표시
                    if (uiState.debugOptions.showDebugOverlay) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        SettingsSwitchItem(
                            title = "  얼굴 박스 표시",
                            checked = uiState.debugOptions.showFaceBoundingBox,
                            onCheckedChange = { onDebugOptionChanged(DebugOptionType.SHOW_FACE_BOX, it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        SettingsSwitchItem(
                            title = "  사진 평가 점수 표시",
                            checked = uiState.debugOptions.showScoreOverlay,
                            onCheckedChange = { onDebugOptionChanged(DebugOptionType.SHOW_SCORE, it) }
                        )
                    }
                }

                // 섹션 2: 분석 파라미터 튜닝
                SettingsSectionHeader(title = "분석 파라미터 튜닝")
                SettingsCardSection(borderColor = DebugPinkOutline) {
                    SettingsCardTuningHeader(
                        actionText = "기본값으로 재설정",
                        onActionClick = onResetThresholds
                    )
                    val blurSpec = ThresholdSpecs.forType(ThresholdType.BLUR)
                    val minFaceSpec = ThresholdSpecs.forType(ThresholdType.MIN_FACE_SIZE)
                    val headAngleSpec = ThresholdSpecs.forType(ThresholdType.HEAD_ANGLE)
                    val eyeOpenSpec = ThresholdSpecs.forType(ThresholdType.EYE_OPEN)
                    val smileExceptionSpec = ThresholdSpecs.forType(ThresholdType.SMILE_EXCEPTION)

                    SettingsSliderItem(
                        title = "흔들림 임계값",
                        value = uiState.smartDiscardThresholds.blurThreshold,
                        onValueChange = { onThresholdChanged(ThresholdType.BLUR, it) },
                        valueRange = blurSpec.range,
                        steps = blurSpec.steps,
                        valueFormatter = { "%.1f".format(it) },
                        defaultValue = 55.0f,
                        subtitle = "값을 올리면 더 많이 '흔들림'으로 잡고, 내리면 심하게 흔들린 사진만 잡아요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "얼굴 최소 크기",
                        value = uiState.smartDiscardThresholds.minFaceSize,
                        onValueChange = { onThresholdChanged(ThresholdType.MIN_FACE_SIZE, it) },
                        valueRange = minFaceSpec.range,
                        steps = minFaceSpec.steps,
                        valueFormatter = { "%.1f%%".format(it * 100) },
                        defaultValue = 0.05f,
                        subtitle = "사진에서 얼굴이 차지하는 비율(%)이에요. 값을 올리면 얼굴이 작은 사진을 더 많이 '작음'으로 잡고, 내리면 작은 얼굴도 더 통과해요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "고개 각도 제한",
                        value = uiState.smartDiscardThresholds.headAngleLimit,
                        onValueChange = { onThresholdChanged(ThresholdType.HEAD_ANGLE, it) },
                        valueRange = headAngleSpec.range,
                        steps = headAngleSpec.steps,
                        valueFormatter = { "%.1f°".format(it) },
                        defaultValue = 30.0f,
                        subtitle = "값을 올리면 고개를 더 돌려도 통과하고, 내리면 조금만 돌아도 '고개돌림'으로 잡아요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "눈 뜸 임계값",
                        value = uiState.smartDiscardThresholds.eyeOpenThreshold,
                        onValueChange = { onThresholdChanged(ThresholdType.EYE_OPEN, it) },
                        valueRange = eyeOpenSpec.range,
                        steps = eyeOpenSpec.steps,
                        valueFormatter = { "%.2f".format(it) },
                        defaultValue = 0.50f,
                        subtitle = "값을 올리면 살짝 감긴 눈도 '눈감음'으로 잡고, 내리면 확실히 감긴 경우만 잡아요."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsSliderItem(
                        title = "눈웃음 예외 임계값",
                        value = uiState.smartDiscardThresholds.smileExceptionThreshold,
                        onValueChange = { onThresholdChanged(ThresholdType.SMILE_EXCEPTION, it) },
                        valueRange = smileExceptionSpec.range,
                        steps = smileExceptionSpec.steps,
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
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    PicklyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                cacheSizeBytes = 12 * 1024 * 1024
            ),
            snackbarHostState = SnackbarHostState(),
            onDuplicatePolicyChanged = {},
            onThemeChanged = {},
            onSmartDiscardCriterionToggle = {},
            onClearCache = {}
        )
    }
}
