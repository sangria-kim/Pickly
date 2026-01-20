package com.cola.pickly.feature.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.ThemeMode
import com.cola.pickly.core.ui.theme.BackgroundWhite
import com.cola.pickly.core.ui.theme.TextPrimary
import com.cola.pickly.feature.settings.components.SettingsActionItem
import com.cola.pickly.feature.settings.components.SettingsDivider
import com.cola.pickly.feature.settings.components.SettingsGroupLabel
import com.cola.pickly.feature.settings.components.SettingsRadioItem
import com.cola.pickly.feature.settings.components.SettingsSectionHeader
import com.cola.pickly.feature.settings.components.SettingsSwitchItem
import com.cola.pickly.feature.settings.components.SettingsTextItem
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * S-08 설정 화면 (UI-only 단계)
 *
 * - 링크/캐시 삭제 등은 스낵바로 “준비 중입니다.” 처리
 * - 라디오/토글은 SettingsViewModel을 통해 즉시 상태 반영
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoonMessage = stringResource(R.string.settings_snackbar_coming_soon)
    val cacheClearedMessage = stringResource(R.string.settings_cache_clear_done)
    val cacheClearFailedMessage = stringResource(R.string.settings_cache_clear_failed)

    fun showComingSoon() {
        scope.launch { snackbarHostState.showSnackbar(comingSoonMessage) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.CacheCleared -> snackbarHostState.showSnackbar(cacheClearedMessage)
                SettingsEvent.CacheClearFailed -> snackbarHostState.showSnackbar(cacheClearFailedMessage)
            }
        }
    }

    val appVersionText = rememberAppVersionText()
    val cacheSizeText = when {
        uiState.isCacheSizeLoading -> stringResource(R.string.settings_cache_size_loading)
        uiState.cacheSizeBytes == null -> stringResource(R.string.settings_cache_size_placeholder)
        else -> formatBytes(uiState.cacheSizeBytes)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { innerPadding ->
        SettingsScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            uiState = uiState,
            appVersionText = appVersionText,
            cacheSizeText = cacheSizeText,
            onComingSoon = ::showComingSoon,
            onClearCache = viewModel::clearCache,
            onSetDuplicateFilenamePolicy = viewModel::setDuplicateFilenamePolicy,
            onSetRecommendationEnabled = viewModel::setRecommendationEnabled,
            onSetThemeMode = viewModel::setThemeMode
        )
    }
}

@Composable
private fun rememberAppVersionText(): String {
    val context = LocalContext.current
    return remember(context) {
        val packageName = context.packageName
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }

        val versionName = info.versionName ?: "—"
        val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

        "$versionName ($versionCode)"
    }
}

@Composable
internal fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState,
    appVersionText: String,
    cacheSizeText: String,
    onComingSoon: () -> Unit,
    onClearCache: () -> Unit,
    onSetDuplicateFilenamePolicy: (DuplicateFilenamePolicy) -> Unit,
    onSetRecommendationEnabled: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit
) {
    LazyColumn(modifier = modifier) {
        // A. 사진 정리
        item { SettingsSectionHeader(stringResource(R.string.settings_section_organize)) }

        item { SettingsGroupLabel(stringResource(R.string.settings_group_duplicate_filename)) }
        item {
            SettingsRadioItem(
                title = stringResource(R.string.settings_option_duplicate_overwrite),
                selected = uiState.duplicateFilenamePolicy == DuplicateFilenamePolicy.Overwrite,
                onClick = { onSetDuplicateFilenamePolicy(DuplicateFilenamePolicy.Overwrite) }
            )
        }
        item { SettingsDivider() }
        item {
            SettingsRadioItem(
                title = stringResource(R.string.settings_option_duplicate_skip),
                selected = uiState.duplicateFilenamePolicy == DuplicateFilenamePolicy.Skip,
                onClick = { onSetDuplicateFilenamePolicy(DuplicateFilenamePolicy.Skip) }
            )
        }

        // B. 사진 추천
        item { SettingsSectionHeader(stringResource(R.string.settings_section_recommendation)) }
        item {
            SettingsSwitchItem(
                title = stringResource(R.string.settings_option_recommendation_enabled),
                checked = uiState.isRecommendationEnabled,
                onCheckedChange = onSetRecommendationEnabled,
                subtitle = stringResource(R.string.settings_option_recommendation_subtitle)
            )
        }

        // C. 화면 표시
        item { SettingsSectionHeader(stringResource(R.string.settings_section_display)) }
        item { SettingsGroupLabel(stringResource(R.string.settings_group_theme)) }
        item {
            SettingsRadioItem(
                title = stringResource(R.string.settings_option_theme_system),
                selected = uiState.themeMode == ThemeMode.System,
                onClick = { onSetThemeMode(ThemeMode.System) }
            )
        }
        item { SettingsDivider() }
        item {
            SettingsRadioItem(
                title = stringResource(R.string.settings_option_theme_light),
                selected = uiState.themeMode == ThemeMode.Light,
                onClick = { onSetThemeMode(ThemeMode.Light) }
            )
        }
        item { SettingsDivider() }
        item {
            SettingsRadioItem(
                title = stringResource(R.string.settings_option_theme_dark),
                selected = uiState.themeMode == ThemeMode.Dark,
                onClick = { onSetThemeMode(ThemeMode.Dark) }
            )
        }

        // D. 데이터 관리
        item { SettingsSectionHeader(stringResource(R.string.settings_section_data)) }
        item {
            SettingsTextItem(
                title = stringResource(R.string.settings_cache_size),
                value = cacheSizeText
            )
        }
        item { SettingsDivider() }
        item {
            SettingsActionItem(
                title = stringResource(R.string.settings_cache_clear),
                onClick = onClearCache,
                trailingText = if (uiState.isClearingCache) stringResource(R.string.settings_cache_clear_in_progress) else null,
                enabled = !uiState.isClearingCache
            )
        }

        // E. 앱 정보
        item { SettingsSectionHeader(stringResource(R.string.settings_section_app_info)) }
        item {
            SettingsTextItem(
                title = stringResource(R.string.settings_app_version),
                value = appVersionText
            )
        }
        item { SettingsDivider() }
        item {
            SettingsActionItem(
                title = stringResource(R.string.settings_privacy_policy),
                onClick = onComingSoon
            )
        }
        item { SettingsDivider() }
        item {
            SettingsActionItem(
                title = stringResource(R.string.settings_open_source_licenses),
                onClick = onComingSoon
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0B"
    val unit = 1024.0
    val kb = bytes / unit
    val mb = kb / unit
    val gb = mb / unit
    return when {
        gb >= 1 -> String.format("%.1fGB", gb)
        mb >= 1 -> String.format("%.1fMB", mb)
        kb >= 1 -> String.format("%.1fKB", kb)
        else -> "${bytes}B"
    }
}


