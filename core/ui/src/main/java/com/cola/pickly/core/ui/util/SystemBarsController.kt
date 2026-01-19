package com.cola.pickly.core.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ImmersiveMode(isOverlayVisible: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return

    DisposableEffect(isOverlayVisible) {
        val controller = WindowCompat.getInsetsController(window, view)

        if (isOverlayVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
