package com.cola.pickly.core.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun FullImmersiveMode() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return

    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(window, view)

        // systemBars를 항상 숨김
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            // 화면 종료 시 원래 상태로 복원
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
