package com.cola.pickly.core.ui.util

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

import androidx.compose.ui.graphics.toArgb


import androidx.compose.ui.graphics.luminance

@Composable
fun ViewerSystemBarsPolicy() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    
    // Check actual theme darkness using background luminance
    // This handles cases where App Theme overrides System Theme
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkTheme = backgroundColor.luminance() < 0.5f

    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(window, view)
        
        // Viewer Policy:
        // 1. Status Bar: Visible + Transparent (Theme defaults)
        // 2. Navigation Bar: Visible + Transparent (Theme defaults)
        // 3. Icons: Viewer is dark (black background), so we need Light Icons (white).
        //    isAppearanceLightStatusBars = false (Light icons)
        //    isAppearanceLightNavigationBars = false (Light icons)
        
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        // Viewer has black background regardless of app theme.
        // Force nav bar to black to avoid mismatched bottom system bar.
        window.navigationBarColor = Color.Black.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        onDispose {
            // Restore to Theme defaults (Common Policy)
            // PicklyTheme logic: isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightStatusBars = !isDarkTheme
            controller.isAppearanceLightNavigationBars = !isDarkTheme

            window.navigationBarColor = if (isDarkTheme) {
                android.graphics.Color.TRANSPARENT
            } else {
                backgroundColor.toArgb()
            }
        }
    }
}
