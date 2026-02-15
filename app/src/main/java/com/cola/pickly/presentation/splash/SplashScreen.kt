package com.cola.pickly.presentation.splash

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextSecondary
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

        if (!initializingState.isChecking && !initializingState.showWelcome && !initializingState.showPrePermission) {
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
            is MainUiState.Initializing -> when {
                state.showWelcome -> WelcomeSplashContent(
                    onContinue = { viewModel.onWelcomeDismissed() }
                )
                state.showPrePermission -> PrePermissionContent(
                    onConfirm = {
                        viewModel.onPrePermissionConfirmed()
                        requestPermissionsLauncher.launch(
                            viewModel.getRequiredPermissions().toTypedArray()
                        )
                    }
                )
                else -> when (state.permissionState) {
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
                    else -> Unit
                }
            }
            is MainUiState.Ready -> Unit
        }
    }
}

@Composable
private fun WelcomeSplashContent(onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TealAccent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pickly",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "사진을 더 스마트하게 정리하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = TealAccent
                )
            ) {
                Text(
                    text = "시작하기",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PrePermissionContent(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "사진 접근 권한 안내",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pickly가 사진을 정리하려면 갤러리 접근이 필요합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PrePermissionItem(
                    title = "전체 허용",
                    description = "모든 사진을 자유롭게 분석하고 정리할 수 있습니다."
                )
                PrePermissionItem(
                    title = "부분 허용 (Android 14+)",
                    description = "선택한 사진만 접근 가능합니다. 분석 범위가 제한됩니다."
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealAccent,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "확인, 권한 요청하기",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PrePermissionItem(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(top = 6.dp)
                .background(TealAccent, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
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
