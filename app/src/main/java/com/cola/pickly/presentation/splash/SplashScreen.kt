package com.cola.pickly.presentation.splash

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextSecondary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val requestPermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantedMap ->
            viewModel.onPermissionResult(
                grantedMap = grantedMap,
                shouldShowRationaleChecker = { permission ->
                    (context as? androidx.activity.ComponentActivity)
                        ?.shouldShowRequestPermissionRationale(permission) ?: false
                }
            )
        }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // PermanentlyDenied 상태에서 설정 복귀 시 권한 재확인
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recheckPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ready 상태 시 메인 화면으로 이동
    LaunchedEffect(uiState) {
        if (uiState is MainUiState.Ready) {
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    if (uiState is MainUiState.Initializing) {
        val initializingState = uiState as MainUiState.Initializing

        if (!initializingState.isChecking) {
            when (initializingState.permissionState) {
                PermissionState.NotDetermined -> {
                    val permissions = viewModel.getRequiredPermissions()
                    LaunchedEffect(Unit) {
                        requestPermissionsLauncher.launch(permissions.toTypedArray())
                    }
                }
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            is MainUiState.Initializing -> when (state.permissionState) {
                PermissionState.Denied -> PermissionDeniedContent(
                    onRetry = {
                        requestPermissionsLauncher.launch(
                            viewModel.getRequiredPermissions().toTypedArray()
                        )
                    }
                )
                PermissionState.PermanentlyDenied -> PermissionPermanentlyDeniedContent(
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
                PermissionState.PartiallyGranted -> PermissionPartiallyGrantedContent(
                    onContinue = { viewModel.proceedWithPartialAccess() },
                    onRequestAll = {
                        requestPermissionsLauncher.launch(
                            viewModel.getRequiredPermissions().toTypedArray()
                        )
                    }
                )
                else -> Text("Pickly")
            }
            is MainUiState.Ready -> Unit
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "사진 접근 권한이 필요합니다.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("다시 요청")
        }
    }
}

@Composable
private fun PermissionPermanentlyDeniedContent(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "권한이 영구적으로 거부되었습니다.\n설정에서 허용해주세요.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text("설정으로 이동")
        }
    }
}

@Composable
private fun PermissionPartiallyGrantedContent(
    onContinue: () -> Unit,
    onRequestAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "일부 사진만 접근 가능합니다.",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "전체 허용 또는 현재 상태로 계속할 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(
                containerColor = TealAccent,
                contentColor = Color.White
            )
        ) {
            Text("계속하기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRequestAll,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = TealAccent
            )
        ) {
            Text("전체 허용")
        }
    }
}
