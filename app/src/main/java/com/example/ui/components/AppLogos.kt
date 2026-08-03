package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MonarchLogo - An original, anime-inspired dark fantasy "Shadow Monarch" emblem.
 * Features a sharp crown/horns silhouette, glowing dual-tone dagger core, shadow aura,
 * and electric cyan/purple energy sparks.
 */
@Composable
fun MonarchLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showAura: Boolean = true,
    isSelected: Boolean = true
) {
    val auraAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = tween(300),
        label = "auraAlpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height
            val scaleX = width / 100f
            val scaleY = height / 100f

            // 1. Outer Shadow Aura
            if (showAura) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8A2BE2).copy(alpha = 0.45f * auraAlpha),
                            Color(0xFF00E5FF).copy(alpha = 0.25f * auraAlpha),
                            Color(0xFF0D0B1E).copy(alpha = 0.1f * auraAlpha),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.5f),
                        radius = width * 0.52f
                    ),
                    radius = width * 0.5f,
                    center = Offset(width * 0.5f, height * 0.5f)
                )
            }

            // 2. Crown / Horns Shadow Silhouette
            val crownPath = Path().apply {
                moveTo(50f * scaleX, 88f * scaleY) // Bottom base tip
                cubicTo(
                    38f * scaleX, 82f * scaleY,
                    22f * scaleX, 72f * scaleY,
                    15f * scaleX, 58f * scaleY
                ) // Left lower curve
                lineTo(10f * scaleX, 28f * scaleY) // Left outer horn peak
                lineTo(25f * scaleX, 42f * scaleY) // Left outer notch
                lineTo(34f * scaleX, 20f * scaleY) // Left inner crown peak
                lineTo(44f * scaleX, 38f * scaleY) // Center left dip
                lineTo(50f * scaleX, 10f * scaleY) // Center Monarch Spire
                lineTo(56f * scaleX, 38f * scaleY) // Center right dip
                lineTo(66f * scaleX, 20f * scaleY) // Right inner crown peak
                lineTo(75f * scaleX, 42f * scaleY) // Right outer notch
                lineTo(90f * scaleX, 28f * scaleY) // Right outer horn peak
                cubicTo(
                    78f * scaleX, 72f * scaleY,
                    62f * scaleX, 82f * scaleY,
                    50f * scaleX, 88f * scaleY
                ) // Right lower curve
                close()
            }

            // Fill Crown Silhouette with deep metallic obsidian gradient
            drawPath(
                path = crownPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1038),
                        Color(0xFF0F0821),
                        Color(0xFF06030F)
                    )
                )
            )

            // Outline Crown with glowing electric purple / cyan stroke
            drawPath(
                path = crownPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFB026FF),
                        Color(0xFF6A0DAD)
                    )
                ),
                style = Stroke(
                    width = 2.2f * scaleX,
                    join = StrokeJoin.Round,
                    cap = StrokeCap.Round
                )
            )

            // 3. Central Dagger / Shadow Blade Core
            val leftBladePath = Path().apply {
                moveTo(50f * scaleX, 28f * scaleY)
                lineTo(38f * scaleX, 52f * scaleY)
                lineTo(50f * scaleX, 82f * scaleY)
                close()
            }
            val rightBladePath = Path().apply {
                moveTo(50f * scaleX, 28f * scaleY)
                lineTo(62f * scaleX, 52f * scaleY)
                lineTo(50f * scaleX, 82f * scaleY)
                close()
            }

            // Left facet cyan gradient
            drawPath(
                path = leftBladePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF0091EA)
                    )
                )
            )

            // Right facet violet gradient
            drawPath(
                path = rightBladePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD500F9),
                        Color(0xFF651FFF)
                    )
                )
            )

            // Center spine line
            drawLine(
                color = Color.White,
                start = Offset(50f * scaleX, 28f * scaleY),
                end = Offset(50f * scaleX, 82f * scaleY),
                strokeWidth = 1.8f * scaleX,
                cap = StrokeCap.Round
            )

            // 4. Energy Core / Rune Eye
            val eyePath = Path().apply {
                moveTo(50f * scaleX, 42f * scaleY)
                lineTo(56f * scaleX, 48f * scaleY)
                lineTo(50f * scaleX, 54f * scaleY)
                lineTo(44f * scaleX, 48f * scaleY)
                close()
            }
            drawPath(
                path = eyePath,
                color = Color(0xFFE0F7FA)
            )

            // 5. Energy Sparks (Top Horn Peaks)
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 2.5f * scaleX,
                center = Offset(10f * scaleX, 28f * scaleY)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 2.5f * scaleX,
                center = Offset(90f * scaleX, 28f * scaleY)
            )
            drawCircle(
                color = Color.White,
                radius = 3f * scaleX,
                center = Offset(50f * scaleX, 10f * scaleY)
            )
        }
    }
}

