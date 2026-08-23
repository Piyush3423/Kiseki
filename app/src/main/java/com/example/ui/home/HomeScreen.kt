package com.example.ui.home

import kotlinx.coroutines.launch
import com.example.ui.components.KisekiHeaderLogo
import com.example.ui.components.KisekiLogoBadge
import com.example.ui.components.KisekiTaskCheckbox
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.pressScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.rounded.LocalFireDepartment
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
import com.example.ui.category.parseCategoryColor
import com.example.ui.theme.NavBarBg
import com.example.ui.theme.*
import com.example.viewmodel.ActivityTaskViewModel

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.data.repository.ThemeMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Add

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    val isTasksLoaded by viewModel.isTasksLoaded.collectAsStateWithLifecycle()
    val newlyCreatedTaskId by viewModel.newlyCreatedTaskId.collectAsStateWithLifecycle()
    val allDailyScores by viewModel.allDailyScores.collectAsStateWithLifecycle()
    val allXpEvents by viewModel.allXpEvents.collectAsStateWithLifecycle()
    val dbCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val dbGroups by viewModel.allGroups.collectAsStateWithLifecycle()
    val levelInfo by viewModel.levelInfo.collectAsStateWithLifecycle()
    val momentumInfo by viewModel.momentumInfo.collectAsStateWithLifecycle()
    val xpToastAmount by viewModel.xpToastAmount.collectAsStateWithLifecycle()
    val highFrictionTasks by viewModel.highFrictionTasks.collectAsStateWithLifecycle()
    val topFrictionTask = androidx.compose.runtime.remember(highFrictionTasks) { highFrictionTasks.firstOrNull() }
    val allEndOfDayReviews by viewModel.allEndOfDayReviews.collectAsStateWithLifecycle()

    var showEndOfDayReviewSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showTomorrowReviewSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var breakSubtasksTask by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ActivityTask?>(null) }
    var rescheduleFrictionTask by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ActivityTask?>(null) }

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

    val tomorrowDate = androidx.compose.runtime.remember(selectedDate) { selectedDate.plusDays(1) }
    val tomorrowSummary = androidx.compose.runtime.remember(tasks, selectedDate) {
        com.example.domain.TomorrowWorkloadCalculator.calculate(tasks, baseDate = selectedDate)
    }

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

    val (datedTasks, noDateTasks, unfinishedPastTasks) = androidx.compose.runtime.remember(
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

        val unfinishedPast = tasks.filter { task ->
            !task.isCompleted && task.dueDate != null && Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate().isBefore(today)
        }

        val noDate = if (selectedDate == today) {
            searchAndFilterMatching.filter { task -> task.dueDate == null }.sortedWith(taskComparator)
        } else {
            emptyList()
        }

        Triple(dated, noDate, unfinishedPast)
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

    Box(modifier = Modifier.fillMaxSize()) {
    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {}
    ) { innerPadding ->
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
                    val view = androidx.compose.ui.platform.LocalView.current
                    androidx.compose.material3.Button(
                        onClick = {
                            com.example.util.KisekiHaptics.performDeleteConfirm(view)
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

        if (showEndOfDayReviewSheet) {
            val reviewSummary = remember(selectedDate, tasks, allDailyScores, allXpEvents) {
                viewModel.getDaySummaryForReview(selectedDate.toString())
            }
            val existingReview = allEndOfDayReviews.find { it.date == selectedDate.toString() }
            com.example.ui.components.EndOfDayReviewSheet(
                summary = reviewSummary,
                existingReview = existingReview,
                onSaveReview = { review ->
                    viewModel.saveEndOfDayReview(review)
                },
                onDeleteReview = {
                    viewModel.deleteEndOfDayReviewForDate(selectedDate.toString())
                },
                onDismiss = {
                    showEndOfDayReviewSheet = false
                }
            )
        }

        breakSubtasksTask?.let { task ->
            com.example.ui.components.BreakIntoSubtasksDialog(
                taskTitle = task.title,
                onDismiss = { breakSubtasksTask = null },
                onConfirm = { subtasks ->
                    viewModel.breakTaskIntoSubtasks(task, subtasks)
                    breakSubtasksTask = null
                }
            )
        }

        rescheduleFrictionTask?.let { task ->
            com.example.ui.components.RescheduleFrictionTaskDialog(
                taskTitle = task.title,
                currentDueDate = task.dueDate,
                onDismiss = { rescheduleFrictionTask = null },
                onConfirm = { newDueDate ->
                    viewModel.rescheduleFrictionTask(task, newDueDate)
                    rescheduleFrictionTask = null
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER
            item {
                if (isShadowMonarch) {
                    MonarchHomeHeader(
                        selectedDate = selectedDate,
                        streak = currentStreak,
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
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)
                    ) {
                        AnimatedContent(
                            targetState = isSearchActive,
                            transitionSpec = {
                                (fadeIn(animationSpec = MotionTokens.standardTween()) + expandVertically()) togetherWith
                                        (fadeOut(animationSpec = MotionTokens.standardTween()) + shrinkVertically())
                            },
                            label = "SearchHeaderTransition"
                        ) { searchActive ->
                            if (!searchActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        KisekiHeaderLogo()
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Text(
                                                text = "Kiseki",
                                                style = MaterialTheme.typography.titleLarge.copy(
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                                    ) {
                                        IconButton(
                                            onClick = { isSearchActive = true },
                                            modifier = Modifier.pressScale(0.9f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = "Search tasks",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { showFilterBottomSheet = true },
                                            modifier = Modifier.pressScale(0.9f)
                                        ) {
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
                                                IconButton(
                                                    onClick = { searchQuery = "" },
                                                    modifier = Modifier.pressScale(0.9f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Clear,
                                                        contentDescription = "Clear search",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { showFilterBottomSheet = true },
                                                modifier = Modifier.pressScale(0.9f)
                                            ) {
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
                                                },
                                                modifier = Modifier.pressScale(0.9f)
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
                }
            }

            // 2. COMPACT LEVEL / XP STRIP
            item {
                HomeLevelIndicator(
                    levelInfo = levelInfo,
                    momentumInfo = momentumInfo,
                    isShadowMonarch = isShadowMonarch
                )
            }

            // 3. WEEK SELECTOR
            item {
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
            }

            if (hasActiveFilters) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp, vertical = 2.dp),
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
                                    selectedContainerColor = if (isShadowMonarch) MonarchSurfaceSecondary else MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = if (isShadowMonarch) MonarchTextPrimary else MaterialTheme.colorScheme.onPrimaryContainer
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
                                    selectedContainerColor = if (isShadowMonarch) MonarchSurfaceSecondary else MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = if (isShadowMonarch) MonarchTextPrimary else MaterialTheme.colorScheme.onPrimaryContainer
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
                                    selectedContainerColor = if (isShadowMonarch) MonarchSurfaceSecondary else MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = if (isShadowMonarch) MonarchTextPrimary else MaterialTheme.colorScheme.onPrimaryContainer
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
                                    selectedContainerColor = if (isShadowMonarch) MonarchSurfaceSecondary else MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = if (isShadowMonarch) MonarchTextPrimary else MaterialTheme.colorScheme.onPrimaryContainer
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
                            Text("Clear All", color = if (isShadowMonarch) MonarchPrimary else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 4. TODAY SECTION HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (selectedDate == today) "TODAY" else selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isShadowMonarch) Color(0xFFF2F3F7) else MaterialTheme.colorScheme.onBackground
                    )

                    val scoreForDate = allDailyScores.find { it.date == selectedDate.toString() }?.score
                    val statusText = if (scoreForDate != null && scoreForDate > 0) {
                        "Score $scoreForDate • Rank $rankLabel"
                    } else {
                        "$completedSelectedDateTasks / $totalSelectedDateTasks"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (selectedDate == today && unfinishedPastTasks.isNotEmpty()) {
                item {
                    com.example.ui.components.UnfinishedTasksCard(
                        unfinishedTasks = unfinishedPastTasks,
                        onMoveToTomorrow = { task -> viewModel.moveTaskToTomorrow(task) },
                        onKeep = { task -> viewModel.keepTaskAsIs(task) },
                        onReschedule = { task -> rescheduleFrictionTask = task },
                        modifier = Modifier.padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp),
                        isShadowMonarch = isShadowMonarch
                    )
                }
            }

            topFrictionTask?.let { task ->
                item {
                    com.example.ui.components.FrictionSuggestionCard(
                        task = task,
                        onBreakIntoSubtasks = { breakSubtasksTask = task },
                        onReschedule = { rescheduleFrictionTask = task },
                        onLowerPriority = { viewModel.lowerTaskPriority(task) },
                        onKeepAsIs = { viewModel.keepTaskAsIs(task) },
                        modifier = Modifier.padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp),
                        isShadowMonarch = isShadowMonarch
                    )
                }
            }

            // 4. TODAY SECTION CONTENT
            if (filteredTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null,
                            tint = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (hasActiveFilters || trimmedQuery.isNotEmpty()) "No tasks match filters" else "No tasks scheduled",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isShadowMonarch) Color(0xFFF0F0F5) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val emptySubtext = when {
                            trimmedQuery.isNotEmpty() -> "No tasks found matching \"$trimmedQuery\"."
                            hasActiveFilters -> "Try resetting active filters."
                            else -> "Plan something for ${if (selectedDate == today) "today" else "this date"}."
                        }
                        Text(
                            text = emptySubtext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isShadowMonarch) Color(0xFF9E9EAF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasActiveFilters || trimmedQuery.isNotEmpty()) {
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
                        } else {
                            Button(
                                onClick = onAddTaskClick,
                                modifier = Modifier
                                    .height(46.dp)
                                    .pressScale()
                                    .testTag("inline_add_task_button"),
                                shape = RoundedCornerShape(14.dp),
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
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Task",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            } else {
                if (datedTasks.isNotEmpty()) {
                    items(datedTasks, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp)) {
                            TaskItemCard(
                                task = task,
                                categories = dbCategories,
                                groups = dbGroups,
                                onToggleComplete = { viewModel.updateTask(it.copy(isCompleted = !it.isCompleted)) },
                                onDelete = { taskToDelete = it },
                                onEdit = { onEditTaskClick(it.id) },
                                onClick = { onTaskClick(task.id) },
                                isNewlyCreated = (task.id == newlyCreatedTaskId),
                                onEntranceAnimationFinished = { viewModel.clearNewlyCreatedTaskId() },
                                themeMode = themeMode
                            )
                        }
                    }
                }

                if (noDateTasks.isNotEmpty()) {
                    item {
                        val noDateSidePadding = if (isShadowMonarch) 16.dp else 24.dp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = noDateSidePadding, end = noDateSidePadding, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "No due date",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                        Box(modifier = Modifier.padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp)) {
                            TaskItemCard(
                                task = task,
                                categories = dbCategories,
                                groups = dbGroups,
                                onToggleComplete = { viewModel.updateTask(it.copy(isCompleted = !it.isCompleted)) },
                                onDelete = { taskToDelete = it },
                                onEdit = { onEditTaskClick(it.id) },
                                onClick = { onTaskClick(task.id) },
                                isNewlyCreated = (task.id == newlyCreatedTaskId),
                                onEntranceAnimationFinished = { viewModel.clearNewlyCreatedTaskId() },
                                themeMode = themeMode
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = onAddTaskClick,
                            modifier = Modifier
                                .height(42.dp)
                                .pressScale()
                                .testTag("inline_add_task_button_small"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Task",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // 5. TOMORROW PREVIEW
            if (selectedDate == today) {
                item {
                    Box(modifier = Modifier.padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp)) {
                        com.example.ui.components.TomorrowPreviewCard(
                            summary = tomorrowSummary,
                            onReviewTasks = { showTomorrowReviewSheet = true },
                            isShadowMonarch = isShadowMonarch
                        )
                    }
                }
            }

            // 6. END-OF-DAY REVIEW
            if (!selectedDate.isAfter(today)) {
                item {
                    val reviewSummary = remember(selectedDate, tasks, allDailyScores, allXpEvents) {
                        viewModel.getDaySummaryForReview(selectedDate.toString())
                    }
                    val existingReview = allEndOfDayReviews.find { it.date == selectedDate.toString() }
                    com.example.ui.components.EndOfDayReviewCard(
                        summary = reviewSummary,
                        review = existingReview,
                        onOpenReview = { showEndOfDayReviewSheet = true },
                        modifier = Modifier.padding(horizontal = if (isShadowMonarch) 16.dp else 24.dp)
                    )
                }
            }
        }

        if (showTomorrowReviewSheet) {
            com.example.ui.components.TomorrowReviewBottomSheet(
                summary = tomorrowSummary,
                tomorrowDate = tomorrowDate,
                onNavigateToTomorrow = {
                    selectedDate = tomorrowDate
                    val startOfWeek = if (startWeekOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
                    val todayWeekStart = today.with(startOfWeek)
                    val tomorrowWeekStart = tomorrowDate.with(startOfWeek)
                    if (tomorrowWeekStart != todayWeekStart) {
                        weekOffset = 1
                    }
                },
                onDismiss = {
                    showTomorrowReviewSheet = false
                },
                onTaskClick = { taskId ->
                    onTaskClick(taskId)
                },
                isShadowMonarch = isShadowMonarch
            )
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

        XpToastOverlay(
            xpAmount = xpToastAmount,
            onDismiss = { viewModel.clearXpToast() }
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
    onRemoveFromGroup: (() -> Unit)? = null,
    isNewlyCreated: Boolean = false,
    onEntranceAnimationFinished: (() -> Unit)? = null
) {
    val isShadowMonarch = themeMode == ThemeMode.SHADOW_MONARCH
    if (isShadowMonarch) {
        MonarchTaskCard(
            task = task,
            categories = categories,
            groups = groups,
            onToggleComplete = onToggleComplete,
            onDelete = onDelete,
            onEdit = onEdit,
            onClick = onClick,
            modifier = modifier,
            onRemoveFromGroup = onRemoveFromGroup,
            isNewlyCreated = isNewlyCreated,
            onEntranceAnimationFinished = onEntranceAnimationFinished
        )
        return
    }

    val targetAlpha = if (task.isCompleted) 0.7f else 1f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "TaskCardAlpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.70f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "TaskTextAlpha"
    )

    val cardPulseAnim = remember(task.id) { androidx.compose.animation.core.Animatable(1f) }
    var lastCardCompletedState by remember(task.id) { mutableStateOf(task.isCompleted) }

    LaunchedEffect(task.isCompleted) {
        if (task.isCompleted && !lastCardCompletedState) {
            lastCardCompletedState = true
            cardPulseAnim.animateTo(0.99f, tween(durationMillis = 80, easing = FastOutSlowInEasing))
            cardPulseAnim.animateTo(1f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
        } else if (!task.isCompleted && lastCardCompletedState) {
            lastCardCompletedState = false
            cardPulseAnim.snapTo(1f)
        }
    }

    val entranceAlpha = remember(task.id) { androidx.compose.animation.core.Animatable(if (isNewlyCreated) 0f else 1f) }
    val entranceTranslationY = remember(task.id) { androidx.compose.animation.core.Animatable(if (isNewlyCreated) 10f else 0f) }
    val entranceScale = remember(task.id) { androidx.compose.animation.core.Animatable(if (isNewlyCreated) 0.98f else 1f) }

    LaunchedEffect(isNewlyCreated) {
        if (isNewlyCreated) {
            launch {
                entranceAlpha.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            }
            launch {
                entranceTranslationY.animateTo(0f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            }
            entranceScale.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            onEntranceAnimationFinished?.invoke()
        }
    }

    val cardBgColor = when {
        isShadowMonarch -> MonarchSurface
        else -> MaterialTheme.colorScheme.surface
    }

    val borderStroke = when {
        isShadowMonarch -> BorderStroke(1.dp, MonarchBorder)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }

    val cardRadius = if (isShadowMonarch) MonarchRadius.TaskCard else 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(0.985f)
            .graphicsLayer {
                val px10 = 10.dp.toPx()
                alpha = animatedAlpha * entranceAlpha.value
                translationY = (entranceTranslationY.value / 10f) * px10
                scaleX = entranceScale.value * cardPulseAnim.value
                scaleY = entranceScale.value * cardPulseAnim.value
            }
            .clip(RoundedCornerShape(cardRadius))
            .background(cardBgColor)
            .clickable { onClick() }
            .border(borderStroke, RoundedCornerShape(cardRadius))
            .padding(if (isShadowMonarch) 14.dp else 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (isShadowMonarch && task.priority == Priority.High) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MonarchHighPriority)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val view = androidx.compose.ui.platform.LocalView.current
                        KisekiTaskCheckbox(
                            checked = task.isCompleted,
                            onCheckedChange = {
                                if (!task.isCompleted) com.example.util.KisekiHaptics.performTaskComplete(view)
                                else com.example.util.KisekiHaptics.performTaskUncomplete(view)
                                onToggleComplete(task)
                            },
                            activeColor = if (isShadowMonarch) MonarchPrimary else MaterialTheme.colorScheme.primary,
                            inactiveColor = if (isShadowMonarch) MonarchBorder else MaterialTheme.colorScheme.outline,
                            checkmarkColor = if (isShadowMonarch) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isShadowMonarch) FontWeight.Medium else FontWeight.SemiBold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = when {
                                task.isCompleted && isShadowMonarch -> MonarchTextMuted.copy(alpha = 0.70f * textAlpha)
                                task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f * textAlpha)
                                isShadowMonarch -> MonarchTextPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Priority Indicator & Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val priorityColor = when (task.priority) {
                            Priority.High -> if (isShadowMonarch) MonarchHighPriority else Color(0xFFB3261E)
                            Priority.Medium -> if (isShadowMonarch) MonarchMediumPriority else Color(0xFFF29900)
                            Priority.Low -> if (isShadowMonarch) MonarchLowPriority else Color(0xFF1D9BF0)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(if (isShadowMonarch) MonarchRadius.Chip else 8.dp))
                                .background(priorityColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = task.priority.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
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
                                tint = if (isShadowMonarch) MonarchTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))
                        
                        IconButton(
                            onClick = { onDelete(task) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete Task",
                                tint = if (isShadowMonarch) MonarchTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                color = if (isShadowMonarch) MonarchTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
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
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(groupColor)
                                )
                                Text(
                                    text = groupObj.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isShadowMonarch) MonarchTextSecondary else groupColor
                                )
                                if (onRemoveFromGroup != null) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove from group",
                                        tint = if (isShadowMonarch) MonarchTextMuted else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = if (isShadowMonarch) MonarchTextMuted else MaterialTheme.colorScheme.outline
                        )
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(task.dueDate))
                        Text(
                            text = "Due: $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isShadowMonarch) MonarchTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (task.repeatType != RepeatType.None) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isShadowMonarch) MonarchTextMuted else MaterialTheme.colorScheme.outline
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = if (isShadowMonarch) MonarchTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Repeats: ${task.repeatType.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isShadowMonarch) MonarchTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            .padding(horizontal = if (isShadowMonarch) MonarchSpacing.Screen else 24.dp)
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
                    style = if (isShadowMonarch) {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    },
                    color = if (isShadowMonarch) MonarchTextPrimary else MaterialTheme.colorScheme.onSurface
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
                            color = if (isShadowMonarch) MonarchPrimary else MaterialTheme.colorScheme.primary
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
                        tint = if (isShadowMonarch) MonarchPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onNextWeek,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next Week",
                        tint = if (isShadowMonarch) MonarchPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isShadowMonarch) 4.dp else 6.dp)
        ) {
            val dayFormatter = androidx.compose.runtime.remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
            weekDays.forEach { date ->
                val isSelected = (date == selectedDate)
                val isTodayDate = (date == today)

                if (isShadowMonarch) {
                    val targetBgColor = if (isSelected) Color(0xFF292344) else Color(0xFF121620)
                    val targetTextColor = if (isSelected) Color(0xFFF2F3F7) else Color(0xFF737B8E)

                    val animatedBgColor by androidx.compose.animation.animateColorAsState(
                        targetValue = targetBgColor,
                        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "MonarchDayBg"
                    )

                    val animatedTextColor by androidx.compose.animation.animateColorAsState(
                        targetValue = targetTextColor,
                        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "MonarchDayText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .pressScale(0.98f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(animatedBgColor)
                            .then(
                                if (isSelected) Modifier.border(BorderStroke(1.dp, Color(0xFF7967E8)), RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = date.format(dayFormatter).take(3).uppercase(),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = animatedTextColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${date.dayOfMonth}",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = animatedTextColor
                            )
                            if (isTodayDate && !isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            color = Color(0xFF7967E8),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                } else {
                    val targetBgColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isTodayDate -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val targetContentColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isTodayDate -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    val animatedBgColor by androidx.compose.animation.animateColorAsState(
                        targetValue = targetBgColor,
                        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "DayBgColor"
                    )

                    val animatedContentColor by androidx.compose.animation.animateColorAsState(
                        targetValue = targetContentColor,
                        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "DayContentColor"
                    )

                    val borderStroke = when {
                        isSelected -> null
                        isTodayDate -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pressScale(0.98f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(animatedBgColor)
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
                                fontWeight = if (isSelected || isTodayDate) FontWeight.SemiBold else FontWeight.Normal,
                                color = animatedContentColor
                            )
                            Text(
                                text = "${date.dayOfMonth}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected || isTodayDate) FontWeight.Bold else FontWeight.SemiBold,
                                color = animatedContentColor
                            )
                            if (isTodayDate && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
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
}

@Composable
fun MonarchEmblem(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val outerPath = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.5f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.5f)
            close()
        }
        drawPath(
            path = outerPath,
            color = Color(0xFF7967E8),
            style = Stroke(width = 2.dp.toPx())
        )

        val innerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.2f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.8f)
            lineTo(w * 0.2f, h * 0.5f)
            close()
        }
        drawPath(
            path = innerPath,
            color = Color(0xFF5687E8),
            style = Fill
        )

        val centerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.35f, h * 0.5f)
            close()
        }
        drawPath(
            path = centerPath,
            color = Color(0xFFF2F3F7),
            style = Fill
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonarchHomeHeader(
    selectedDate: LocalDate,
    streak: Int,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onCloseSearch: () -> Unit,
    hasActiveFilters: Boolean
) {
    val dateString = androidx.compose.runtime.remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090B10))
            .drawBehind {
                drawLine(
                    color = Color(0xFF202638),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isSearchActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KisekiHeaderLogo(color = Color(0xFFF2F3F7))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = "Kiseki",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFFF2F3F7)
                        )
                        Text(
                            text = "Shape your day. Build your story.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFFA9B0C0)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.pressScale(0.96f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search tasks",
                            tint = Color(0xFFF2F3F7)
                        )
                    }
                    IconButton(
                        onClick = onFilterClick,
                        modifier = Modifier.pressScale(0.96f)
                    ) {
                        if (hasActiveFilters) {
                            BadgedBox(badge = { Badge(containerColor = MonarchPrimary) }) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = "Filter and sort tasks",
                                    tint = MonarchPrimary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Filter and sort tasks",
                                tint = Color(0xFFA9B0C0)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(MonarchRadius.Control))
                            .background(MonarchSurfaceSecondary)
                            .border(
                                BorderStroke(1.dp, MonarchBorderSubtle),
                                RoundedCornerShape(MonarchRadius.Control)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = MonarchPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$streak",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFF2F3F7)
                            )
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tasks...", color = Color(0xFF737B8E)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color(0xFFA9B0C0)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFFA9B0C0)
                                )
                            }
                        }
                        IconButton(onClick = onCloseSearch) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close search",
                                tint = Color(0xFFA9B0C0)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(MonarchRadius.Control),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF121620),
                    unfocusedContainerColor = Color(0xFF121620),
                    focusedBorderColor = MonarchPrimary,
                    unfocusedBorderColor = Color(0xFF283044),
                    focusedTextColor = Color(0xFFF2F3F7),
                    unfocusedTextColor = Color(0xFFF2F3F7)
                )
            )
        }
    }
}

@Composable
fun MonarchDailyProgressPanel(
    completedCount: Int,
    totalCount: Int,
    completionPercentage: Int,
    dailyScore: Int = 0,
    modifier: Modifier = Modifier
) {
    val rank = remember(dailyScore) { com.example.domain.dailyScoreToRank(dailyScore) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121620))
            .border(
                BorderStroke(1.dp, Color(0xFF283044)),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Progress",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFFA9B0C0)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "DAILY RANK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFA9B0C0)
                        )
                        AnimatedContent(
                            targetState = rank,
                            transitionSpec = {
                                (fadeIn(tween(220, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                                        (fadeOut(tween(160, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.96f, animationSpec = tween(160, easing = FastOutSlowInEasing)))
                            },
                            label = "MonarchDailyRankTransition"
                        ) { targetRank ->
                            Text(
                                text = targetRank,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (targetRank == "S") Color(0xFF7967E8) else Color(0xFFF2F3F7)
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = completionPercentage,
                        transitionSpec = {
                            (fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 4 }) togetherWith
                                    (fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 })
                        },
                        label = "MonarchCompletionPercentageTransition"
                    ) { targetPct ->
                        Text(
                            text = "$targetPct%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF7967E8)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF202638))
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = (completionPercentage / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "monarch_daily_progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF7967E8), Color(0xFF5687E8))
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = "$completedCount / $totalCount",
                    transitionSpec = {
                        (fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 4 }) togetherWith
                                (fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 })
                    },
                    label = "MonarchCompletedTaskCountTransition"
                ) { targetText ->
                    Text(
                        text = "$targetText tasks completed",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF737B8E)
                    )
                }
            }
        }
    }
}

@Composable
fun MonarchSectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF7967E8))
            )
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFF2F3F7)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1C2230))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$count",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFF2F3F7)
            )
        }
    }
}

