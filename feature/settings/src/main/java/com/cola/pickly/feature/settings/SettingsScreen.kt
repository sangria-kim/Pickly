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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.ThemeMode
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.ui.theme.PicklyTheme
import com.cola.pickly.feature.settings.components.SettingsActionItem
import com.cola.pickly.feature.settings.components.SettingsExpandableChecklist
import com.cola.pickly.feature.settings.components.SettingsGroupLabel
import com.cola.pickly.feature.settings.components.SettingsRadioItem
import com.cola.pickly.feature.settings.components.SettingsSectionHeader
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
        onSmartDiscardReasonEnabledChanged = viewModel::setSmartDiscardReasonEnabled,
        onSmartDiscardCriterionToggle = viewModel::toggleSmartDiscardCriterion,
        onClearCache = viewModel::clearCache
    )
}

@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onDuplicatePolicyChanged: (DuplicateFilenamePolicy) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onSmartDiscardReasonEnabledChanged: (Boolean) -> Unit,
    onSmartDiscardCriterionToggle: (RejectReason) -> Unit,
    onClearCache: () -> Unit
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
            // 1. 사진 정리 (TODO: Real functionality)
            SettingsSectionHeader(title = "사진 정리")
            SettingsGroupLabel(text = "동일 파일명 처리")
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

            // 2. 스마트 제외 (Implemented)
            SettingsSectionHeader(title = "스마트 제외")
            SettingsSwitchItem(
                title = "후보 사유 표시",
                subtitle = "후보 배지에 사유(예: 얼굴잘림) 표시",
                checked = uiState.isSmartDiscardReasonEnabled,
                onCheckedChange = onSmartDiscardReasonEnabledChanged
            )
            
            val criteriaList = (RejectReason.entries - RejectReason.NO_FACE).map { reason ->
                reason to (reason in uiState.smartDiscardCriteria)
            }
            
            SettingsExpandableChecklist(
                title = "후보 기준",
                subtitle = "${uiState.smartDiscardCriteria.count { it != RejectReason.NO_FACE }}개 선택됨",
                items = criteriaList.map { (reason, checked) -> reason.label to checked },
                onItemCheckedChange = { index, _ -> 
                    onSmartDiscardCriterionToggle(criteriaList[index].first)
                }
            )

            // 3. 화면 표시 (TODO: Real functionality)
            SettingsSectionHeader(title = "화면 표시")
            SettingsGroupLabel(text = "테마")
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

            // 4. 데이터 관리 (TODO: Real functionality)
            SettingsSectionHeader(title = "데이터 관리")
            SettingsTextItem(
                title = "캐시 용량",
                value = uiState.cacheSizeBytes?.let { formatFileSize(it) } ?: "계산 중..."
            )
            SettingsActionItem(
                title = "캐시 삭제",
                onClick = onClearCache,
                subtitle = "사진 원본에는 영향을 주지 않습니다.",
                enabled = !uiState.isClearingCache,
                trailingText = if (uiState.isClearingCache) "삭제 중..." else null
            )

            // 5. 앱 정보 (UI Only)
            SettingsSectionHeader(title = "앱 정보")
            SettingsTextItem(
                title = "앱 버전",
                value = "1.0.0" // TODO: Get from BuildConfig
            )
            SettingsActionItem(
                title = "개인정보 처리방침",
                onClick = { /* TODO: Navigation */ }
            )
            SettingsActionItem(
                title = "오픈소스 라이선스",
                onClick = { /* TODO: Navigation */ }
            )
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
            onSmartDiscardReasonEnabledChanged = {},
            onSmartDiscardCriterionToggle = {},
            onClearCache = {}
        )
    }
}
