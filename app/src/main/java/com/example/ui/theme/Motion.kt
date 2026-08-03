package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

object MotionTokens {
    const val DurationFast = 150
    const val DurationStandard = 250
    const val DurationSlow = 350

    val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingStandard: Easing = FastOutSlowInEasing

    fun <T> fastTween() = tween<T>(durationMillis = DurationFast, easing = EasingStandard)
    fun <T> standardTween() = tween<T>(durationMillis = DurationStandard, easing = EasingEmphasized)
    fun <T> slowTween() = tween<T>(durationMillis = DurationSlow, easing = EasingEmphasized)
}

/**
 * Adds a subtle, responsive press scale feedback to interactive elements.
 */
@Composable
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    enabled: Boolean = true
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = MotionTokens.fastTween(),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
