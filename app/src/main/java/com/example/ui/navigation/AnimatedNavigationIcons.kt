package com.example.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedNavigationIcon(
    route: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    iconColor: Color = LocalContentColor.current
) {
    when (route) {
        Routes.TODAY -> AnimatedHomeIcon(selected = selected, modifier = modifier, iconColor = iconColor)
        Routes.HISTORY -> AnimatedHistoryIcon(selected = selected, modifier = modifier, iconColor = iconColor)
        Routes.ANALYTICS -> AnimatedAnalyticsIcon(selected = selected, modifier = modifier, iconColor = iconColor)
        Routes.SETTINGS -> AnimatedSettingsIcon(selected = selected, modifier = modifier, iconColor = iconColor)
        else -> Box(modifier = modifier.size(24.dp))
    }
}

@Composable
fun AnimatedHomeIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    iconColor: Color = LocalContentColor.current
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 500f
        ),
        label = "homeScale"
    )

    val roofOffset by animateDpAsState(
        targetValue = if (selected) 0.dp else 3.dp,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "homeRoofOffset"
    )
    
    val bodyOffset by animateDpAsState(
        targetValue = if (selected) 0.dp else 2.dp,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "homeBodyOffset"
    )

    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = tween(180),
        label = "homeAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 2.dp.toPx()
            
            // Roof
            val roofY = roofOffset.toPx()
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.1f, size.height * 0.45f + roofY),
                end = Offset(size.width * 0.5f, size.height * 0.1f + roofY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.5f, size.height * 0.1f + roofY),
                end = Offset(size.width * 0.9f, size.height * 0.45f + roofY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            
            // Body
            val bodyY = bodyOffset.toPx()
            drawRect(
                color = iconColor,
                topLeft = Offset(size.width * 0.2f, size.height * 0.45f + bodyY),
                size = Size(size.width * 0.6f, size.height * 0.45f),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun AnimatedHistoryIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    iconColor: Color = LocalContentColor.current
) {
    val rotation by animateFloatAsState(
        targetValue = if (selected) 0f else -35f,
        animationSpec = tween(
            durationMillis = 380,
            easing = FastOutSlowInEasing
        ),
        label = "historyRotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 450f
        ),
        label = "historyScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = tween(180),
        label = "historyAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 2.dp.toPx()
            
            withTransform({
                rotate(
                    degrees = rotation,
                    pivot = Offset(size.width / 2, size.height / 2)
                )
            }) {
                // Draw circular arrow (history)
                drawArc(
                    color = iconColor,
                    startAngle = -150f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
                    size = Size(size.width * 0.7f, size.height * 0.7f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // Draw arrow head
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.1f, size.height * 0.4f),
                    end = Offset(size.width * 0.25f, size.height * 0.2f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.4f, size.height * 0.35f),
                    end = Offset(size.width * 0.25f, size.height * 0.2f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            
            // Draw clock hands
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.5f, size.height * 0.5f),
                end = Offset(size.width * 0.5f, size.height * 0.35f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.5f, size.height * 0.5f),
                end = Offset(size.width * 0.65f, size.height * 0.65f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AnimatedAnalyticsIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    iconColor: Color = LocalContentColor.current
) {
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = tween(180),
        label = "analyticsAlpha"
    )

    val bar1 = remember { Animatable(0.35f) }
    val bar2 = remember { Animatable(0.55f) }
    val bar3 = remember { Animatable(0.75f) }

    LaunchedEffect(selected) {
        if (selected) {
            bar1.snapTo(0.15f)
            bar2.snapTo(0.15f)
            bar3.snapTo(0.15f)

            launch {
                bar1.animateTo(
                    0.45f,
                    tween(240, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(70)
                bar2.animateTo(
                    0.70f,
                    tween(260, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(140)
                bar3.animateTo(
                    1f,
                    tween(280, easing = FastOutSlowInEasing)
                )
            }
        } else {
            bar1.snapTo(0.35f)
            bar2.snapTo(0.55f)
            bar3.snapTo(0.75f)
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
        }
    ) {
        val barWidth = size.width * 0.16f
        val gap = size.width * 0.12f
        val startX = (size.width - (barWidth * 3 + gap * 2)) / 2

        // Bar 1
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(
                startX,
                size.height * (1f - bar1.value)
            ),
            size = Size(
                barWidth,
                size.height * bar1.value
            ),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // Bar 2
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(
                startX + barWidth + gap,
                size.height * (1f - bar2.value)
            ),
            size = Size(
                barWidth,
                size.height * bar2.value
            ),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // Bar 3
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(
                startX + (barWidth + gap) * 2,
                size.height * (1f - bar3.value)
            ),
            size = Size(
                barWidth,
                size.height * bar3.value
            ),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun AnimatedSettingsIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    iconColor: Color = LocalContentColor.current
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 450f
        ),
        label = "settingsScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = tween(180),
        label = "settingsAlpha"
    )

    val rotation = remember { Animatable(-20f) }

    LaunchedEffect(selected) {
        if (selected) {
            rotation.snapTo(-20f)
            rotation.animateTo(
                25f,
                animationSpec = tween(
                    180,
                    easing = FastOutSlowInEasing
                )
            )
            rotation.animateTo(
                0f,
                animationSpec = tween(
                    160,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            rotation.snapTo(-20f)
        }
    }

    Icon(
        imageVector = Icons.Outlined.Settings,
        contentDescription = "Settings",
        tint = iconColor,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation.value
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    )
}
