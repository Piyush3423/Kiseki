package com.example.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

const val MotionFast = 150
const val MotionStandard = 240
const val MotionSlow = 320

object MotionTokens {
    const val DurationFast = MotionFast
    const val DurationStandard = MotionStandard
    const val DurationSlow = MotionSlow

    val EasingMoving: Easing = FastOutSlowInEasing
    val EasingAppearing: Easing = LinearOutSlowInEasing
    val EasingDisappearing: Easing = FastOutLinearInEasing

    fun <T> fastTween() = tween<T>(durationMillis = MotionFast, easing = FastOutSlowInEasing)
    fun <T> standardTween() = tween<T>(durationMillis = MotionStandard, easing = FastOutSlowInEasing)
    fun <T> slowTween() = tween<T>(durationMillis = MotionSlow, easing = FastOutSlowInEasing)
    fun <T> appearingTween() = tween<T>(durationMillis = MotionStandard, easing = LinearOutSlowInEasing)
    fun <T> disappearingTween() = tween<T>(durationMillis = MotionStandard, easing = FastOutLinearInEasing)
}

/**
 * Adds a subtle, restrained press scale feedback (1f to 0.98f, 100ms) to interactive elements.
 */
@Composable
fun Modifier.pressScale(
    pressedScale: Float = 0.98f,
    enabled: Boolean = true
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val safeScale = pressedScale.coerceAtLeast(0.96f)
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) safeScale else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

