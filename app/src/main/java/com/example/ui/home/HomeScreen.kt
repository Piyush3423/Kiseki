package com.example.ui.home

import com.example.ui.components.KisekiLogoBadge
import com.example.ui.components.MonarchLogo
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.entity.TaskGroup
import com.example.ui.taskgroup.parseGroupColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ActivityTask
import com.example.data.entity.Category
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import com.example.ui.category.CategoryDotIndicator
import com.example.ui.category.ManageCategoriesDialog
import com.example.ui.theme.NavBarBg
import com.example.viewmodel.ActivityTaskViewModel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.data.repository.ThemeMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Add

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityTaskViewModel,
    showCompletedOnToday: Boolean = true,
    startWeekOnMonday: Boolean = true,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onAddTaskClick: () -> Unit = {},
    onEditTaskClick: (String) -> Unit = {},
    onTaskClick: (String) -> Unit = {},
    onNavigateToTemplates: () -> Unit = {}
) {
    val isShadowMonarch = themeMode == ThemeMode.SHADOW_MONARCH

    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val dbCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val dbGroups by viewModel.allGroups.collectAsStateWithLifecycle()

    var isSearchActive by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    var selectedCategory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All") }
    var selectedPriority by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All") }
    var selectedStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All") }
    var selectedSort by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(SortOption.DEFAULT) }
    var showFilterBottomSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showManageCategoriesDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val today = androidx.compose.runtime.remember { LocalDate.now() }
    var selectedDate by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(today) }
    var weekOffset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val startOfWeekDay = if (startWeekOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val currentWeekStart = androidx.compose.runtime.remember(today, weekOffset, startWeekOnMonday) {
        today.plusWeeks(weekOffset.toLong()).with(startOfWeekDay)
    }

    val weekDays = androidx.compose.runtime.remember(currentWeekStart) {
        (0..6).map { currentWeekStart.plusDays(it.toLong()) }
    }

    val weekRangeText = androidx.compose.runtime.remember(weekDays) {
        val start = weekDays.first()
        val end = weekDays.last()
        val startFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        if (start.month == end.month) {
            val endFormatter = DateTimeFormatter.ofPattern("d, yyyy", Locale.getDefault())
            "${start.format(startFormatter)} - ${end.format(endFormatter)}"
        } else {
            val endFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            "${start.format(startFormatter)} - ${end.format(endFormatter)}"
        }
    }

    val categories = androidx.compose.runtime.remember(tasks) {
        val distinctCategories = tasks.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        val defaultCategories = listOf("Work", "Personal", "Health", "Study")
        val combined = (defaultCategories + distinctCategories).distinct().sorted()
        listOf("All") + combined
    }

    val hasActiveFilters = selectedCategory != "All" || selectedPriority != "All" || selectedStatus != "All" || selectedSort != SortOption.DEFAULT

    val trimmedQuery = searchQuery.trim()
    val priorityWeight = mapOf(Priority.High to 3, Priority.Medium to 2, Priority.Low to 1)

    val (datedTasks, noDateTasks) = androidx.compose.runtime.remember(
        tasks,
        trimmedQuery,
        selectedCategory,
        selectedPriority,
        selectedStatus,
        selectedSort,
        selectedDate,
        today
    ) {
        val zoneId = ZoneId.systemDefault()
        val searchAndFilterMatching = tasks.filter { task ->
            val matchesSearch = if (trimmedQuery.isEmpty()) true else {
                task.title.contains(trimmedQuery, ignoreCase = true) ||
                (task.description?.contains(trimmedQuery, ignoreCase = true) == true) ||
                task.category.contains(trimmedQuery, ignoreCase = true)
            }
            val matchesCategory = if (selectedCategory == "All") true else {
                task.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesPriority = if (selectedPriority == "All") true else {
                task.priority.name.equals(selectedPriority, ignoreCase = true)
            }
            val matchesStatus = when (selectedStatus) {
                "Incomplete" -> !task.isCompleted
                "Completed" -> task.isCompleted
                else -> if (!showCompletedOnToday) !task.isCompleted else true
            }

            matchesSearch && matchesCategory && matchesPriority && matchesStatus
        }

        val taskComparator = Comparator<ActivityTask> { task1, task2 ->
            if (task1.isCompleted != task2.isCompleted) {
                task1.isCompleted.compareTo(task2.isCompleted)
            } else {
                when (selectedSort) {
                    SortOption.DEFAULT, SortOption.CREATED_DATE_DESC -> task2.createdAt.compareTo(task1.createdAt)
                    SortOption.CREATED_DATE_ASC -> task1.createdAt.compareTo(task2.createdAt)
                    SortOption.DUE_DATE_ASC -> (task1.dueDate ?: Long.MAX_VALUE).compareTo(task2.dueDate ?: Long.MAX_VALUE)
                    SortOption.DUE_DATE_DESC -> (task2.dueDate ?: 0L).compareTo(task1.dueDate ?: 0L)
                    SortOption.PRIORITY_HIGH_LOW -> (priorityWeight[task2.priority] ?: 0).compareTo(priorityWeight[task1.priority] ?: 0)
                    SortOption.PRIORITY_LOW_HIGH -> (priorityWeight[task1.priority] ?: 0).compareTo(priorityWeight[task2.priority] ?: 0)
                    SortOption.ALPHABETICAL_ASC -> task1.title.lowercase().compareTo(task2.title.lowercase())
                }
            }
        }

        val dated = searchAndFilterMatching.filter { task ->
            if (task.dueDate == null) false
            else {
                val taskDate = Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate()
                taskDate == selectedDate
            }
        }.sortedWith(taskComparator)

        val noDate = if (selectedDate == today) {
            searchAndFilterMatching.filter { task -> task.dueDate == null }.sortedWith(taskComparator)
        } else {
            emptyList()
        }

        Pair(dated, noDate)
    }

    val filteredTasks = datedTasks + noDateTasks

    // Monarch Mode Stats
    val totalSelectedDateTasks = datedTasks.size + noDateTasks.size
    val completedSelectedDateTasks = datedTasks.count { it.isCompleted } + noDateTasks.count { it.isCompleted }
    val completionPercentage = if (totalSelectedDateTasks > 0) {
        (completedSelectedDateTasks * 100) / totalSelectedDateTasks
    } else 0

    val currentStreak = androidx.compose.runtime.remember(tasks) {
        val zoneId = ZoneId.systemDefault()
        val completedDates = tasks
            .filter { it.isCompleted && it.dueDate != null }
            .map { Instant.ofEpochMilli(it.dueDate!!).atZone(zoneId).toLocalDate() }
            .toSet()

        var streak = 0
        var curr = LocalDate.now()
        if (!completedDates.contains(curr)) {
            curr = curr.minusDays(1)
        }
        while (completedDates.contains(curr)) {
            streak++
            curr = curr.minusDays(1)
        }
        streak
    }

    val rankLabel = when {
        completionPercentage >= 100 -> "S"
        completionPercentage >= 80 -> "A"
        completionPercentage >= 60 -> "B"
        completionPercentage >= 40 -> "C"
        completionPercentage >= 20 -> "D"
        else -> "E"
    }

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (isShadowMonarch) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2E1065), Color(0xFF0F172A))
                            )
                        )
                        .border(
                            BorderStroke(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFA855F7), Color(0xFF00E5FF))
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onAddTaskClick() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Task",
                        tint = Color(0xFF00E5FF)
                    )
                }
            } else {
                androidx.compose.material3.FloatingActionButton(
                    onClick = onAddTaskClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            if (isShadowMonarch) {
                MonarchStatusHeader(
                    selectedDate = selectedDate,
                    completionPercentage = completionPercentage,
                    completedCount = completedSelectedDateTasks,
                    totalCount = totalSelectedDateTasks,
                    streak = currentStreak,
                    rank = rankLabel,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchClick = { isSearchActive = true },
                    onFilterClick = { showFilterBottomSheet = true },
                    onCloseSearch = {
                        searchQuery = ""
                        isSearchActive = false
                    },
                    hasActiveFilters = hasActiveFilters
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    if (!isSearchActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KisekiLogoBadge(
                                    badgeSize = 44.dp,
                                    logoSize = 28.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Kiseki",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Shape your day. Build your story.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search tasks",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { showFilterBottomSheet = true }) {
                                    if (hasActiveFilters) {
                                        BadgedBox(badge = { Badge() }) {
                                            Icon(
                                                imageVector = Icons.Rounded.FilterList,
                                                contentDescription = "Filter and sort tasks",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.FilterList,
                                            contentDescription = "Filter and sort tasks",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search tasks...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showFilterBottomSheet = true }) {
                                        if (hasActiveFilters) {
                                            BadgedBox(badge = { Badge() }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.FilterList,
                                                    contentDescription = "Filter and sort tasks",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.FilterList,
                                                contentDescription = "Filter and sort tasks",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            isSearchActive = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Active Filter Chips Row
            if (hasActiveFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategory != "All") {
                        FilterChip(
                            selected = true,
                            onClick = { selectedCategory = "All" },
                            label = { Text("Category: $selectedCategory") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove category filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    if (selectedPriority != "All") {
                        FilterChip(
                            selected = true,
                            onClick = { selectedPriority = "All" },
                            label = { Text("Priority: $selectedPriority") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove priority filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    if (selectedStatus != "All") {
                        FilterChip(
                            selected = true,
                            onClick = { selectedStatus = "All" },
                            label = { Text("Status: $selectedStatus") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove status filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    if (selectedSort != SortOption.DEFAULT) {
                        FilterChip(
                            selected = true,
                            onClick = { selectedSort = SortOption.DEFAULT },
                            label = { Text("Sort: ${selectedSort.displayName}") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Reset sort",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    TextButton(
                        onClick = {
                            selectedCategory = "All"
                            selectedPriority = "All"
                            selectedStatus = "All"
                            selectedSort = SortOption.DEFAULT
                        }
                    ) {
                        Text("Clear All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            WeeklyDateSelector(
                selectedDate = selectedDate,
                today = today,
                weekDays = weekDays,
                weekRangeText = weekRangeText,
                themeMode = themeMode,
                onDateSelected = { selectedDate = it },
                onPreviousWeek = {
                    weekOffset--
                    val newMonday = today.plusWeeks(weekOffset.toLong()).with(DayOfWeek.MONDAY)
                    selectedDate = newMonday
                },
                onNextWeek = {
                    weekOffset++
                    val newMonday = today.plusWeeks(weekOffset.toLong()).with(DayOfWeek.MONDAY)
                    selectedDate = newMonday
                },
                onTodayClick = {
                    weekOffset = 0
                    selectedDate = today
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content
            if (filteredTasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tasks for this day",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val emptyMessage = when {
                        trimmedQuery.isNotEmpty() && hasActiveFilters -> "No tasks match \"$trimmedQuery\" with active filters."
                        trimmedQuery.isNotEmpty() -> "No tasks found matching \"$trimmedQuery\""
                        hasActiveFilters -> "No tasks match active filters."
                        else -> "No tasks scheduled for ${if (selectedDate == today) "today" else selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))}."
                    }
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (hasActiveFilters || trimmedQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "All"
                                selectedPriority = "All"
                                selectedStatus = "All"
                                selectedSort = SortOption.DEFAULT
                            }
                        ) {
                            Text("Clear filters & search")
                        }
                    }
                }
            } else {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                var previousTasks by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(filteredTasks) }

                androidx.compose.runtime.SideEffect {
                    if (previousTasks != filteredTasks) {
                        listState.requestScrollToItem(
                            listState.firstVisibleItemIndex,
                            listState.firstVisibleItemScrollOffset
                        )
                        previousTasks = filteredTasks
                    }
                }

                var taskToDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ActivityTask?>(null) }
                
                if (taskToDelete != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { taskToDelete = null },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = {
                            Text(
                                text = "Delete Task?",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to permanently delete '${taskToDelete?.title}'?\nThis action cannot be undone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    taskToDelete?.let { viewModel.deleteTask(it) }
                                    taskToDelete = null
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { taskToDelete = null }
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (datedTasks.isNotEmpty()) {
                        items(datedTasks, key = { it.id }) { task ->
                            TaskItemCard(
                                task = task,
                                categories = dbCategories,
                                groups = dbGroups,
                                onToggleComplete = { viewModel.updateTask(it.copy(isCompleted = !it.isCompleted)) },
                                onDelete = { taskToDelete = it },
                                onEdit = { onEditTaskClick(it.id) },
                                onClick = { onTaskClick(task.id) },
                                modifier = Modifier.animateItem(),
                                themeMode = themeMode
                            )
                        }
                    }

                    if (noDateTasks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "No due date",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${noDateTasks.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        items(noDateTasks, key = { it.id }) { task ->
                            TaskItemCard(
                                task = task,
                                categories = dbCategories,
                                groups = dbGroups,
                                onToggleComplete = { viewModel.updateTask(it.copy(isCompleted = !it.isCompleted)) },
                                onDelete = { taskToDelete = it },
                                onEdit = { onEditTaskClick(it.id) },
                                onClick = { onTaskClick(task.id) },
                                modifier = Modifier.animateItem(),
                                themeMode = themeMode
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        if (showFilterBottomSheet) {
            FilterSortBottomSheet(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                selectedPriority = selectedPriority,
                onSelectPriority = { selectedPriority = it },
                selectedStatus = selectedStatus,
                onSelectStatus = { selectedStatus = it },
                selectedSort = selectedSort,
                onSelectSort = { selectedSort = it },
                categories = dbCategories,
                onManageCategories = { showManageCategoriesDialog = true },
                onClearAll = {
                    selectedCategory = "All"
                    selectedPriority = "All"
                    selectedStatus = "All"
                    selectedSort = SortOption.DEFAULT
                },
                onDismissRequest = { showFilterBottomSheet = false }
            )
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
    }
}

enum class SortOption(val displayName: String) {
    DEFAULT("Default"),
    DUE_DATE_ASC("Due Date (Earliest First)"),
    DUE_DATE_DESC("Due Date (Latest First)"),
    CREATED_DATE_DESC("Created Date (Newest First)"),
    CREATED_DATE_ASC("Created Date (Oldest First)"),
    PRIORITY_HIGH_LOW("Priority (High → Low)"),
    PRIORITY_LOW_HIGH("Priority (Low → High)"),
    ALPHABETICAL_ASC("Alphabetical (A → Z)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortBottomSheet(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    selectedPriority: String,
    onSelectPriority: (String) -> Unit,
    selectedStatus: String,
    onSelectStatus: (String) -> Unit,
    selectedSort: SortOption,
    onSelectSort: (SortOption) -> Unit,
    categories: List<Category>,
    onManageCategories: () -> Unit = {},
    onClearAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter & Sort Tasks",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }

            // Sort By Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortOption.values().forEach { option ->
                        FilterChip(
                            selected = selectedSort == option,
                            onClick = { onSelectSort(option) },
                            label = { Text(option.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Category Filter Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onManageCategories) {
                        Text("Manage Categories", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "All",
                        onClick = { onSelectCategory("All") },
                        label = { Text("All Categories") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat.name, ignoreCase = true),
                            onClick = { onSelectCategory(cat.name) },
                            leadingIcon = { CategoryDotIndicator(colorHex = cat.colorHex) },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Priority Filter Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "High", "Medium", "Low").forEach { prio ->
                        FilterChip(
                            selected = selectedPriority.equals(prio, ignoreCase = true),
                            onClick = { onSelectPriority(prio) },
                            label = { Text(if (prio == "All") "All Priorities" else prio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Status Filter Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Completion Status",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Incomplete", "Completed").forEach { status ->
                        FilterChip(
                            selected = selectedStatus.equals(status, ignoreCase = true),
                            onClick = { onSelectStatus(status) },
                            label = { Text(if (status == "All") "All Statuses" else status) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            androidx.compose.material3.Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Apply & Close")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TaskItemCard(
    task: ActivityTask,
    categories: List<Category> = emptyList(),
    groups: List<TaskGroup> = emptyList(),
    onToggleComplete: (ActivityTask) -> Unit,
    onDelete: (ActivityTask) -> Unit,
    onEdit: (ActivityTask) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onRemoveFromGroup: (() -> Unit)? = null
) {
    val isShadowMonarch = themeMode == ThemeMode.SHADOW_MONARCH
    val alpha = if (task.isCompleted) (if (isShadowMonarch) 0.7f else 0.6f) else 1f
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (task.isCompleted && isShadowMonarch) 1.01f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "LevelUpPulse"
    )

    val cardBgColor = when {
        isShadowMonarch && task.isCompleted -> Color(0xFF0B0D18)
        isShadowMonarch -> Color(0xFF121528)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderStroke = when {
        isShadowMonarch && task.isCompleted -> BorderStroke(1.dp, Color(0xFF1B2035))
        isShadowMonarch && task.priority == Priority.High -> BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFF8B5CF6))))
        isShadowMonarch -> BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF6B21A8).copy(alpha = 0.5f), Color(0xFF1E3A8A).copy(alpha = 0.5f))))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .clickable { onClick() }
            .border(borderStroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .alpha(alpha)
    ) {
        Column {
            if (isShadowMonarch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val questBadgeBg = when {
                        task.isCompleted -> Color(0xFF064E3B)
                        task.priority == Priority.High -> Color(0xFF4C0519)
                        task.priority == Priority.Medium -> Color(0xFF2E1065)
                        else -> Color(0xFF0F172A)
                    }
                    val questBadgeText = when {
                        task.isCompleted -> "QUEST CLEARED"
                        task.priority == Priority.High -> "CRITICAL QUEST"
                        task.priority == Priority.Medium -> "ACTIVE QUEST"
                        else -> "QUEST"
                    }
                    val questBadgeColor = when {
                        task.isCompleted -> Color(0xFF34D399)
                        task.priority == Priority.High -> Color(0xFFF43F5E)
                        task.priority == Priority.Medium -> Color(0xFFA855F7)
                        else -> Color(0xFF00E5FF)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(questBadgeBg)
                            .border(BorderStroke(0.8.dp, questBadgeColor.copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = questBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = questBadgeColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleComplete(task) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = when {
                            task.isCompleted && isShadowMonarch -> Color(0xFF64748B)
                            task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            isShadowMonarch -> Color(0xFFF1F5F9)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Priority Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val priorityColor = when (task.priority) {
                        Priority.High -> if (isShadowMonarch) Color(0xFFF43F5E) else Color(0xFFB3261E)
                        Priority.Medium -> if (isShadowMonarch) Color(0xFFA855F7) else Color(0xFFF29900)
                        Priority.Low -> if (isShadowMonarch) Color(0xFF00E5FF) else Color(0xFF1D9BF0)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = priorityColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    IconButton(
                        onClick = { onEdit(task) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Task",
                            tint = if (isShadowMonarch) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    
                    IconButton(
                        onClick = { onDelete(task) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete Task",
                            tint = if (isShadowMonarch) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.category.isNotBlank()) {
                    val catObj = categories.find { it.name.equals(task.category, ignoreCase = true) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CategoryDotIndicator(colorHex = catObj?.colorHex)
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isShadowMonarch) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!task.groupId.isNullOrBlank()) {
                    val groupObj = groups.find { it.id == task.groupId }
                    if (groupObj != null) {
                        val groupColor = parseGroupColor(groupObj.color)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(groupColor)
                            )
                            Text(
                                text = groupObj.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = groupColor
                            )
                            if (onRemoveFromGroup != null) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove from group",
                                    tint = if (isShadowMonarch) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveFromGroup() }
                                )
                            }
                        }
                    }
                }
                
                if (task.dueDate != null) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isShadowMonarch) Color(0xFF475569) else MaterialTheme.colorScheme.outline
                    )
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(task.dueDate))
                    Text(
                        text = "Due: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isShadowMonarch) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (task.repeatType != RepeatType.None) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isShadowMonarch) Color(0xFF475569) else MaterialTheme.colorScheme.outline
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isShadowMonarch) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "Repeats: ${task.repeatType.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isShadowMonarch) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyDateSelector(
    selectedDate: LocalDate,
    today: LocalDate,
    weekDays: List<LocalDate>,
    weekRangeText: String,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val isShadowMonarch = themeMode == ThemeMode.SHADOW_MONARCH

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = weekRangeText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isShadowMonarch) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.onSurface
                )
                if (selectedDate != today) {
                    TextButton(
                        onClick = onTodayClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isShadowMonarch) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPreviousWeek,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous Week",
                        tint = if (isShadowMonarch) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onNextWeek,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next Week",
                        tint = if (isShadowMonarch) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val dayFormatter = androidx.compose.runtime.remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
            weekDays.forEach { date ->
                val isSelected = (date == selectedDate)
                val isTodayDate = (date == today)

                val backgroundColor = when {
                    isSelected -> if (isShadowMonarch) Color.Unspecified else MaterialTheme.colorScheme.primary
                    isTodayDate -> if (isShadowMonarch) Color(0xFF13172A) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> if (isShadowMonarch) Color(0xFF0E101A) else MaterialTheme.colorScheme.surface
                }

                val contentColor = when {
                    isSelected -> if (isShadowMonarch) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onPrimary
                    isTodayDate -> if (isShadowMonarch) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary
                    else -> if (isShadowMonarch) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurface
                }

                val borderStroke = when {
                    isSelected -> if (isShadowMonarch) BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF00E5FF)))) else null
                    isTodayDate -> if (isShadowMonarch) BorderStroke(1.2.dp, Color(0xFF3B82F6)) else BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else -> if (isShadowMonarch) BorderStroke(1.dp, Color(0xFF1E243B)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isSelected && isShadowMonarch) {
                                Modifier.background(
                                    Brush.linearGradient(listOf(Color(0xFF2D1252), Color(0xFF0F172A)))
                                )
                            } else {
                                Modifier.background(backgroundColor)
                            }
                        )
                        .then(
                            if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = date.format(dayFormatter).take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || isTodayDate) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected || isTodayDate) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = contentColor
                        )
                        if (isTodayDate && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        color = if (isShadowMonarch) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonarchStatusHeader(
    selectedDate: LocalDate,
    completionPercentage: Int,
    completedCount: Int,
    totalCount: Int,
    streak: Int,
    rank: String,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onCloseSearch: () -> Unit,
    hasActiveFilters: Boolean
) {
    val dateString = androidx.compose.runtime.remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isSearchActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonarchLogo(
                        size = 38.dp,
                        showAura = true,
                        isSelected = true
                    )
                    Column {
                        Text(
                            text = "KISEKI MONARCH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search tasks",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    IconButton(onClick = onFilterClick) {
                        if (hasActiveFilters) {
                            BadgedBox(badge = { Badge(containerColor = Color(0xFFA855F7)) }) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = "Filter and sort tasks",
                                    tint = Color(0xFF00E5FF)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Filter and sort tasks",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Compact Monarch Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF131628), Color(0xFF0D0F1B))
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                    Color(0xFF00E5FF).copy(alpha = 0.6f)
                                )
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2E1065),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(
                                    1.2.dp,
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFA855F7), Color(0xFF00E5FF))
                                    )
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RANK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = rank,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    // Stats & Progress
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUEST CLEARANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "$completionPercentage%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color(0xFF00E5FF)
                            )
                        }

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E243A))
                        ) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = completionPercentage / 100f,
                                animationSpec = tween(500),
                                label = "progress"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF8B5CF6), Color(0xFF00E5FF))
                                        )
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$completedCount / $totalCount Quests",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "$streak Days Streak",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFA855F7)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search quests...", color = Color(0xFF64748B)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                        IconButton(onClick = onFilterClick) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Filter",
                                tint = if (hasActiveFilters) Color(0xFFA855F7) else Color(0xFF94A3B8)
                            )
                        }
                        IconButton(onClick = onCloseSearch) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close search",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF121528),
                    unfocusedContainerColor = Color(0xFF121528),
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF3B2D5A),
                    focusedTextColor = Color(0xFFF1F5F9),
                    unfocusedTextColor = Color(0xFFF1F5F9)
                )
            )
        }
    }
}
