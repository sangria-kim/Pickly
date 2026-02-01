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
            onSmartDiscardReasonEnabledChanged = {},
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
                isSmartDiscardReasonEnabled = true,
                smartDiscardCriteria = setOf(RejectReason.BLURRY, RejectReason.EYES_CLOSED)
            ),
            onDuplicatePolicyChanged = {},
            onThemeChanged = {},
            onSmartDiscardReasonEnabledChanged = {},
            onSmartDiscardCriterionToggle = {},
            onClearCache = {}
        )
    }
}
