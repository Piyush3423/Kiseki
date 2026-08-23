package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun KisekiTaskCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
    checkmarkColor: Color = MaterialTheme.colorScheme.onPrimary,
    size: Dp = 22.dp,
    enabled: Boolean = true
) {
    val scaleAnim = remember { Animatable(1f) }
    val checkProgressAnim = remember { Animatable(if (checked) 1f else 0f) }

    // Recomposition safety: track previous checked state to only animate on genuine transitions
    var lastCheckedState by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (checked != lastCheckedState) {
            val isChecking = checked
            lastCheckedState = checked

            if (isChecking) {
                // INCOMPLETE -> COMPLETE
                // 1. Press response: scale 1.0f -> 0.88f in 80ms
                scaleAnim.animateTo(
                    targetValue = 0.88f,
                    animationSpec = tween(durationMillis = 80, easing = LinearEasing)
                )
                // 2. Then spring back: 0.88f -> 1.12f -> 1.0f with restrained spring
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 550f
                        )
                    )
                }
                // Checkmark progress 0f -> 1f in 180ms
                launch {
                    checkProgressAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                }
            } else {
                // COMPLETE -> INCOMPLETE
                // 1. Checkbox scale: 1f -> 0.92f -> 1f
                scaleAnim.animateTo(
                    targetValue = 0.92f,
                    animationSpec = tween(durationMillis = 60, easing = LinearEasing)
                )
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        )
                    )
                }
                // Checkmark progress: 1f -> 0f in 160ms
                launch {
                    checkProgressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                    )
                }
            }
        } else {
            // Initial composition or non-transition recomposition: snap values
            scaleAnim.snapTo(1f)
            checkProgressAnim.snapTo(if (checked) 1f else 0f)
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (checked) activeColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "KisekiCheckboxBg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "KisekiCheckboxBorder"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(RoundedCornerShape(6.dp))
            .background(animatedBgColor)
            .border(
                border = BorderStroke(1.5.dp, animatedBorderColor),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                onCheckedChange()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = checkProgressAnim.value
            if (progress > 0f) {
                val path = Path().apply {
                    moveTo(this@Canvas.size.width * 0.28f, this@Canvas.size.height * 0.50f)
                    lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.66f)
                    lineTo(this@Canvas.size.width * 0.72f, this@Canvas.size.height * 0.34f)
                }
                val pathMeasure = PathMeasure()
                pathMeasure.setPath(path, false)
                val length = pathMeasure.length
                val segmentPath = Path()
                pathMeasure.getSegment(0f, length * progress, segmentPath, true)

                drawPath(
                    path = segmentPath,
                    color = checkmarkColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
