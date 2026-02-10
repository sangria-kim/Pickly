package com.cola.pickly.core.ui.transition

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@OptIn(ExperimentalSharedTransitionApi::class)
object ContainerTransformSpec {
    const val FORWARD_DURATION_MS = 400
    const val BACKWARD_DURATION_MS = 350
    const val OVERLAY_ENTER_DURATION_MS = 250
    const val OVERLAY_EXIT_DURATION_MS = 200
    const val NAV_FADE_DURATION_MS = 400

    val PhotoContainerTransform = BoundsTransform { initialBounds, targetBounds ->
        val isExpanding = targetBounds.width * targetBounds.height >
                initialBounds.width * initialBounds.height
        if (isExpanding) {
            tween(durationMillis = FORWARD_DURATION_MS, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = BACKWARD_DURATION_MS, easing = FastOutLinearInEasing)
        }
    }

    fun overlayEnter() = fadeIn(tween(OVERLAY_ENTER_DURATION_MS, easing = LinearOutSlowInEasing))
    fun overlayExit() = fadeOut(tween(OVERLAY_EXIT_DURATION_MS, easing = FastOutLinearInEasing))
    fun navFadeIn() = fadeIn(tween(NAV_FADE_DURATION_MS, easing = LinearOutSlowInEasing))
    fun navFadeOut() = fadeOut(tween(NAV_FADE_DURATION_MS, easing = FastOutLinearInEasing))
}
