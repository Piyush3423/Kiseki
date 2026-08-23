package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.TomorrowWorkloadSummary
import com.example.domain.WorkloadCategory
import com.example.ui.theme.pressScale

@Composable
fun TomorrowPreviewCard(
    summary: TomorrowWorkloadSummary,
    onReviewTasks: () -> Unit,
    modifier: Modifier = Modifier,
    isShadowMonarch: Boolean = false
) {
    val isZeroTasks = summary.taskCount == 0

    val categoryColor = when (summary.category) {
        WorkloadCategory.LIGHT -> Color(0xFF2E7D32) // Soft Emerald Green
        WorkloadCategory.BALANCED -> if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary
        WorkloadCategory.HEAVY -> Color(0xFFE65100) // Soft Amber Orange
        WorkloadCategory.OVERLOADED -> Color(0xFFC62828) // Muted Crimson
    }

    val categoryBgColor = categoryColor.copy(alpha = if (isShadowMonarch) 0.18f else 0.12f)

    val cardBgColor = if (isShadowMonarch) {
        Color(0xFF1B1B26)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    val cardBorderColor = if (isShadowMonarch) {
        Color(0xFF2E2E42)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(0.99f)
            .clickable { onReviewTasks() }
            .testTag("tomorrow_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: TOMORROW & Load Category Chip (if tasks exist)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isShadowMonarch) Color(0xFF7967E8).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isShadowMonarch) Color(0xFFB0A2F7) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "TOMORROW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            ),
                            color = if (isShadowMonarch) Color(0xFF9E9EAF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isZeroTasks) "No tasks planned yet" else "${summary.taskCount} ${if (summary.taskCount == 1) "task" else "tasks"}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isShadowMonarch) Color(0xFFF0F0F5) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Category Tag (only if tasks > 0)
                if (!isZeroTasks) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryBgColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(categoryColor, CircleShape)
                            )
                            Text(
                                text = summary.category.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = categoryColor
                            )
                        }
                    }
                }
            }

            if (!isZeroTasks) {
                // Key Stats Grid: Priority / Est. Workload / Load %
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isShadowMonarch) Color(0xFF14141E) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // High Priority Count
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (summary.highPriorityCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFB3261E), CircleShape)
                                )
                                Text(
                                    text = summary.prioritySummary.ifEmpty { "${summary.highPriorityCount} High" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFB3261E)
                                )
                            } else {
                                Text(
                                    text = summary.prioritySummary.ifEmpty { "Standard" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (isShadowMonarch) Color(0xFFD0D0DC) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Estimated Workload
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Est. Workload",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary.formattedDuration,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isShadowMonarch) Color(0xFFF0F0F5) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Load Percentage
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Load",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${summary.loadPercentage}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = categoryColor
                        )
                    }
                }

                // Visual Load Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { (summary.clampedLoadPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = categoryColor,
                        trackColor = if (isShadowMonarch) Color(0xFF262638) else MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Heavy Day Soft Warning
                if (summary.isHeavyOrOverloaded) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryColor.copy(alpha = if (isShadowMonarch) 0.16f else 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (summary.category == WorkloadCategory.OVERLOADED) "⚠ Overloaded day · Consider pacing yourself" else "⚠ Heavy day · Consider pacing high-priority tasks",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = categoryColor
                            )
                        }
                    }
                }
            }

            // Action Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onReviewTasks,
                    modifier = Modifier
                        .pressScale()
                        .testTag("review_tomorrow_tasks_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (isShadowMonarch) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7967E8),
                            contentColor = Color.White
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                ) {
                    Icon(
                        imageVector = if (isZeroTasks) Icons.Rounded.CalendarToday else Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isZeroTasks) "Plan Tomorrow" else "Review Tasks",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
