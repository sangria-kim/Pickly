package com.cola.pickly.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MainUiState {
    data class Initializing(
        val isChecking: Boolean = true,
        val permissionState: PermissionState = PermissionState.NotDetermined
    ) : MainUiState()
    object Ready : MainUiState()
}

sealed class PermissionState {
    object Granted : PermissionState()
    object PartiallyGranted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
    object NotDetermined : PermissionState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Initializing())
    val uiState: StateFlow<MainUiState> = _uiState

    fun init() {
        viewModelScope.launch {
            checkPermission()
        }
    }

    fun recheckPermission() {
        viewModelScope.launch {
            checkPermission()
        }
    }

    private fun checkPermission() {
        val permissions = getRequiredPermissions()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val imagesGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val partialGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED

            when {
                imagesGranted -> _uiState.value = MainUiState.Ready
                partialGranted -> _uiState.update {
                    (it as? MainUiState.Initializing)?.copy(
                        isChecking = false,
                        permissionState = PermissionState.PartiallyGranted
                    ) ?: it
                }
                else -> _uiState.update {
                    (it as? MainUiState.Initializing)?.copy(
                        isChecking = false,
                        permissionState = PermissionState.NotDetermined
                    ) ?: it
                }
            }
        } else {
            val allGranted = permissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }

            if (allGranted) {
                _uiState.value = MainUiState.Ready
            } else {
                _uiState.update {
                    (it as? MainUiState.Initializing)?.copy(
                        isChecking = false,
                        permissionState = PermissionState.NotDetermined
                    ) ?: it
                }
            }
        }
    }

    fun onPermissionResult(
        grantedMap: Map<String, Boolean>,
        shouldShowRationaleChecker: (String) -> Boolean
    ) {
        val state = when {
            // Android 14+: READ_MEDIA_IMAGES 전체 허용
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                grantedMap[Manifest.permission.READ_MEDIA_IMAGES] == true ->
                PermissionState.Granted

            // Android 14+: READ_MEDIA_VISUAL_USER_SELECTED만 허용 (부분 허용)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                grantedMap[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true ->
                PermissionState.PartiallyGranted

            // 전체 허용 (공통)
            grantedMap.values.all { it } ->
                PermissionState.Granted

            // 영구 거부: 거부된 권한 중 rationale 표시 불가한 항목 존재
            grantedMap.any { (permission, granted) ->
                !granted && !shouldShowRationaleChecker(permission)
            } -> PermissionState.PermanentlyDenied

            // 일반 거부
            else -> PermissionState.Denied
        }

        if (state == PermissionState.Granted) {
            _uiState.value = MainUiState.Ready
        } else {
            _uiState.update {
                (it as? MainUiState.Initializing)?.copy(permissionState = state) ?: it
            }
        }
    }

    fun proceedWithPartialAccess() {
        _uiState.value = MainUiState.Ready
    }

    fun getRequiredPermissions(): List<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}
