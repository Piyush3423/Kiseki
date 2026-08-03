package com.example.ui.taskgroup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.entity.TaskGroup
import com.example.viewmodel.ActivityTaskViewModel

val PRESET_GROUP_COLORS = listOf(
    0xFF8B5CF6.toInt(), // Violet
    0xFF3B82F6.toInt(), // Blue
    0xFF10B981.toInt(), // Emerald Green
    0xFFF59E0B.toInt(), // Amber
    0xFFEC4899.toInt(), // Pink
    0xFF6366F1.toInt(), // Indigo
    0xFF14B8A6.toInt(), // Teal
    0xFFEF4444.toInt(), // Red
    0xFF84CC16.toInt(), // Lime
    0xFF64748B.toInt()  // Slate
)

fun parseGroupColor(colorInt: Int?, defaultColor: Color = Color(0xFF8B5CF6)): Color {
    return colorInt?.let { Color(it) } ?: defaultColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGroupScreen(
    viewModel: ActivityTaskViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onGroupClick: ((String) -> Unit)? = null
) {
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<TaskGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<TaskGroup?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Task Groups",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Task Group"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (groups.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No Task Groups Yet",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Create groups to organize and structure your tasks effortlessly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Task Group")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${groups.size} ${if (groups.size == 1) "Group" else "Groups"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(
                        items = groups,
                        key = { it.id }
                    ) { group ->
                        val count = allTasks.count { it.groupId == group.id }
                        TaskGroupCard(
                            group = group,
                            taskCount = count,
                            onClick = if (onGroupClick != null) { { onGroupClick(group.id) } } else null,
                            onEdit = { groupToEdit = group },
                            onDelete = { groupToDelete = group }
                        )
                    }
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateDialog) {
        TaskGroupDialog(
            title = "Create Task Group",
            confirmButtonText = "Create",
            existingGroups = groups,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color ->
                viewModel.createGroup(name, color) { result ->
                    if (result.isSuccess) {
                        showCreateDialog = false
                    }
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

    // Delete Group Confirmation Dialog
    groupToDelete?.let { delGroup ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = {
                Text(
                    text = "Delete Task Group?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${delGroup.name}\"? Tasks in this group will not be deleted and will become ungrouped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(delGroup.id)
                        groupToDelete = null
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
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TaskGroupCard(
    group: TaskGroup,
    taskCount: Int = 0,
    onClick: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = parseGroupColor(group.color)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (taskCount == 1) "1 task" else "$taskCount tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete group",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskGroupDialog(
    title: String,
    confirmButtonText: String,
    existingGroups: List<TaskGroup>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int?) -> Unit,
    initialName: String = "",
    initialColor: Int? = PRESET_GROUP_COLORS[0],
    currentGroupId: String? = null
) {
    var nameText by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf<Int?>(initialColor ?: PRESET_GROUP_COLORS[0]) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        errorMessage = null
                    },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Work, Personal, Fitness") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Group Color",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PRESET_GROUP_COLORS.forEach { colorInt ->
                        val isSelected = selectedColor == colorInt
                        val composeColor = Color(colorInt)

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(composeColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else composeColor.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorInt },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = nameText.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Group name cannot be empty"
                        return@Button
                    }
                    val isDuplicate = existingGroups.any {
                        it.id != currentGroupId && it.name.equals(trimmed, ignoreCase = true)
                    }
                    if (isDuplicate) {
                        errorMessage = "A group with this name already exists"
                        return@Button
                    }
                    onConfirm(trimmed, selectedColor)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
