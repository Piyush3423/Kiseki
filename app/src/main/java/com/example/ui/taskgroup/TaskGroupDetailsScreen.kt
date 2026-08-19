package com.example.ui.taskgroup

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ActivityTask
import com.example.data.entity.TaskGroup
import com.example.data.repository.ThemeMode
import com.example.ui.home.TaskItemCard
import com.example.viewmodel.ActivityTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGroupDetailsScreen(
    groupId: String,
    viewModel: ActivityTaskViewModel,
    onNavigateBack: () -> Unit,
    onEditTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val group = groups.find { it.id == groupId }

    val groupTasks by viewModel.getTasksForGroup(groupId).collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val dbCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showAddTasksDialog by remember { mutableStateOf(false) }
    var showGroupActionsSheet by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<TaskGroup?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<ActivityTask?>(null) }

    if (group == null) {
        // Handle deleted or nonexistent group
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Task Group") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Group not found or has been deleted.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val groupColor = parseGroupColor(group.color)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(groupColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = group.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { groupToEdit = group }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Group"
                        )
                    }
                    IconButton(onClick = { showDeleteGroupDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete Group",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header card summarizing group stats & action
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = groupColor.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = groupColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val completedCount = groupTasks.count { it.isCompleted }
                            Text(
                                text = "${groupTasks.size} ${if (groupTasks.size == 1) "task" else "tasks"} • $completedCount completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddTasksDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Tasks", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showGroupActionsSheet = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Group Actions", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (groupTasks.isEmpty()) {
                // Empty state for group tasks
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No tasks in this group",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Add existing tasks to organize them into this group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = { showAddTasksDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Tasks to Group")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = groupTasks,
                        key = { it.id }
                    ) { task ->
                        TaskItemCard(
                            task = task,
                            categories = dbCategories,
                            groups = groups,
                            onToggleComplete = {
                                viewModel.updateTask(it.copy(isCompleted = !it.isCompleted))
                            },
                            onDelete = { taskToDelete = it },
                            onEdit = { onEditTaskClick(it.id) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                fadeOutSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                placementSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            ),
                            themeMode = themeMode,
                            onRemoveFromGroup = {
                                viewModel.removeTaskFromGroup(task.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Existing Tasks to Group Dialog
    if (showAddTasksDialog) {
        val availableTasks = allTasks.filter { it.groupId != groupId }

        AlertDialog(
            onDismissRequest = { showAddTasksDialog = false },
            title = {
                Text(
                    text = "Add Tasks to Group",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (availableTasks.isEmpty()) {
                    Text(
                        text = "All tasks are already assigned to this group or no other tasks exist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = availableTasks,
                            key = { it.id }
                        ) { task ->
                            val isInThisGroup = task.groupId == groupId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        if (isInThisGroup) {
                                            viewModel.removeTaskFromGroup(task.id)
                                        } else {
                                            viewModel.assignTaskToGroup(task.id, groupId)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (task.category.isNotBlank()) {
                                        Text(
                                            text = task.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isInThisGroup,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.assignTaskToGroup(task.id, groupId)
                                        } else {
                                            viewModel.removeTaskFromGroup(task.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAddTasksDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Edit Group Dialog
    groupToEdit?.let { editGroup ->
        TaskGroupDialog(
            title = "Edit Task Group",
            confirmButtonText = "Save",
            initialName = editGroup.name,
            initialColor = editGroup.color,
            currentGroupId = editGroup.id,
            existingGroups = groups,
            onDismiss = { groupToEdit = null },
            onConfirm = { name, color ->
                val updated = editGroup.copy(name = name, color = color)
                viewModel.updateGroup(updated) { result ->
                    if (result.isSuccess) {
                        groupToEdit = null
                    }
                }
            }
        )
    }

    // Delete Group Dialog
    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = {
                Text(
                    text = "Delete Task Group?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${group.name}\"? Tasks in this group will not be deleted and will become ungrouped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(group.id)
                        showDeleteGroupDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Task Dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Group Actions Bottom Sheet
    if (showGroupActionsSheet) {
        GroupActionsBottomSheet(
            group = group,
            tasks = groupTasks,
            onDismiss = { showGroupActionsSheet = false },
            onMarkAllComplete = {
                viewModel.bulkMarkGroupTasksCompleted(groupId)
            },
            onMarkAllIncomplete = {
                viewModel.bulkMarkGroupTasksIncomplete(groupId)
            },
            onSetPriority = { priority ->
                viewModel.bulkSetGroupTasksPriority(groupId, priority)
            },
            onSetDueDate = { dueDate ->
                viewModel.bulkSetGroupTasksDueDate(groupId, dueDate)
            },
            onSetRepeatType = { repeatType, customDays ->
                viewModel.bulkSetGroupTasksRepeatType(groupId, repeatType, customDays)
            },
            onRemoveAllTasks = {
                viewModel.bulkRemoveAllTasksFromGroup(groupId)
            }
        )
    }
}
