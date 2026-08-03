package com.example.ui.taskgroup

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ActivityTask
import com.example.data.entity.TaskGroup
import com.example.data.model.Priority
import com.example.data.model.RepeatType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupActionsBottomSheet(
    group: TaskGroup,
    tasks: List<ActivityTask>,
    onDismiss: () -> Unit,
    onMarkAllComplete: () -> Unit,
    onMarkAllIncomplete: () -> Unit,
    onSetPriority: (Priority) -> Unit,
    onSetDueDate: (Long?) -> Unit,
    onSetRepeatType: (RepeatType, Int?) -> Unit,
    onRemoveAllTasks: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    var showCompleteConfirmDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }

    val taskCount = tasks.size
    val incompleteCount = tasks.count { !it.isCompleted }
    val completedCount = tasks.count { it.isCompleted }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Group Actions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$taskCount ${if (taskCount == 1) "task" else "tasks"} in \"${group.name}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Actions List
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // 1. Mark All Complete
                GroupActionItem(
                    title = "Mark all complete",
                    subtitle = if (incompleteCount > 0) "Complete $incompleteCount incomplete ${if (incompleteCount == 1) "task" else "tasks"}" else "All tasks are already completed",
                    icon = Icons.Rounded.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    enabled = incompleteCount > 0,
                    onClick = {
                        showCompleteConfirmDialog = true
                    }
                )

                // 2. Mark All Incomplete
                GroupActionItem(
                    title = "Mark all incomplete",
                    subtitle = if (completedCount > 0) "Reopen $completedCount completed ${if (completedCount == 1) "task" else "tasks"}" else "No completed tasks to reopen",
                    icon = Icons.Rounded.RadioButtonUnchecked,
                    iconTint = MaterialTheme.colorScheme.primary,
                    enabled = completedCount > 0,
                    onClick = {
                        onMarkAllIncomplete()
                        onDismiss()
                    }
                )

                // 3. Set Priority
                GroupActionItem(
                    title = "Set priority",
                    subtitle = "Set Low, Medium, or High priority for $taskCount ${if (taskCount == 1) "task" else "tasks"}",
                    icon = Icons.Rounded.Flag,
                    iconTint = Color(0xFFFF9800),
                    enabled = taskCount > 0,
                    onClick = {
                        showPriorityDialog = true
                    }
                )

                // 4. Set Due Date
                GroupActionItem(
                    title = "Set due date",
                    subtitle = "Assign or clear due date for $taskCount ${if (taskCount == 1) "task" else "tasks"}",
                    icon = Icons.Rounded.Event,
                    iconTint = Color(0xFF2196F3),
                    enabled = taskCount > 0,
                    onClick = {
                        showDatePickerDialog = true
                    }
                )

                // 5. Set Repeat Pattern
                GroupActionItem(
                    title = "Set repeat pattern",
                    subtitle = "Set frequency (Daily, Weekly, Monthly, Custom) for $taskCount ${if (taskCount == 1) "task" else "tasks"}",
                    icon = Icons.Rounded.Repeat,
                    iconTint = Color(0xFF9C27B0),
                    enabled = taskCount > 0,
                    onClick = {
                        showRepeatDialog = true
                    }
                )

                // 6. Remove All Tasks from Group
                GroupActionItem(
                    title = "Remove all tasks from group",
                    subtitle = "Ungroup all $taskCount ${if (taskCount == 1) "task" else "tasks"} without deleting them",
                    icon = Icons.Rounded.FolderOff,
                    iconTint = MaterialTheme.colorScheme.error,
                    isDestructive = true,
                    enabled = taskCount > 0,
                    onClick = {
                        showRemoveConfirmDialog = true
                    }
                )
            }
        }
    }

    // --- Dialogs ---

    // Complete All Confirmation Dialog
    if (showCompleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmDialog = false },
            title = {
                Text(
                    text = "Complete $incompleteCount ${if (incompleteCount == 1) "Task" else "Tasks"}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to mark all $incompleteCount incomplete tasks in \"${group.name}\" as complete? Recurring tasks will automatically generate their next occurrence.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteConfirmDialog = false
                        onMarkAllComplete()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mark Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove All Tasks Confirmation Dialog
    if (showRemoveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmDialog = false },
            title = {
                Text(
                    text = "Remove $taskCount ${if (taskCount == 1) "Task" else "Tasks"} from Group?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all $taskCount tasks from \"${group.name}\"? The tasks will remain in your task list as ungrouped tasks.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirmDialog = false
                        onRemoveAllTasks()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Remove Tasks")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set Priority Dialog
    if (showPriorityDialog) {
        var selectedPriority by remember { mutableStateOf(Priority.Medium) }

        AlertDialog(
            onDismissRequest = { showPriorityDialog = false },
            title = {
                Column {
                    Text("Set Priority", fontWeight = FontWeight.Bold)
                    Text(
                        "Affects $taskCount ${if (taskCount == 1) "task" else "tasks"} in \"${group.name}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.values().forEach { priority ->
                        val (pColor, pText) = when (priority) {
                            Priority.High -> Color(0xFFE53935) to "High"
                            Priority.Medium -> Color(0xFFFB8C00) to "Medium"
                            Priority.Low -> Color(0xFF43A047) to "Low"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedPriority == priority)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .clickable { selectedPriority = priority }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = pText,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            RadioButton(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPriorityDialog = false
                        onSetPriority(selectedPriority)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply to $taskCount ${if (taskCount == 1) "Task" else "Tasks"}")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriorityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set Due Date Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            showDatePickerDialog = false
                            onSetDueDate(null)
                            onDismiss()
                        }
                    ) {
                        Text("Clear Due Date", color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            val selectedMillis = datePickerState.selectedDateMillis
                            showDatePickerDialog = false
                            if (selectedMillis != null) {
                                onSetDueDate(selectedMillis)
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply Date")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            Column {
                Text(
                    text = "Apply due date to $taskCount ${if (taskCount == 1) "task" else "tasks"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                DatePicker(state = datePickerState)
            }
        }
    }

    // Set Repeat Pattern Dialog
    if (showRepeatDialog) {
        var selectedRepeat by remember { mutableStateOf(RepeatType.Daily) }
        var customDaysInput by remember { mutableStateOf("2") }

        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = {
                Column {
                    Text("Set Repeat Pattern", fontWeight = FontWeight.Bold)
                    Text(
                        "Affects $taskCount ${if (taskCount == 1) "task" else "tasks"} in \"${group.name}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatType.values().forEach { rep ->
                        val repLabel = when (rep) {
                            RepeatType.None -> "None (No repeat)"
                            RepeatType.Daily -> "Daily"
                            RepeatType.Weekly -> "Weekly"
                            RepeatType.Monthly -> "Monthly"
                            RepeatType.Custom -> "Custom (Every X days)"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedRepeat == rep)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .clickable { selectedRepeat = rep }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = repLabel,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            RadioButton(
                                selected = selectedRepeat == rep,
                                onClick = { selectedRepeat = rep }
                            )
                        }
                    }

                    if (selectedRepeat == RepeatType.Custom) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customDaysInput,
                            onValueChange = { customDaysInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Repeat every N days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customDays = if (selectedRepeat == RepeatType.Custom) {
                            customDaysInput.toIntOrNull() ?: 1
                        } else null
                        showRepeatDialog = false
                        onSetRepeatType(selectedRepeat, customDays)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply to $taskCount ${if (taskCount == 1) "Task" else "Tasks"}")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepeatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GroupActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isDestructive && enabled)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (enabled) iconTint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) iconTint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = if (!enabled)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else if (isDestructive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
