package com.cola.pickly.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.ThemeMode
import com.cola.pickly.core.ui.theme.PicklyTheme
import com.cola.pickly.core.model.RejectReason

@Preview(name = "SettingsScreenContent - Default", showBackground = true)
@Composable
private fun SettingsScreenContentPreview_Default() {
    PicklyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            onDuplicatePolicyChanged = {},
            onThemeChanged = {},
            onSmartDiscardCriterionToggle = {},
            onClearCache = {}
        )
    }
}

@Preview(name = "SettingsScreenContent - Customized", showBackground = true)
@Composable
private fun SettingsScreenContentPreview_Customized() {
    PicklyTheme(darkTheme = true) {
        SettingsScreenContent(
            uiState = SettingsUiState(
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Skip,
                themeMode = ThemeMode.Dark,
                smartDiscardCriteria = setOf(RejectReason.BLURRY, RejectReason.EYES_CLOSED)
            ),
            onDuplicatePolicyChanged = {},
            onThemeChanged = {},
            onSmartDiscardCriterionToggle = {},
            onClearCache = {}
        )
    }
}

@Preview(name = "Debug Visible", showBackground = true)
@Composable
private fun SettingsScreenContentPreview_DebugVisible() {
    PicklyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(isDebugBuild = true, isDebugMenuVisible = true, cacheSizeBytes = 134 * 1024 * 1024),
            onDuplicatePolicyChanged = {},
            onThemeChanged = {},
            onSmartDiscardCriterionToggle = {},
            onClearCache = {}
        )
    }
}
