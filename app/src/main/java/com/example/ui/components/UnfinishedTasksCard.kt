package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ActivityTask
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnfinishedTasksCard(
    unfinishedTasks: List<ActivityTask>,
    onMoveToTomorrow: (ActivityTask) -> Unit,
    onKeep: (ActivityTask) -> Unit,
    onReschedule: (ActivityTask) -> Unit,
    modifier: Modifier = Modifier,
    isShadowMonarch: Boolean = false
) {
    if (unfinishedTasks.isEmpty()) return

    val containerColor = if (isShadowMonarch) {
        Color(0xFF2B201B)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    }

    val borderColor = if (isShadowMonarch) {
        Color(0xFFA0522D).copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    }

    val iconColor = if (isShadowMonarch) Color(0xFFFF8A65) else MaterialTheme.colorScheme.error

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("unfinished_tasks_card"),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "Unfinished Tasks",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UNFINISHED TASKS (${unfinishedTasks.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isShadowMonarch) Color(0xFFF2F3F7) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Decide what to do with tasks missed on earlier dates",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isShadowMonarch) Color(0xFF9E9EAF) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            unfinishedTasks.forEach { task ->
                val dateText = remember(task.dueDate) {
                    if (task.dueDate != null) {
                        val localDate = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                        localDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
                    } else "Past"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isShadowMonarch) Color(0xFF1E1E24) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Scheduled $dateText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AssistChip(
                                onClick = { onKeep(task) },
                                label = { Text("Keep here", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )

                            AssistChip(
                                onClick = { onMoveToTomorrow(task) },
                                label = { Text("Move to tomorrow", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            AssistChip(
                                onClick = { onReschedule(task) },
                                label = { Text("Choose date", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
