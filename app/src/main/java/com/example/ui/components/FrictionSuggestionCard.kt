package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrictionSuggestionCard(
    task: ActivityTask,
    onBreakIntoSubtasks: () -> Unit,
    onReschedule: () -> Unit,
    onLowerPriority: () -> Unit,
    onKeepAsIs: () -> Unit,
    modifier: Modifier = Modifier,
    isShadowMonarch: Boolean = false
) {
    val containerColor = if (isShadowMonarch) {
        Color(0xFF221B14)
    } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    }

    val borderColor = if (isShadowMonarch) {
        Color(0xFF8B5E2B).copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
    }

    val iconColor = if (isShadowMonarch) Color(0xFFFFB74D) else MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassEmpty,
                        contentDescription = "Task Friction Warning",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "This task keeps getting postponed.",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = onBreakIntoSubtasks,
                    label = { Text("Break into subtasks", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )

                AssistChip(
                    onClick = onReschedule,
                    label = { Text("Reschedule", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Event,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )

                if (task.priority != Priority.Low) {
                    AssistChip(
                        onClick = onLowerPriority,
                        label = { Text("Lower priority", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.South,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                }

                AssistChip(
                    onClick = onKeepAsIs,
                    label = { Text("Keep as is", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

@Composable
fun BreakIntoSubtasksDialog(
    taskTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var subtasks by remember { mutableStateOf(listOf("", "")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Break into smaller tasks",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Create 2 to 5 smaller subtasks for '$taskTitle':",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                subtasks.forEachIndexed { index, title ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { newText ->
                                val updated = subtasks.toMutableList()
                                updated[index] = newText
                                subtasks = updated
                            },
                            label = { Text("Subtask ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (subtasks.size > 2) {
                            IconButton(
                                onClick = {
                                    val updated = subtasks.toMutableList()
                                    updated.removeAt(index)
                                    subtasks = updated
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove subtask"
                                )
                            }
                        }
                    }
                }

                if (subtasks.size < 5) {
                    TextButton(
                        onClick = {
                            subtasks = subtasks + ""
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subtask")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = subtasks.map { it.trim() }.filter { it.isNotBlank() }
                    if (valid.size >= 2) {
                        onConfirm(valid)
                    }
                },
                enabled = subtasks.count { it.trim().isNotBlank() } >= 2
            ) {
                Text("Create Subtasks")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RescheduleFrictionTaskDialog(
    taskTitle: String,
    currentDueDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val now = LocalDate.now()
    val zoneId = ZoneId.systemDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reschedule Task",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pick a new target date for '$taskTitle':",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val tomorrowMillis = now.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                        onConfirm(tomorrowMillis)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Tomorrow (${now.plusDays(1).format(DateTimeFormatter.ofPattern("E, MMM d"))})")
                }

                Button(
                    onClick = {
                        val in3DaysMillis = now.plusDays(3).atStartOfDay(zoneId).toInstant().toEpochMilli()
                        onConfirm(in3DaysMillis)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("In 3 days (${now.plusDays(3).format(DateTimeFormatter.ofPattern("E, MMM d"))})")
                }

                Button(
                    onClick = {
                        val nextWeekMillis = now.plusWeeks(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                        onConfirm(nextWeekMillis)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Next Week (${now.plusWeeks(1).format(DateTimeFormatter.ofPattern("E, MMM d"))})")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
