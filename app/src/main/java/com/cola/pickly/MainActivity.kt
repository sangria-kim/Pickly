package com.cola.pickly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
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

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // 시스템 스플래시 설치 (super.onCreate 이전에 호출해야 함)
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 상태바 숨기기
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }

        // 초기화 로직 시작
        mainViewModel.init()

        // 스플래시 화면 유지 조건 설정
        splashScreen.setKeepOnScreenCondition {
            // 권한 체크 중일 때만 시스템 스플래시 유지
            val state = mainViewModel.uiState.value
            if (state is MainUiState.Initializing) {
                state.isChecking
            } else {
                false
            }
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