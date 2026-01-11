package com.cola.pickly.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cola.pickly.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.data.settings.ResultSaveLocationPolicy
import com.cola.pickly.data.settings.ThemeMode
import com.cola.pickly.ui.theme.PicklyTheme

@Preview(name = "SettingsScreenContent - Default", showBackground = true)
@Composable
private fun SettingsScreenContentPreview_Default() {
    PicklyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            appVersionText = "1.0.0 (1)",
            cacheSizeText = "—",
            onComingSoon = {},
            onClearCache = {},
            onSetResultSaveLocationPolicy = {},
            onSetDuplicateFilenamePolicy = {},
            onSetRecommendationEnabled = {},
            onSetThemeMode = {}
        )
    }
}

@Preview(name = "SettingsScreenContent - Customized", showBackground = true)
@Composable
private fun SettingsScreenContentPreview_Customized() {
    PicklyTheme(darkTheme = true) {
        SettingsScreenContent(
            uiState = SettingsUiState(
                resultSaveLocationPolicy = ResultSaveLocationPolicy.AlwaysCreateNewFolder,
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Skip,
                isRecommendationEnabled = true,
                themeMode = ThemeMode.Dark
            ),
            appVersionText = "2.3.4 (123)",
            cacheSizeText = "128MB",
            onComingSoon = {},
            onClearCache = {},
            onSetResultSaveLocationPolicy = {},
            onSetDuplicateFilenamePolicy = {},
            onSetRecommendationEnabled = {},
            onSetThemeMode = {}
        )
    }
}


