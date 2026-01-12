package com.cola.pickly.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.ThemeMode
import com.cola.pickly.core.ui.theme.PicklyTheme

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
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Skip,
                isRecommendationEnabled = true,
                themeMode = ThemeMode.Dark
            ),
            appVersionText = "2.3.4 (123)",
            cacheSizeText = "128MB",
            onComingSoon = {},
            onClearCache = {},
            onSetDuplicateFilenamePolicy = {},
            onSetRecommendationEnabled = {},
            onSetThemeMode = {}
        )
    }
}


