package com.example.ui.addtask

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.pressScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ActivityTask
import com.example.data.entity.TaskGroup
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import com.example.domain.TaskNaturalLanguageParser
import com.example.ui.category.CategoryDotIndicator
import com.example.ui.category.ManageCategoriesDialog
import com.example.ui.taskgroup.parseGroupColor
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

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
    var estimatedDurationInput by remember { mutableStateOf("") }
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

    // Natural Language Parsing State
    var naturalLanguageInput by remember { mutableStateOf("") }
    val parsedResult = remember(naturalLanguageInput) {
        if (naturalLanguageInput.isNotBlank()) {
            TaskNaturalLanguageParser.parse(naturalLanguageInput)
        } else {
            null
        }
    }

    // Title live parsing detection for users typing directly in Title
    val titleParsedResult = remember(title) {
        if (title.length >= 6 && naturalLanguageInput.isBlank() && taskToEdit == null) {
            val parsed = TaskNaturalLanguageParser.parse(title)
            if (parsed.hasExtractedAnyField) parsed else null
        } else {
            null
        }
    }

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
            estimatedDurationInput = taskToEdit!!.estimatedDurationMinutes?.toString() ?: ""
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
            // Smart Quick Fill Natural Language Card (shown on new task or optional helper)
            if (taskToEdit == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Smart Quick Fill",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Type in plain English to auto-populate fields",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = naturalLanguageInput,
                            onValueChange = { naturalLanguageInput = it },
                            placeholder = {
                                Text(
                                    text = "e.g. Gym tomorrow 7pm high priority",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                if (naturalLanguageInput.isNotEmpty()) {
                                    IconButton(onClick = { naturalLanguageInput = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear natural language input")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Live Parsed Preview Chips
                        AnimatedVisibility(
                            visible = parsedResult != null && (parsedResult.hasExtractedAnyField || parsedResult.title.isNotBlank()),
                            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.96f, animationSpec = tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                            exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.96f, animationSpec = tween(120, easing = FastOutSlowInEasing)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Parsed Preview (Review before saving):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (parsedResult?.title?.isNotBlank() == true) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Title: ${parsedResult.title}", style = MaterialTheme.typography.labelSmall) },
                                            icon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    if (parsedResult?.date != null) {
                                        val dateStr = parsedResult.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(dateStr, style = MaterialTheme.typography.labelSmall) },
                                            icon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    if (parsedResult?.time != null) {
                                        val timeStr = parsedResult.time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(timeStr, style = MaterialTheme.typography.labelSmall) },
                                            icon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    if (parsedResult?.priority != null) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("${parsedResult.priority.name} Priority", style = MaterialTheme.typography.labelSmall) },
                                            icon = { Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    if (parsedResult?.estimatedDurationMinutes != null) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("${parsedResult.estimatedDurationMinutes} min", style = MaterialTheme.typography.labelSmall) },
                                            icon = { Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    if (parsedResult?.repeatType != null) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(parsedResult.repeatDescription ?: parsedResult.repeatType.name, style = MaterialTheme.typography.labelSmall) },
                                            icon = { Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { naturalLanguageInput = "" }) {
                                        Text("Clear")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val res = parsedResult
                                            if (res != null) {
                                                if (res.title.isNotBlank()) {
                                                    title = res.title
                                                    isTitleError = false
                                                }
                                                res.dueDateMillis?.let { dueDateMillis = it }
                                                res.priority?.let { priority = it }
                                                res.repeatType?.let { repeatType = it }
                                                res.customRepeatDays?.let { customDaysInput = it.toString() }
                                                res.estimatedDurationMinutes?.let { estimatedDurationInput = it.toString() }
                                            }
                                            naturalLanguageInput = ""
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apply to Form")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        if (it.isNotBlank()) isTitleError = false 
                    },
                    label = { Text("Title *") },
                    isError = isTitleError,
                    supportingText = {
                        AnimatedVisibility(
                            visible = isTitleError,
                            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(180, easing = FastOutSlowInEasing)),
                            exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + shrinkVertically(tween(140, easing = FastOutSlowInEasing))
                        ) {
                            Text("Title is required", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Inline Parse Suggestion for Title input
                AnimatedVisibility(
                    visible = titleParsedResult != null && titleParsedResult.hasExtractedAnyField,
                    enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) + shrinkVertically(tween(140, easing = FastOutSlowInEasing))
                ) {
                    val summaryParts = mutableListOf<String>()
                    titleParsedResult?.date?.let { summaryParts.add(it.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))) }
                    titleParsedResult?.time?.let { summaryParts.add(it.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))) }
                    titleParsedResult?.priority?.let { summaryParts.add("${it.name} Priority") }
                    titleParsedResult?.estimatedDurationMinutes?.let { summaryParts.add("${it}m") }
                    titleParsedResult?.repeatType?.let { summaryParts.add(titleParsedResult.repeatDescription ?: it.name) }

                    SuggestionChip(
                        onClick = {
                            val res = titleParsedResult
                            if (res != null) {
                                title = res.title
                                res.dueDateMillis?.let { dueDateMillis = it }
                                res.priority?.let { priority = it }
                                res.repeatType?.let { repeatType = it }
                                res.customRepeatDays?.let { customDaysInput = it.toString() }
                                res.estimatedDurationMinutes?.let { estimatedDurationInput = it.toString() }
                            }
                        },
                        label = {
                            Text(
                                text = "✨ Parse: ${summaryParts.joinToString(" • ")} (Tap to fill fields)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
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

            // Estimated Duration (Optional)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = estimatedDurationInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            estimatedDurationInput = input
                        }
                    },
                    label = { Text("Estimated Duration (optional)") },
                    placeholder = { Text("e.g. 30") },
                    trailingIcon = {
                        if (estimatedDurationInput.isNotEmpty()) {
                            IconButton(onClick = { estimatedDurationInput = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear duration")
                            }
                        } else {
                            Text(
                                text = "min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        val isSelected = estimatedDurationInput == mins.toString()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                estimatedDurationInput = if (isSelected) "" else mins.toString()
                            },
                            label = { Text("${mins}m") }
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
                            AnimatedContent(
                                targetState = dateStr,
                                transitionSpec = { fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(180, easing = FastOutSlowInEasing)) },
                                label = "dateStrText"
                            ) { text ->
                                Text(text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        AnimatedVisibility(
                            visible = dueDateMillis != null,
                            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.96f, animationSpec = tween(180, easing = FastOutSlowInEasing)),
                            exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.96f, animationSpec = tween(120, easing = FastOutSlowInEasing)),
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueDateMillis ?: System.currentTimeMillis()))
                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                AnimatedContent(
                                    targetState = timeStr,
                                    transitionSpec = { fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(180, easing = FastOutSlowInEasing)) },
                                    label = "timeStrText"
                                ) { text ->
                                    Text(text, style = MaterialTheme.typography.bodyMedium)
                                }
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

                    AnimatedVisibility(
                        visible = isReminderEnabled,
                        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
                    ) {
                        Column {
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
            AnimatedVisibility(
                visible = repeatType == RepeatType.Custom,
                enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
            ) {
                Column {
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
                    modifier = Modifier.pressScale(0.98f),
                    enabled = !isSaving,
                    onClick = {
                        if (title.isBlank()) {
                            isTitleError = true
                        } else {
                            if (!isSaving) {
                                 val durationMinutes = estimatedDurationInput.toIntOrNull()?.takeIf { it > 0 }
                                 val newTask = if (taskToEdit != null) {
                                    taskToEdit!!.copy(
                                        title = title.trim(),
                                        description = description.trim(),
                                        category = category,
                                        groupId = selectedGroupId,
                                        priority = priority,
                                        estimatedDurationMinutes = durationMinutes,
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
                                        estimatedDurationMinutes = durationMinutes,
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
