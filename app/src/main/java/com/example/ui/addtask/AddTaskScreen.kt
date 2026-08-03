package com.example.ui.addtask

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import com.example.ui.category.CategoryDotIndicator
import com.example.ui.category.ManageCategoriesDialog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Folder
import com.example.data.entity.TaskGroup
import com.example.ui.taskgroup.parseGroupColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: com.example.viewmodel.ActivityTaskViewModel,
    taskId: String? = null,
    onNavigateToManageGroups: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val taskToEdit by (if (taskId != null) viewModel.getTaskById(taskId) else kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val dbCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val dbGroups by viewModel.allGroups.collectAsStateWithLifecycle()

    var isInitialized by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var pendingTask: com.example.data.entity.ActivityTask? by remember { mutableStateOf(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var taskGroupExpanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(Priority.Medium) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var repeatType by remember { mutableStateOf(RepeatType.None) }
    var repeatTypeExpanded by remember { mutableStateOf(false) }
    var customDaysInput by remember { mutableStateOf("3") }
    
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var isReminderEnabled by remember { mutableStateOf(false) }
    var reminderTimeMillis by remember { mutableStateOf<Long?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    var isTitleError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(taskToEdit, dbCategories) {
        if (taskToEdit != null && !isInitialized) {
            title = taskToEdit!!.title
            description = taskToEdit!!.description ?: ""
            category = taskToEdit!!.category
            selectedGroupId = taskToEdit!!.groupId
            priority = taskToEdit!!.priority
            repeatType = taskToEdit!!.repeatType
            customDaysInput = taskToEdit!!.customDays?.toString() ?: "3"
            dueDateMillis = taskToEdit!!.dueDate
            isReminderEnabled = taskToEdit!!.isReminderEnabled
            reminderTimeMillis = taskToEdit!!.reminderTime
            isInitialized = true
        } else if (taskToEdit == null && category.isBlank() && dbCategories.isNotEmpty()) {
            category = dbCategories.first().name
        }
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = {
                Text(
                    text = "Similar Task Exists",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "A similar task already exists. Do you still want to create this task?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateDialog = false
                        isSaving = true
                        pendingTask?.let {
                            if (taskId != null) viewModel.updateTask(it) else viewModel.insertTask(it)
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Create Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDuplicateDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId != null) "Edit Task" else "New Activity") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { 
                    title = it
                    if (it.isNotBlank()) isTitleError = false 
                },
                label = { Text("Title *") },
                isError = isTitleError,
                supportingText = if (isTitleError) {
                    { Text("Title is required") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            // Category Dropdown
            val currentCategoryObj = dbCategories.find { it.name.equals(category, ignoreCase = true) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        CategoryDotIndicator(colorHex = currentCategoryObj?.colorHex)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    dbCategories.forEach { cat ->
                        val isSelected = cat.name.equals(category, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryDotIndicator(colorHex = cat.colorHex)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat.name,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = {
                                category = cat.name
                                categoryExpanded = false
                            }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Manage Categories...",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            categoryExpanded = false
                            showManageCategoriesDialog = true
                        }
                    )
                }
            }

            // Task Group Dropdown
            val selectedGroupObj = dbGroups.find { it.id == selectedGroupId }
            ExposedDropdownMenuBox(
                expanded = taskGroupExpanded,
                onExpandedChange = { taskGroupExpanded = !taskGroupExpanded }
            ) {
                OutlinedTextField(
                    value = selectedGroupObj?.name ?: "No Group",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Task Group") },
                    leadingIcon = {
                        if (selectedGroupObj != null) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(parseGroupColor(selectedGroupObj.color), CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskGroupExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = taskGroupExpanded,
                    onDismissRequest = { taskGroupExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "No Group",
                                color = if (selectedGroupId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selectedGroupId == null) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            selectedGroupId = null
                            taskGroupExpanded = false
                        }
                    )
                    dbGroups.forEach { grp ->
                        val isSelected = grp.id == selectedGroupId
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(parseGroupColor(grp.color), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = grp.name,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = {
                                selectedGroupId = grp.id
                                taskGroupExpanded = false
                            }
                        )
                    }
                    if (onNavigateToManageGroups != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Manage Groups...",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.primary
                            ),
                            onClick = {
                                taskGroupExpanded = false
                                onNavigateToManageGroups()
                            }
                        )
                    }
                }
            }
            // Priority Dropdown
            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = !priorityExpanded }
            ) {
                OutlinedTextField(
                    value = priority.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Priority.values().forEach { prio ->
                        val isSelected = prio == priority
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = prio.name,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = {
                                priority = prio
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }
            // Due Date Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Due Date",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (dueDateMillis != null) {
                            IconButton(
                                onClick = {
                                    dueDateMillis = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear Due Date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            val dateStr = if (dueDateMillis != null) {
                                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dueDateMillis!!))
                            } else {
                                "Set Date"
                            }
                            Text(dateStr, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (dueDateMillis != null) {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueDateMillis!!))
                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(timeStr, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Reminder Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = if (isReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { enabled ->
                                isReminderEnabled = enabled
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    if (reminderTimeMillis == null) {
                                        reminderTimeMillis = dueDateMillis ?: (System.currentTimeMillis() + 3600000)
                                    }
                                }
                            }
                        )
                    }

                    if (isReminderEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = reminderTimeMillis?.let {
                                "Notify on: ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(it))}"
                            } ?: "No time set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (dueDateMillis != null) {
                                FilterChip(
                                    selected = reminderTimeMillis == dueDateMillis,
                                    onClick = { reminderTimeMillis = dueDateMillis },
                                    label = { Text("At due time") }
                                )
                                FilterChip(
                                    selected = reminderTimeMillis == (dueDateMillis!! - 900000),
                                    onClick = { reminderTimeMillis = dueDateMillis!! - 900000 },
                                    label = { Text("15 min before") }
                                )
                            }
                            OutlinedButton(
                                onClick = { showReminderDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Custom Time")
                            }
                        }
                    }
                }
            }
            // Repeat Type Dropdown
            ExposedDropdownMenuBox(
                expanded = repeatTypeExpanded,
                onExpandedChange = { repeatTypeExpanded = !repeatTypeExpanded }
            ) {
                OutlinedTextField(
                    value = repeatType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repeat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatTypeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = repeatTypeExpanded,
                    onDismissRequest = { repeatTypeExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    RepeatType.values().forEach { rep ->
                        val isSelected = rep == repeatType
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = rep.name,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = {
                                repeatType = rep
                                repeatTypeExpanded = false
                            }
                        )
                    }
                }
            }
            if (repeatType == RepeatType.Custom) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customDaysInput,
                    onValueChange = { customDaysInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Repeat Every (Days)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onNavigateBack) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    enabled = !isSaving,
                    onClick = {
                        if (title.isBlank()) {
                            isTitleError = true
                        } else {
                            if (!isSaving) {
                                 val newTask = if (taskToEdit != null) {
                                    taskToEdit!!.copy(
                                        title = title.trim(),
                                        description = description.trim(),
                                        category = category,
                                        groupId = selectedGroupId,
                                        priority = priority,
                                        dueDate = dueDateMillis,
                                        isReminderEnabled = isReminderEnabled,
                                        reminderTime = if (isReminderEnabled) reminderTimeMillis else null,
                                        repeatType = repeatType,
                                        customDays = if (repeatType == RepeatType.Custom) customDaysInput.toIntOrNull() ?: 1 else null
                                    )
                                } else {
                                    com.example.data.entity.ActivityTask(
                                        title = title.trim(),
                                        description = description.trim(),
                                        category = category,
                                        groupId = selectedGroupId,
                                        priority = priority,
                                        isCompleted = false,
                                        createdAt = System.currentTimeMillis(),
                                        dueDate = dueDateMillis,
                                        isReminderEnabled = isReminderEnabled,
                                        reminderTime = if (isReminderEnabled) reminderTimeMillis else null,
                                        repeatType = repeatType,
                                        parentTaskId = null,
                                        customDays = if (repeatType == RepeatType.Custom) customDaysInput.toIntOrNull() ?: 1 else null
                                    )
                                }

                                val duplicateTask = tasks.find {
                                    it.id != (taskToEdit?.id ?: "") &&
                                    it.title.trim().equals(newTask.title, ignoreCase = true) &&
                                    it.category == newTask.category &&
                                    it.dueDate == newTask.dueDate
                                }

                                if (duplicateTask != null) {
                                    pendingTask = newTask
                                    showDuplicateDialog = true
                                } else {
                                    isSaving = true
                                    if (taskToEdit != null) viewModel.updateTask(newTask) else viewModel.insertTask(newTask)
                                    onNavigateBack()
                                }
                            }
                        }
                    }
                ) {
                    Text(if (taskId != null) "Update" else "Save")
                }
            }
        }

        if (showManageCategoriesDialog) {
            ManageCategoriesDialog(
                categories = dbCategories,
                onAddCategory = { cat -> viewModel.insertCategory(cat) },
                onUpdateCategory = { cat, oldName -> viewModel.updateCategory(cat, oldName) },
                onDeleteCategory = { cat, targetCat -> viewModel.deleteCategory(cat, targetCat) },
                onDismissRequest = { showManageCategoriesDialog = false }
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                val currentCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis ?: System.currentTimeMillis() }
                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                            }
                            dueDateMillis = cal.timeInMillis
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            val initialCal = Calendar.getInstance().apply {
                timeInMillis = dueDateMillis ?: System.currentTimeMillis()
            }
            val timePickerState = rememberTimePickerState(
                initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
                initialMinute = initialCal.get(Calendar.MINUTE),
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Due Time") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dueDateMillis ?: System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }
                        dueDateMillis = cal.timeInMillis
                        if (isReminderEnabled && reminderTimeMillis == null) {
                            reminderTimeMillis = cal.timeInMillis
                        }
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
        }

        if (showReminderDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = reminderTimeMillis ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showReminderDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                val currentCal = Calendar.getInstance().apply { timeInMillis = reminderTimeMillis ?: System.currentTimeMillis() }
                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                            }
                            reminderTimeMillis = cal.timeInMillis
                        }
                        showReminderDatePicker = false
                        showReminderTimePicker = true
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showReminderTimePicker) {
            val initialCal = Calendar.getInstance().apply {
                timeInMillis = reminderTimeMillis ?: System.currentTimeMillis()
            }
            val timePickerState = rememberTimePickerState(
                initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
                initialMinute = initialCal.get(Calendar.MINUTE),
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showReminderTimePicker = false },
                title = { Text("Select Reminder Time") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = reminderTimeMillis ?: System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }
                        reminderTimeMillis = cal.timeInMillis
                        showReminderTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") }
                }
            )
        }
    }
}
