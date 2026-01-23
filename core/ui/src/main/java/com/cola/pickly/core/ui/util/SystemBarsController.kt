package com.cola.pickly.core.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.compose.ui.graphics.toArgb


@Composable
fun ViewerSystemBarsPolicy() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    // Use surface color for navigation bar to match app background (bottom nav)
    val navBarColor = colorScheme.surface.toArgb()

    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(window, view)
        
        // Apply Viewer Policy:
        // 1. Hide Status Bar
        controller.hide(WindowInsetsCompat.Type.statusBars())
        // 2. Show Navigation Bar
        controller.show(WindowInsetsCompat.Type.navigationBars())
        // 3. Set Navigation Bar Color to App Background
        window.navigationBarColor = navBarColor
        
        // Ensure transient bars behavior for status bar swipe
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            // Restore to Common Policy (Transparent)
            controller.show(WindowInsetsCompat.Type.statusBars())
            controller.show(WindowInsetsCompat.Type.navigationBars())
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}
