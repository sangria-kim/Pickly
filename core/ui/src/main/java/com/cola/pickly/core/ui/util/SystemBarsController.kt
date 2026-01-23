package com.cola.pickly.core.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.compose.ui.graphics.toArgb


import androidx.compose.ui.graphics.luminance

@Composable
fun ViewerSystemBarsPolicy() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    
    // Check actual theme darkness using background luminance
    // This handles cases where App Theme overrides System Theme
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
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

        onDispose {
            // Restore to Theme defaults (Common Policy)
            // PicklyTheme logic: isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightStatusBars = !isDarkTheme
            controller.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
}