/**
 * KisekiLogo - A modern, anime-inspired emblem for Kiseki.
 * Features a stylized geometric 'K' monogram integrated into an ascending crystalline spark diamond
 * and an orbital trajectory arc, symbolizing growth, progress, and daily transformation.
 */
@Composable
fun KisekiLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlow: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height
            val scaleX = width / 100f
            val scaleY = height / 100f

            // 1. Soft Outer Glow
            if (showGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C4DFF).copy(alpha = 0.35f),
                            Color(0xFF00E5FF).copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.5f),
                        radius = width * 0.5f
                    ),
                    radius = width * 0.48f,
                    center = Offset(width * 0.5f, height * 0.5f)
                )
            }

            // 2. Orbital Trajectory Arc (Behind)
            val arcPath = Path().apply {
                moveTo(12f * scaleX, 68f * scaleY)
                cubicTo(
                    28f * scaleX, 85f * scaleY,
                    72f * scaleX, 85f * scaleY,
                    88f * scaleX, 62f * scaleY
                )
            }
            drawPath(
                path = arcPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF7C4DFF),
                        Color(0xFFD500F9)
                    )
                ),
                style = Stroke(
                    width = 3.5f * scaleX,
                    cap = StrokeCap.Round
                )
            )

            // 3. Left Vertical Pillar of 'K' Monogram
            val pillarPath = Path().apply {
                moveTo(22f * scaleX, 18f * scaleY) // Top notch left
                lineTo(34f * scaleX, 12f * scaleY) // Top notch peak
                lineTo(34f * scaleX, 82f * scaleY) // Bottom right
                lineTo(22f * scaleX, 82f * scaleY) // Bottom left
                close()
            }
            drawPath(
                path = pillarPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF651FFF),
                        Color(0xFF304FFE)
                    )
                )
            )

            // 4. Upper Ascending Arm of 'K' terminating in a Miracle Star
            val upperArmPath = Path().apply {
                moveTo(30f * scaleX, 52f * scaleY)
                lineTo(66f * scaleX, 22f * scaleY)
                lineTo(74f * scaleX, 30f * scaleY)
                lineTo(38f * scaleX, 60f * scaleY)
                close()
            }
            drawPath(
                path = upperArmPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF651FFF),
                        Color(0xFF00B0FF),
                        Color(0xFF80D8FF)
                    ),
                    start = Offset(30f * scaleX, 52f * scaleY),
                    end = Offset(74f * scaleX, 22f * scaleY)
                )
            )

            // 5. Lower Leg of 'K'
            val lowerLegPath = Path().apply {
                moveTo(36f * scaleX, 48f * scaleY)
                lineTo(72f * scaleX, 82f * scaleY)
                lineTo(62f * scaleX, 82f * scaleY)
                lineTo(28f * scaleX, 54f * scaleY)
                close()
            }
            drawPath(
                path = lowerLegPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7C4DFF),
                        Color(0xFFD500F9)
                    )
                )
            )

            // 6. Ascending Miracle Spark / Star Peak at top right
            val starCenterX = 76f * scaleX
            val starCenterY = 18f * scaleY
            val starPath = Path().apply {
                moveTo(starCenterX, starCenterY - 10f * scaleY)
                cubicTo(
                    starCenterX, starCenterY - 2f * scaleY,
                    starCenterX + 2f * scaleX, starCenterY,
                    starCenterX + 10f * scaleX, starCenterY
                )
                cubicTo(
                    starCenterX + 2f * scaleX, starCenterY,
                    starCenterX, starCenterY + 2f * scaleY,
                    starCenterX, starCenterY + 10f * scaleY
                )
                cubicTo(
                    starCenterX, starCenterY + 2f * scaleY,
                    starCenterX - 2f * scaleX, starCenterY,
                    starCenterX - 10f * scaleX, starCenterY
                )
                cubicTo(
                    starCenterX - 2f * scaleX, starCenterY,
                    starCenterX, starCenterY - 2f * scaleY,
                    starCenterX, starCenterY - 10f * scaleY
                )
                close()
            }
            drawPath(
                path = starPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFE0F7FA),
                        Color(0xFF00E5FF)
                    ),
                    center = Offset(starCenterX, starCenterY),
                    radius = 10f * scaleX
                )
            )

            // Core dot inside star
            drawCircle(
                color = Color.White,
                radius = 2.5f * scaleX,
                center = Offset(starCenterX, starCenterY)
            )
        }
    }
}

/**
 * KisekiLogoBadge - Styled container for Kiseki app logo used in About section & Headers.
 */
@Composable
fun KisekiLogoBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 56.dp,
    logoSize: Dp = 36.dp
) {
    Surface(
        modifier = modifier
            .size(badgeSize)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF7C4DFF).copy(alpha = 0.4f),
                spotColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF7C4DFF),
                    Color(0xFF00E5FF)
                )
            )
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B).copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        ) {
            KisekiLogo(size = logoSize)
        }
    }
}
