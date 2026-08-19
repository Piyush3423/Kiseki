package com.example.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

const val MotionTouchFeedback = 100
const val MotionFast = 160
const val MotionNormal = 240
const val MotionStandard = 240
const val MotionScreenTransition = 280
const val MotionSlow = 320

object MotionTokens {
    const val DurationTouch = MotionTouchFeedback
    const val DurationFast = MotionFast
    const val DurationNormal = MotionNormal
    const val DurationStandard = MotionStandard
    const val DurationScreen = MotionScreenTransition
    const val DurationSlow = MotionSlow

    val EasingMoving: Easing = FastOutSlowInEasing
    val EasingAppearing: Easing = LinearOutSlowInEasing
    val EasingDisappearing: Easing = FastOutLinearInEasing

    fun <T> touchTween() = tween<T>(durationMillis = MotionTouchFeedback, easing = FastOutSlowInEasing)
    fun <T> fastTween() = tween<T>(durationMillis = MotionFast, easing = FastOutSlowInEasing)
    fun <T> normalTween() = tween<T>(durationMillis = MotionNormal, easing = FastOutSlowInEasing)
    fun <T> standardTween() = tween<T>(durationMillis = MotionStandard, easing = FastOutSlowInEasing)
    fun <T> screenTween() = tween<T>(durationMillis = MotionScreenTransition, easing = FastOutSlowInEasing)
    fun <T> slowTween() = tween<T>(durationMillis = MotionSlow, easing = FastOutSlowInEasing)
}

/**
 * Adds a subtle, restrained press scale feedback to interactive elements.
 */
@Composable
fun Modifier.pressScale(
    pressedScale: Float = 0.98f,
    interactionSource: InteractionSource? = null,
    enabled: Boolean = true
): Modifier {
    val fallbackSource = remember { MutableInteractionSource() }
    val source = interactionSource ?: fallbackSource
    val isPressed by source.collectIsPressedAsState()
    val safeScale = pressedScale.coerceIn(0.95f, 1f)
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) safeScale else 1f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}


