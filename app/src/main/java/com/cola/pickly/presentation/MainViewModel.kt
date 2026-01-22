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

enum class PermissionState {
    Granted,
    Denied,
    NotDetermined
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

    private fun checkPermission() {
        val permissions = getRequiredPermissions()
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

    fun onPermissionResult(grantedPermissions: Map<String, Boolean>) {
        val allGranted = grantedPermissions.values.all { it }

        if (allGranted) {
            _uiState.value = MainUiState.Ready
        } else {
            _uiState.update {
                (it as? MainUiState.Initializing)?.copy(permissionState = PermissionState.Denied) ?: it
            }
        }
    }

    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun getRequiredPermissions(): List<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
