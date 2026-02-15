package com.cola.pickly

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.presentation.MainUiState
import com.cola.pickly.presentation.MainViewModel
import com.cola.pickly.presentation.ui.PicklyNavGraph
import com.cola.pickly.core.data.settings.Settings
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.data.settings.ThemeMode
import com.cola.pickly.core.ui.theme.PicklyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val splashStartTime = SystemClock.elapsedRealtime()

    companion object {
        private const val SPLASH_MAX_MS = 1500L
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupUI(splashScreen)
    }

    private fun setupUI(splashScreen: androidx.core.splashscreen.SplashScreen) {
        mainViewModel.init()
        splashScreen.setKeepOnScreenCondition {
            val state = mainViewModel.uiState.value
            val elapsed = SystemClock.elapsedRealtime() - splashStartTime
            state is MainUiState.Initializing && state.isChecking && elapsed < SPLASH_MAX_MS
        }
        setContent {
            val settings = settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = Settings()
            ).value
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            PicklyTheme(darkTheme = darkTheme) {
                PicklyNavGraph(mainViewModel = mainViewModel)
            }
        }
    }
}