@Composable
fun MonarchCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    KisekiTaskCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        activeColor = Color(0xFF7967E8),
        inactiveColor = Color(0xFF737B8E),
        checkmarkColor = Color.White,
        modifier = modifier
    )
}

@Composable
fun MonarchTaskCard(
    task: ActivityTask,
    categories: List<Category> = emptyList(),
    groups: List<TaskGroup> = emptyList(),
    onToggleComplete: (ActivityTask) -> Unit,
    onDelete: (ActivityTask) -> Unit,
    onEdit: (ActivityTask) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onRemoveFromGroup: (() -> Unit)? = null,
    isNewlyCreated: Boolean = false,
    onEntranceAnimationFinished: (() -> Unit)? = null
) {
    var isPressed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val priorityAccentColor = when (task.priority) {
        Priority.High -> Color(0xFFD96772)
        Priority.Medium -> Color(0xFFD7A953)
        Priority.Low -> Color(0xFF6D879C)
        else -> Color.Transparent
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "MonarchCardScale"
    )

    val targetAlpha = if (task.isCompleted) 0.7f else 1f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "MonarchTaskAlpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.70f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "MonarchTextAlpha"
    )

    val cardPulseAnim = remember(task.id) { androidx.compose.animation.core.Animatable(1f) }
    var lastCardCompletedState by remember(task.id) { mutableStateOf(task.isCompleted) }

    LaunchedEffect(task.isCompleted) {
        if (task.isCompleted && !lastCardCompletedState) {
            lastCardCompletedState = true
            cardPulseAnim.animateTo(0.99f, tween(durationMillis = 80, easing = FastOutSlowInEasing))
            cardPulseAnim.animateTo(1f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
        } else if (!task.isCompleted && lastCardCompletedState) {
            lastCardCompletedState = false
            cardPulseAnim.snapTo(1f)
        }
    }

    val entranceAlpha = remember(task.id) { androidx.compose.animation.core.Animatable(if (isNewlyCreated) 0f else 1f) }
    val entranceTranslationY = remember(task.id) { androidx.compose.animation.core.Animatable(if (isNewlyCreated) 10f else 0f) }
    val entranceScale = remember(task.id) { androidx.compose.animation.core.Animatable(if (isNewlyCreated) 0.98f else 1f) }

    LaunchedEffect(isNewlyCreated) {
        if (isNewlyCreated) {
            launch {
                entranceAlpha.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            }
            launch {
                entranceTranslationY.animateTo(0f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            }
            entranceScale.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            onEntranceAnimationFinished?.invoke()
        }
    }

    val cardBgColor = if (isPressed) Color(0xFF171C28) else Color(0xFF121620)
    val cardBorderColor = if (isPressed) Color(0xFF7967E8) else Color(0xFF283044)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val px10 = 10.dp.toPx()
                scaleX = animatedScale.coerceIn(0.97f, 1f) * entranceScale.value * cardPulseAnim.value
                scaleY = animatedScale.coerceIn(0.97f, 1f) * entranceScale.value * cardPulseAnim.value
                alpha = animatedAlpha * entranceAlpha.value
                translationY = (entranceTranslationY.value / 10f) * px10
            }
            .clip(RoundedCornerShape(14.dp))
            .background(cardBgColor)
            .border(BorderStroke(1.dp, cardBorderColor), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(priorityAccentColor)
            )

            val view = androidx.compose.ui.platform.LocalView.current
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonarchCheckbox(
                    checked = task.isCompleted,
                    onCheckedChange = {
                        if (!task.isCompleted) com.example.util.KisekiHaptics.performTaskComplete(view)
                        else com.example.util.KisekiHaptics.performTaskUncomplete(view)
                        onToggleComplete(task)
                    }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) Color(0xFF737B8E).copy(alpha = 0.7f * textAlpha) else Color(0xFFF2F3F7),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val groupObj = if (!task.groupId.isNullOrBlank()) groups.find { it.id == task.groupId } else null
                    val catObj = if (task.category.isNotBlank()) categories.find { it.name.equals(task.category, ignoreCase = true) } else null
                    val hasGroup = groupObj != null || task.category.isNotBlank()
                    val hasTime = task.dueDate != null || task.reminderTime != null
                    val isRecurring = task.repeatType != RepeatType.None

                    if (hasGroup || hasTime || isRecurring) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (groupObj != null) {
                                val groupColor = parseGroupColor(groupObj.color)
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(groupColor)
                                )
                                Text(
                                    text = groupObj.name,
                                    style = TextStyle(fontSize = 12.sp),
                                    color = if (task.isCompleted) Color(0xFF737B8E) else Color(0xFFA9B0C0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (catObj != null || task.category.isNotBlank()) {
                                val catColor = parseCategoryColor(catObj?.colorHex)
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Text(
                                    text = task.category,
                                    style = TextStyle(fontSize = 12.sp),
                                    color = if (task.isCompleted) Color(0xFF737B8E) else Color(0xFFA9B0C0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (hasGroup && hasTime) {
                                Text(
                                    text = "•",
                                    style = TextStyle(fontSize = 12.sp),
                                    color = Color(0xFF737B8E)
                                )
                            }

                            if (hasTime) {
                                val timeText = if (task.dueDate != null) {
                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(task.dueDate))
                                } else if (task.reminderTime != null) {
                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(task.reminderTime))
                                } else ""

                                if (timeText.isNotBlank()) {
                                    Text(
                                        text = timeText,
                                        style = TextStyle(fontSize = 12.sp),
                                        color = if (task.isCompleted) Color(0xFF737B8E) else Color(0xFFA9B0C0)
                                    )
                                }
                            }

                            if (isRecurring) {
                                if (hasGroup || hasTime) {
                                    Text(
                                        text = "•",
                                        style = TextStyle(fontSize = 12.sp),
                                        color = Color(0xFF737B8E)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = "Recurring",
                                    tint = if (task.isCompleted) Color(0xFF737B8E) else Color(0xFFA9B0C0),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onEdit(task) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Task",
                            tint = Color(0xFFA9B0C0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDelete(task) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete Task",
                            tint = Color(0xFFA9B0C0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonarchEmptyState(
    onAddTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.15f)
                lineTo(w * 0.85f, h * 0.5f)
                lineTo(w * 0.5f, h * 0.85f)
                lineTo(w * 0.15f, h * 0.5f)
                close()
            }
            drawPath(
                path = path,
                color = Color(0xFF283044),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF7967E8),
                radius = 4.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No objectives for this day",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFFF2F3F7),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Add a task when you are ready.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            color = Color(0xFFA9B0C0),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddTaskClick,
            modifier = Modifier.pressScale(0.97f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7967E8),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Add Task",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun HomeMomentumBadge(
    momentumInfo: com.example.domain.MomentumResult,
    isShadowMonarch: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!momentumInfo.hasEnoughHistory) return

    val arrowStr = when (momentumInfo.trend) {
        com.example.domain.MomentumTrend.IMPROVING -> "↑"
        com.example.domain.MomentumTrend.STABLE -> "→"
        com.example.domain.MomentumTrend.DECLINING -> "↓"
    }

    val arrowColor = when (momentumInfo.trend) {
        com.example.domain.MomentumTrend.IMPROVING -> Color(0xFF4CAF50)
        com.example.domain.MomentumTrend.STABLE -> if (isShadowMonarch) Color(0xFFA9B0C0) else MaterialTheme.colorScheme.onSurfaceVariant
        com.example.domain.MomentumTrend.DECLINING -> Color(0xFFE53935)
    }

    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isShadowMonarch) Color(0xFF1E2433) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isShadowMonarch) Color(0xFF283044) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Momentum ${momentumInfo.currentMomentum}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = if (isShadowMonarch) Color(0xFFF2F3F7) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = arrowStr,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = arrowColor
            )
        }
    }
}

@Composable
fun HomeLevelIndicator(
    levelInfo: com.example.domain.LevelInfo,
    momentumInfo: com.example.domain.MomentumResult? = null,
    modifier: Modifier = Modifier,
    isShadowMonarch: Boolean = false
) {
    val animatedXpProgress by animateFloatAsState(
        targetValue = levelInfo.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "HomeLevelProgress"
    )

    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isShadowMonarch) MonarchSurfaceSecondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isShadowMonarch) BorderStroke(1.dp, MonarchBorderSubtle) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary,
                                if (isShadowMonarch) Color(0xFF9182F3) else MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                AnimatedContent(
                    targetState = levelInfo.level,
                    transitionSpec = {
                        (fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 4 }) togetherWith
                                (fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 })
                    },
                    label = "LevelNumberTransition"
                ) { targetLevel ->
                    Text(
                        text = "LV. $targetLevel",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Level Progress",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = if (isShadowMonarch) MonarchTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedContent(
                        targetState = "${levelInfo.currentLevelXp} / ${levelInfo.requiredXpForNextLevel} XP",
                        transitionSpec = {
                            (fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 4 }) togetherWith
                                    (fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 })
                        },
                        label = "XpNumbersTransition"
                    ) { targetXpText ->
                        Text(
                            text = targetXpText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (isShadowMonarch) MonarchTextPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { animatedXpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary,
                    trackColor = if (isShadowMonarch) Color(0xFF282E3E) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }

            if (momentumInfo != null && momentumInfo.hasEnoughHistory) {
                HomeMomentumBadge(
                    momentumInfo = momentumInfo,
                    isShadowMonarch = isShadowMonarch
                )
            }
        }
    }
}

@Composable
fun XpToastOverlay(
    xpAmount: Int?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    AnimatedVisibility(
        visible = xpAmount != null,
        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300)),
        modifier = modifier
    ) {
        if (xpAmount != null) {
            val offsetY = remember { androidx.compose.animation.core.Animatable(0f) }
            val alpha = remember { androidx.compose.animation.core.Animatable(1f) }

            LaunchedEffect(xpAmount) {
                offsetY.animateTo(-30f, animationSpec = tween(durationMillis = 700))
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 200))
                onDismiss()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .graphicsLayer {
                            this.translationY = offsetY.value * density.density
                            this.alpha = alpha.value
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+$xpAmount XP",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
