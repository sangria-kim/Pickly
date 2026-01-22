package com.cola.pickly.presentation.splash

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cola.pickly.presentation.MainUiState
import com.cola.pickly.presentation.MainViewModel
import com.cola.pickly.presentation.PermissionState

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val requestPermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantedMap ->
            viewModel.onPermissionResult(grantedMap)
        }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 상태에 따른 네비게이션 처리
    LaunchedEffect(uiState) {
        if (uiState is MainUiState.Ready) {
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    if (uiState is MainUiState.Initializing) {
        val initializingState = uiState as MainUiState.Initializing
        
        // 권한 체크가 끝날 때까지 대기
        if (!initializingState.isChecking) {
            when (initializingState.permissionState) {
                PermissionState.NotDetermined -> {
                    val permissions = viewModel.getRequiredPermissions()
                    LaunchedEffect(Unit) {
                        requestPermissionsLauncher.launch(permissions.toTypedArray())
                    }
                }
                PermissionState.Granted -> {
                    // ViewModel에서 이미 Ready 상태로 전환되었을 수 있으므로 여기서도 처리
                    // 하지만 onPermissionResult에서 Ready로 바꾸면 리컴포지션이 일어나서 위의 LaunchedEffect(uiState)가 처리할 것임.
                    // 초기 진입 시 이미 권한이 있는 경우를 위해 뷰모델 init에서 바로 Ready로 가는 경우도 고려해야 함.
                }
                PermissionState.Denied -> {
                    // TODO: 권한 거부 시 UI 처리 (예: 설정으로 유도하는 안내 문구 표시)
                    // 여기는 Box 내부 UI로 처리됨
                }
            }
        }
    }

    // 스플래시 기본 UI (로딩 중이거나 권한 체크 중일 때 표시됨)
    // 권한 거부 상태일 때만 다른 UI 표시
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (uiState is MainUiState.Initializing && 
            (uiState as MainUiState.Initializing).permissionState == PermissionState.Denied) {
            Text("권한이 거부되었습니다. 앱 설정에서 권한을 허용해주세요.")
        } else {
            Text("Pickly")
        }
    }
}
