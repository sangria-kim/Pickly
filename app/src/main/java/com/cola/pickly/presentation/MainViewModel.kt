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
        val requiredPermissions = getRequiredPermissions()
        val isGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (isGranted) {
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

    fun onPermissionResult(result: Map<String, Boolean>) {
        val isGranted = getRequiredPermissions().all { permission ->
            result[permission] == true
        }

        if (isGranted) {
            _uiState.value = MainUiState.Ready
        } else {
            _uiState.update {
                (it as? MainUiState.Initializing)?.copy(permissionState = PermissionState.Denied) ?: it
            }
        }
    }

    fun getRequiredPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(Manifest.permission.READ_MEDIA_IMAGES)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }
}
