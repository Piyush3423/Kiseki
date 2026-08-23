package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ActivityTask
import com.example.data.entity.Category
import com.example.data.entity.TaskGroup
import com.example.data.entity.TaskGroupTemplate
import com.example.data.repository.ActivityTaskRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskGroupRepository
import com.example.data.repository.TaskGroupTemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.model.Priority
import com.example.data.model.RepeatType
import com.example.util.RepeatUtils
import java.util.UUID

import android.content.Context
import com.example.util.ReminderScheduler

import com.example.data.entity.EndOfDayReview
import com.example.data.entity.FocusSession
import com.example.data.entity.XpEvent
import com.example.data.repository.EndOfDayReviewRepository
import com.example.data.repository.FocusSessionRepository
import com.example.data.repository.XpRepository
import com.example.domain.LevelCalculator
import com.example.domain.LevelInfo
import com.example.domain.XpEvaluator
import com.example.domain.dailyScoreToRank
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.example.ui.taskdetails.TaskDetailsUiState
import java.util.concurrent.ConcurrentHashMap

data class DayReviewSummary(
    val date: String,
    val completedTasks: Int,
    val totalTasks: Int,
    val score: Int,
    val rank: String,
    val xpEarned: Int
)

data class FocusTimerState(
    val taskId: String? = null,
    val targetDurationMinutes: Int = 25,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val sessionStartTime: Long = 0L,
    val activeSegmentStartTime: Long = 0L,
    val accumulatedFocusedMs: Long = 0L
)

class ActivityTaskViewModel(
    private val repository: ActivityTaskRepository,
    private val categoryRepository: CategoryRepository? = null,
    private val taskGroupRepository: TaskGroupRepository? = null,
    private val templateRepository: TaskGroupTemplateRepository? = null,
    private val dailyScoreRepository: com.example.data.repository.DailyScoreRepository? = null,
    private val xpRepository: XpRepository? = null,
    private val personalBestRepository: com.example.data.repository.PersonalBestRepository? = null,
    private val endOfDayReviewRepository: EndOfDayReviewRepository? = null,
    private val focusSessionRepository: FocusSessionRepository? = null,
    private val context: Context? = null
) : BaseViewModel() {

    private val _isTasksLoaded = MutableStateFlow(false)
    val isTasksLoaded: StateFlow<Boolean> = _isTasksLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allTasks.collect {
                _isTasksLoaded.value = true
            }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.allTasks.collect { tasks ->
                val scores = com.example.domain.DailyScoreCalculator.calculateAllScores(tasks)
                dailyScoreRepository?.insertScores(scores)
            }
        }
        categoryRepository?.let { catRepo ->
            viewModelScope.launch {
                catRepo.ensureDefaultCategories()
            }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.flow.combine(
                repository.allTasks,
                dailyScoreRepository?.allScores ?: flowOf(emptyList()),
                xpRepository?.allEvents ?: flowOf(emptyList())
            ) { tasks, scores, xpEvents ->
                Triple(tasks, scores, xpEvents)
            }.collect { (tasks, scores, xpEvents) ->
                checkAndEvaluatePersonalBests(tasks, scores, xpEvents)
            }
        }
    }

    val allPersonalBests: StateFlow<List<com.example.data.entity.PersonalBest>> =
        (personalBestRepository?.allRecords ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _activePersonalBestToast = MutableStateFlow<com.example.ui.components.PersonalBestToastData?>(null)
    val activePersonalBestToast: StateFlow<com.example.ui.components.PersonalBestToastData?> = _activePersonalBestToast.asStateFlow()

    fun dismissPersonalBestToast(recordKey: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            personalBestRepository?.markAcknowledged(recordKey)
            if (_activePersonalBestToast.value?.recordKey == recordKey) {
                _activePersonalBestToast.value = null
            }
        }
    }

    private suspend fun checkAndEvaluatePersonalBests(
        tasks: List<ActivityTask>,
        dailyScores: List<com.example.data.entity.DailyScore>,
        xpEvents: List<XpEvent>
    ) {
        val pbRepo = personalBestRepository ?: return
        val existingRecords = pbRepo.getAllRecordsOneShot()
        val existingMap = existingRecords.associateBy { it.recordKey }
        val todayStr = java.time.LocalDate.now().toString()

        if (existingRecords.isEmpty()) {
            val historicalBests = com.example.domain.PersonalBestEvaluator.calculateHistoricalPersonalBests(tasks, dailyScores, xpEvents, todayStr)
            historicalBests.values.forEach { pb ->
                pbRepo.saveRecord(pb)
            }
            return
        }

        val zoneId = java.time.ZoneId.systemDefault()
        val completedTasks = tasks.filter { it.isCompleted && it.completedAt != null }

        val todayCompletedCount = completedTasks.count {
            java.time.Instant.ofEpochMilli(it.completedAt!!).atZone(zoneId).toLocalDate().toString() == todayStr
        }

        val todayScoreObj = dailyScores.find { it.date == todayStr }
        val todayScoreVal = todayScoreObj?.score ?: 0

        val todayXpVal = xpEvents.filter { it.date == todayStr }.sumOf { it.amount }

        val completedDatesSet = completedTasks
            .map { java.time.Instant.ofEpochMilli(it.completedAt!!).atZone(zoneId).toLocalDate() }
            .toSet()
        val currentStreakVal = com.example.domain.PersonalBestEvaluator.calculateLongestStreak(completedDatesSet)

        val todayHpCount = completedTasks.count {
            it.priority == Priority.High && java.time.Instant.ofEpochMilli(it.completedAt!!).atZone(zoneId).toLocalDate().toString() == todayStr
        }

        val candidates = listOf(
            com.example.domain.PersonalBestEvaluator.KEY_MOST_TASKS to todayCompletedCount,
            com.example.domain.PersonalBestEvaluator.KEY_HIGHEST_SCORE to todayScoreVal,
            com.example.domain.PersonalBestEvaluator.KEY_MOST_XP to todayXpVal,
            com.example.domain.PersonalBestEvaluator.KEY_LONGEST_STREAK to currentStreakVal,
            com.example.domain.PersonalBestEvaluator.KEY_MOST_HIGH_PRIORITY to todayHpCount
        )

        candidates.forEach { (key, value) ->
            val currentRecord = existingMap[key]
            val eval = com.example.domain.PersonalBestEvaluator.evaluateRecord(key, value, todayStr, currentRecord)
            if (eval.isNewRecord && eval.record != null) {
                pbRepo.saveRecord(eval.record)
                _activePersonalBestToast.value = com.example.ui.components.PersonalBestToastData(
                    recordKey = key,
                    title = com.example.domain.PersonalBestEvaluator.getRecordTitle(key),
                    formattedValue = com.example.domain.PersonalBestEvaluator.formatRecordValue(key, value),
                    previousValue = eval.previousValue
                )
            }
        }

        if (_activePersonalBestToast.value == null) {
            val unack = existingRecords.firstOrNull { !it.acknowledged && it.value > 0 }
            if (unack != null) {
                _activePersonalBestToast.value = com.example.ui.components.PersonalBestToastData(
                    recordKey = unack.recordKey,
                    title = com.example.domain.PersonalBestEvaluator.getRecordTitle(unack.recordKey),
                    formattedValue = com.example.domain.PersonalBestEvaluator.formatRecordValue(unack.recordKey, unack.value),
                    previousValue = unack.previousValue
                )
            }
        }
    }

    val allXpEvents: StateFlow<List<XpEvent>> = (xpRepository?.allEvents
        ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val levelInfo: StateFlow<LevelInfo> = allXpEvents
        .map { events -> LevelCalculator.calculateLevelInfo(events.sumOf { it.amount }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LevelCalculator.calculateLevelInfo(0)
        )

    val xpThisWeek: StateFlow<Int> = allXpEvents
        .map { events ->
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
            events.filter { it.timestamp >= sevenDaysAgo }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val xpThisMonth: StateFlow<Int> = allXpEvents
        .map { events ->
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            events.filter { it.timestamp >= thirtyDaysAgo }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _xpToastAmount = MutableStateFlow<Int?>(null)
    val xpToastAmount: StateFlow<Int?> = _xpToastAmount.asStateFlow()

    fun clearXpToast() {
        _xpToastAmount.value = null
    }

    private suspend fun checkAndAwardXp(task: ActivityTask) {
        val repo = xpRepository ?: return
        if (!task.isCompleted) return

        val existingEvents = repo.getEventsForTask(task.id)
        val newEvents = XpEvaluator.evaluateTaskCompletion(task, existingEvents)
        if (newEvents.isNotEmpty()) {
            newEvents.forEach { repo.insertEvent(it) }
            val totalEarned = newEvents.sumOf { it.amount }
            if (totalEarned > 0) {
                _xpToastAmount.value = totalEarned
            }
        }

        // Evaluate daily bonuses
        val dateStr = XpEvaluator.getTaskDateStr(task)
        val tasksForDay = repository.getAllTasksOneShot().filter {
            XpEvaluator.getTaskDateStr(it) == dateStr
        }
        val currentScore = dailyScoreRepository?.getScoreForDateOneShot(dateStr)?.score ?: 0
        val existingDateEvents = repo.getEventsForDateAndType(dateStr, "PERFECT_DAY") + repo.getEventsForDateAndType(dateStr, "HIGH_SCORE_DAY")
        val bonusEvents = XpEvaluator.evaluateDailyBonuses(dateStr, tasksForDay, currentScore, existingDateEvents)
        if (bonusEvents.isNotEmpty()) {
            bonusEvents.forEach { repo.insertEvent(it) }
        }
    }

    val allGroups: StateFlow<List<TaskGroup>> = (taskGroupRepository?.allGroups
        ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTemplates: StateFlow<List<TaskGroupTemplate>> = (templateRepository?.allTemplates
        ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isApplyingTemplate = MutableStateFlow(false)
    val isApplyingTemplate: StateFlow<Boolean> = _isApplyingTemplate.asStateFlow()

    val allTasks: StateFlow<List<ActivityTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDailyScores = dailyScoreRepository?.allScores?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<com.example.data.entity.DailyScore>()) ?: MutableStateFlow(emptyList<com.example.data.entity.DailyScore>()).asStateFlow()

    val momentumInfo: StateFlow<com.example.domain.MomentumResult> = kotlinx.coroutines.flow.combine(
        allTasks,
        allDailyScores
    ) { tasks, scores ->
        com.example.domain.MomentumCalculator.calculate(tasks, scores)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.domain.MomentumCalculator.calculate(emptyList(), emptyList())
    )

    val whatChangedInsights: StateFlow<List<com.example.domain.InsightItem>> = kotlinx.coroutines.flow.combine(
        allTasks,
        allDailyScores
    ) { tasks, scores ->
        com.example.domain.InsightEvaluator.evaluateInsights(tasks, scores)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val highFrictionTasks: StateFlow<List<ActivityTask>> = allTasks
        .map { tasks -> com.example.domain.TaskFrictionEvaluator.evaluateHighFrictionTasks(tasks) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val allCategories: StateFlow<List<Category>> = (categoryRepository?.allCategories
        ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val taskDetailsUiStates = ConcurrentHashMap<String, StateFlow<TaskDetailsUiState>>()
    private val taskFlows = ConcurrentHashMap<String, StateFlow<ActivityTask?>>()
    private val groupTasksFlows = ConcurrentHashMap<String, StateFlow<List<ActivityTask>>>()

    fun getTaskDetailsUiState(taskId: String): StateFlow<TaskDetailsUiState> {
        return taskDetailsUiStates.computeIfAbsent(taskId) { id ->
            var lastValidTask: ActivityTask? = null
            repository.getTaskById(id)
                .map { task ->
                    if (task != null) {
                        lastValidTask = task
                        TaskDetailsUiState.Success(task)
                    } else {
                        TaskDetailsUiState.NotFound
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = TaskDetailsUiState.Loading
                )
        }
    }

    fun getTaskById(id: String): StateFlow<ActivityTask?> {
        return taskFlows.computeIfAbsent(id) { key ->
            repository.getTaskById(key)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null
                )
        }
    }

    private val _newlyCreatedTaskId = MutableStateFlow<String?>(null)
    val newlyCreatedTaskId: StateFlow<String?> = _newlyCreatedTaskId.asStateFlow()

    fun clearNewlyCreatedTaskId() {
        _newlyCreatedTaskId.value = null
    }

    fun insertTask(task: ActivityTask) = viewModelScope.launch {
        val taskToInsert = if (task.isCompleted) {
            task.copy(completedAt = task.completedAt ?: System.currentTimeMillis())
        } else {
            task.copy(completedAt = null)
        }
        _newlyCreatedTaskId.value = taskToInsert.id
        repository.insert(taskToInsert)
        context?.let {
            ReminderScheduler.scheduleOrCancelReminder(it, taskToInsert)
            com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it)
        }
        if (taskToInsert.isCompleted) {
            checkAndAwardXp(taskToInsert)
        }
    }

    fun updateTask(task: ActivityTask) = viewModelScope.launch {
        val existingTask = repository.getTaskByIdOneShot(task.id)
        val taskToSave = if (existingTask != null) {
            if (!existingTask.isCompleted && task.isCompleted) {
                // Task marked completed
                val completedTask = task.copy(completedAt = task.completedAt ?: System.currentTimeMillis())
                com.example.domain.TaskFrictionEvaluator.recordCompletion(completedTask, completedTask.completedAt!!)
            } else if (existingTask.isCompleted && !task.isCompleted) {
                // Task marked incomplete
                task.copy(completedAt = null)
            } else {
                // Task completion status unchanged (e.g., editing task details)
                if (task.isCompleted) {
                    task.copy(completedAt = task.completedAt ?: existingTask.completedAt ?: System.currentTimeMillis())
                } else {
                    val incTask = task.copy(completedAt = null)
                    if (existingTask.dueDate != null && incTask.dueDate != null && incTask.dueDate != existingTask.dueDate && incTask.dueDate > existingTask.dueDate) {
                        com.example.domain.TaskFrictionEvaluator.recordReschedule(incTask, incTask.dueDate)
                    } else {
                        incTask
                    }
                }
            }
        } else {
            if (task.isCompleted) {
                val completedTask = task.copy(completedAt = task.completedAt ?: System.currentTimeMillis())
                com.example.domain.TaskFrictionEvaluator.recordCompletion(completedTask, completedTask.completedAt!!)
            } else {
                task.copy(completedAt = null)
            }
        }

        repository.update(taskToSave)
        context?.let {
            ReminderScheduler.scheduleOrCancelReminder(it, taskToSave)
            com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it)
        }

        if (taskToSave.isCompleted) {
            checkAndAwardXp(taskToSave)
        }

        if (existingTask != null && !existingTask.isCompleted && taskToSave.isCompleted && taskToSave.repeatType != RepeatType.None) {
            val nextDueDate = RepeatUtils.calculateNextDueDate(
                currentDueDate = taskToSave.dueDate,
                repeatType = taskToSave.repeatType,
                customDays = taskToSave.customDays
            )
            val nextTask = taskToSave.copy(
                id = UUID.randomUUID().toString(),
                isCompleted = false,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                dueDate = nextDueDate,
                reminderTime = if (taskToSave.reminderTime != null && taskToSave.dueDate != null && nextDueDate != null) {
                    // Adjust reminder time proportionally if recurring
                    taskToSave.reminderTime + (nextDueDate - taskToSave.dueDate)
                } else taskToSave.reminderTime
            )
            repository.insert(nextTask)
            context?.let { ReminderScheduler.scheduleOrCancelReminder(it, nextTask) }
        }
    }

    fun breakTaskIntoSubtasks(parentTask: ActivityTask, subtaskTitles: List<String>) = viewModelScope.launch {
        val subtasks = com.example.domain.TaskFrictionEvaluator.createSubtasksForParent(parentTask, subtaskTitles)
        if (subtasks.isNotEmpty()) {
            subtasks.forEach { repository.insert(it) }
            val updatedParent = com.example.domain.TaskFrictionEvaluator.suppressFriction(parentTask)
            repository.update(updatedParent)
            context?.let { com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it) }
        }
    }

    fun rescheduleFrictionTask(task: ActivityTask, newDueDate: Long?) = viewModelScope.launch {
        val updatedTask = com.example.domain.TaskFrictionEvaluator.recordReschedule(task, newDueDate)
        repository.update(updatedTask)
        context?.let {
            ReminderScheduler.scheduleOrCancelReminder(it, updatedTask)
            com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it)
        }
    }

    fun lowerTaskPriority(task: ActivityTask) = viewModelScope.launch {
        val updatedTask = com.example.domain.TaskFrictionEvaluator.lowerPriority(task)
        repository.update(updatedTask)
        context?.let { com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it) }
    }

    fun keepTaskAsIs(task: ActivityTask) = viewModelScope.launch {
        val updatedTask = com.example.domain.TaskFrictionEvaluator.suppressFriction(task)
        repository.update(updatedTask)
    }

    fun deleteTask(task: ActivityTask) = viewModelScope.launch {
        repository.delete(task)
        context?.let {
            ReminderScheduler.cancelReminder(it, task.id)
            com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it)
        }
    }

    fun insertCategory(category: Category) = viewModelScope.launch {
        categoryRepository?.insert(category)
    }

    fun updateCategory(category: Category, oldName: String) = viewModelScope.launch {
        categoryRepository?.update(category, oldName)
    }

    fun deleteCategory(categoryToDelete: Category, targetCategoryName: String) = viewModelScope.launch {
        categoryRepository?.deleteCategory(categoryToDelete, targetCategoryName)
    }

    fun createGroup(name: String, color: Int? = null, onResult: (Result<TaskGroup>) -> Unit = {}) = viewModelScope.launch {
        val result = taskGroupRepository?.createGroup(name, color)
        if (result != null) onResult(result)
    }

    fun updateGroup(group: TaskGroup, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch {
        val result = taskGroupRepository?.updateGroup(group)
        if (result != null) onResult(result)
    }

    fun deleteGroup(groupId: String) = viewModelScope.launch {
        taskGroupRepository?.deleteGroup(groupId)
    }

    fun getTasksForGroup(groupId: String): StateFlow<List<ActivityTask>> {
        return groupTasksFlows.computeIfAbsent(groupId) { key ->
            (taskGroupRepository?.getTasksForGroup(key) ?: flowOf(emptyList()))
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
        }
    }

    fun assignTaskToGroup(taskId: String, groupId: String) = viewModelScope.launch {
        taskGroupRepository?.assignTaskToGroup(taskId, groupId)
    }

    fun removeTaskFromGroup(taskId: String) = viewModelScope.launch {
        taskGroupRepository?.removeTaskFromGroup(taskId)
    }

    fun bulkMarkGroupTasksCompleted(groupId: String) = viewModelScope.launch {
        val groupTasks = repository.getTasksForGroupOneShot(groupId)
        val incompleteTasks = groupTasks.filter { !it.isCompleted }
        if (incompleteTasks.isEmpty()) return@launch

        val now = System.currentTimeMillis()
        val updatedTasks = mutableListOf<ActivityTask>()
        val nextTasks = mutableListOf<ActivityTask>()

        for (task in incompleteTasks) {
            val completedTask = task.copy(
                isCompleted = true,
                completedAt = task.completedAt ?: now
            )
            updatedTasks.add(completedTask)

            if (task.repeatType != RepeatType.None) {
                val nextDueDate = RepeatUtils.calculateNextDueDate(
                    currentDueDate = task.dueDate,
                    repeatType = task.repeatType,
                    customDays = task.customDays
                )
                val nextTask = task.copy(
                    id = UUID.randomUUID().toString(),
                    isCompleted = false,
                    completedAt = null,
                    createdAt = now,
                    dueDate = nextDueDate,
                    reminderTime = if (task.reminderTime != null && task.dueDate != null && nextDueDate != null) {
                        task.reminderTime + (nextDueDate - task.dueDate)
                    } else task.reminderTime
                )
                nextTasks.add(nextTask)
            }
        }

        repository.batchUpdateTasksInGroup(updatedTasks, nextTasks)

        context?.let { ctx ->
            updatedTasks.forEach { ReminderScheduler.scheduleOrCancelReminder(ctx, it) }
            nextTasks.forEach { ReminderScheduler.scheduleOrCancelReminder(ctx, it) }
            com.example.widget.KisekiWidgetUpdater.updateAllWidgets(ctx)
        }
    }

    fun bulkMarkGroupTasksIncomplete(groupId: String) = viewModelScope.launch {
        val groupTasks = repository.getTasksForGroupOneShot(groupId)
        val completedTasks = groupTasks.filter { it.isCompleted }
        if (completedTasks.isEmpty()) return@launch

        val updatedTasks = completedTasks.map { task ->
            task.copy(
                isCompleted = false,
                completedAt = null
            )
        }

        repository.batchUpdateTasksInGroup(updatedTasks)

        context?.let { ctx ->
            updatedTasks.forEach { ReminderScheduler.scheduleOrCancelReminder(ctx, it) }
            com.example.widget.KisekiWidgetUpdater.updateAllWidgets(ctx)
        }
    }

    fun bulkSetGroupTasksPriority(groupId: String, priority: Priority) = viewModelScope.launch {
        val groupTasks = repository.getTasksForGroupOneShot(groupId)
        if (groupTasks.isEmpty()) return@launch

        val updatedTasks = groupTasks.map { task ->
            task.copy(priority = priority)
        }

        repository.batchUpdateTasksInGroup(updatedTasks)
        context?.let { com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it) }
    }

    fun bulkSetGroupTasksDueDate(groupId: String, dueDate: Long?) = viewModelScope.launch {
        val groupTasks = repository.getTasksForGroupOneShot(groupId)
        if (groupTasks.isEmpty()) return@launch

        val updatedTasks = groupTasks.map { task ->
            val newReminderTime = if (dueDate != null && task.reminderTime != null && task.dueDate != null) {
                task.reminderTime + (dueDate - task.dueDate)
            } else if (dueDate == null) {
                null
            } else {
                task.reminderTime
            }
            task.copy(dueDate = dueDate, reminderTime = newReminderTime)
        }

        repository.batchUpdateTasksInGroup(updatedTasks)

        context?.let { ctx ->
            updatedTasks.forEach { ReminderScheduler.scheduleOrCancelReminder(ctx, it) }
            com.example.widget.KisekiWidgetUpdater.updateAllWidgets(ctx)
        }
    }

    fun bulkSetGroupTasksRepeatType(
        groupId: String,
        repeatType: RepeatType,
        customDays: Int? = null
    ) = viewModelScope.launch {
        val groupTasks = repository.getTasksForGroupOneShot(groupId)
        if (groupTasks.isEmpty()) return@launch

        val updatedTasks = groupTasks.map { task ->
            task.copy(
                repeatType = repeatType,
                customDays = if (repeatType == RepeatType.Custom) customDays else null
            )
        }

        repository.batchUpdateTasksInGroup(updatedTasks)
    }

    fun bulkRemoveAllTasksFromGroup(groupId: String) = viewModelScope.launch {
        repository.removeAllTasksFromGroup(groupId)
    }

    fun saveGroupAsTemplate(
        groupId: String,
        customName: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) = viewModelScope.launch {
        val templateRepo = templateRepository ?: run {
            onComplete?.invoke(false)
            return@launch
        }
        val group = taskGroupRepository?.getGroupById(groupId) ?: run {
            onComplete?.invoke(false)
            return@launch
        }
        val groupTasks = repository.getTasksForGroupOneShot(groupId)
        templateRepo.saveGroupAsTemplate(group, groupTasks, customName)
        onComplete?.invoke(true)
    }

    fun applyTemplate(
        template: TaskGroupTemplate,
        startingDateMillis: Long,
        createNewGroup: Boolean,
        groupNameOverride: String? = null,
        onComplete: (() -> Unit)? = null
    ) = viewModelScope.launch {
        if (_isApplyingTemplate.value) return@launch
        _isApplyingTemplate.value = true
        try {
            templateRepository?.createTasksFromTemplate(
                template = template,
                startingDateMillis = startingDateMillis,
                createNewGroup = createNewGroup,
                groupNameOverride = groupNameOverride,
                context = context
            )
            context?.let { com.example.widget.KisekiWidgetUpdater.updateAllWidgets(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isApplyingTemplate.value = false
            onComplete?.invoke()
        }
    }

    fun renameTemplate(templateId: String, newName: String) = viewModelScope.launch {
        templateRepository?.renameTemplate(templateId, newName)
    }

    val allEndOfDayReviews: StateFlow<List<EndOfDayReview>> =
        (endOfDayReviewRepository?.allReviews ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun saveEndOfDayReview(review: EndOfDayReview) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        endOfDayReviewRepository?.saveReview(review)
    }

    fun deleteEndOfDayReview(review: EndOfDayReview) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        endOfDayReviewRepository?.deleteReview(review)
    }

    fun deleteEndOfDayReviewForDate(date: String) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        endOfDayReviewRepository?.deleteReviewForDate(date)
    }

    fun getDaySummaryForReview(dateStr: String): DayReviewSummary {
        val zoneId = ZoneId.systemDefault()
        val todayStr = LocalDate.now().toString()
        val tasks = allTasks.value

        val dayTasks = tasks.filter { task ->
            val taskDate = if (task.dueDate != null) {
                Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate().toString()
            } else if (task.completedAt != null) {
                Instant.ofEpochMilli(task.completedAt).atZone(zoneId).toLocalDate().toString()
            } else {
                todayStr
            }
            taskDate == dateStr
        }
        val completedTasks = dayTasks.count { it.isCompleted }
        val totalTasks = dayTasks.size

        val dayScoreObj = allDailyScores.value.find { it.date == dateStr }
        val score = dayScoreObj?.score ?: if (totalTasks > 0) ((completedTasks.toFloat() / totalTasks) * 100).toInt().coerceIn(0, 100) else 0
        val rank = dailyScoreToRank(score)

        val xpSum = allXpEvents.value.filter { it.date == dateStr }.sumOf { it.amount }
        val xpEarned = if (xpSum > 0) xpSum else dayTasks.filter { it.isCompleted }.sumOf { XpEvaluator.getTaskXpAmount(it.priority) }

        return DayReviewSummary(
            date = dateStr,
            completedTasks = completedTasks,
            totalTasks = totalTasks,
            score = score,
            rank = rank,
            xpEarned = xpEarned
        )
    }

    fun getTomorrowWorkloadSummary(baseDate: LocalDate = LocalDate.now()): com.example.domain.TomorrowWorkloadSummary {
        return com.example.domain.TomorrowWorkloadCalculator.calculate(allTasks.value, baseDate = baseDate)
    }

    fun deleteTemplate(template: TaskGroupTemplate) = viewModelScope.launch {
        templateRepository?.deleteTemplate(template)
    }

    val allFocusSessions: StateFlow<List<FocusSession>> =
        (focusSessionRepository?.allSessions ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val focusAnalyticsData: StateFlow<com.example.domain.FocusAnalyticsData> = allFocusSessions
        .map { sessions ->
            com.example.domain.FocusAnalyticsEvaluator.calculateAnalytics(sessions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.domain.FocusAnalyticsEvaluator.calculateAnalytics(emptyList())
        )

    private val _focusTimerState = MutableStateFlow(FocusTimerState())
    val focusTimerState: StateFlow<FocusTimerState> = _focusTimerState.asStateFlow()

    fun startFocusTimer(taskId: String, targetDurationMinutes: Int) {
        val now = System.currentTimeMillis()
        val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
        _focusTimerState.value = FocusTimerState(
            taskId = taskId,
            targetDurationMinutes = targetDurationMinutes,
            isRunning = true,
            isPaused = false,
            sessionStartTime = now,
            activeSegmentStartTime = elapsedRealtime,
            accumulatedFocusedMs = 0L
        )
    }

    fun pauseFocusTimer() {
        val current = _focusTimerState.value
        if (!current.isRunning || current.isPaused) return
        val elapsedInSegment = android.os.SystemClock.elapsedRealtime() - current.activeSegmentStartTime
        _focusTimerState.value = current.copy(
            isRunning = false,
            isPaused = true,
            accumulatedFocusedMs = current.accumulatedFocusedMs + elapsedInSegment
        )
    }

    fun resumeFocusTimer() {
        val current = _focusTimerState.value
        if (current.isRunning || !current.isPaused) return
        _focusTimerState.value = current.copy(
            isRunning = true,
            isPaused = false,
            activeSegmentStartTime = android.os.SystemClock.elapsedRealtime()
        )
    }

    fun resetFocusTimer(targetDurationMinutes: Int? = null) {
        val duration = targetDurationMinutes ?: _focusTimerState.value.targetDurationMinutes
        _focusTimerState.value = FocusTimerState(
            taskId = _focusTimerState.value.taskId,
            targetDurationMinutes = duration,
            isRunning = false,
            isPaused = false,
            sessionStartTime = 0L,
            activeSegmentStartTime = 0L,
            accumulatedFocusedMs = 0L
        )
    }

    fun setTargetFocusDuration(minutes: Int) {
        val current = _focusTimerState.value
        if (!current.isRunning && !current.isPaused) {
            _focusTimerState.value = current.copy(targetDurationMinutes = minutes)
        }
    }

    fun getActualFocusedDurationMs(): Long {
        val current = _focusTimerState.value
        return if (current.isRunning) {
            current.accumulatedFocusedMs + (android.os.SystemClock.elapsedRealtime() - current.activeSegmentStartTime)
        } else {
            current.accumulatedFocusedMs
        }
    }

    fun saveFocusSession(
        taskId: String,
        startTime: Long,
        endTime: Long,
        durationMs: Long,
        isTaskCompleted: Boolean,
        onSaved: (() -> Unit)? = null
    ) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        if (durationMs > 0) {
            val session = FocusSession(
                taskId = taskId,
                startTime = startTime,
                endTime = endTime,
                duration = durationMs,
                completed = isTaskCompleted
            )
            focusSessionRepository?.insertSession(session)

            // XP Evaluation for Focus Session:
            // +5 XP for session >= 25 minutes, max 20 XP per day, prevent abuse
            val dateStr = LocalDate.now().toString()
            val existingTodayFocusEvents = xpRepository?.getEventsForDateAndType(dateStr, XpEvaluator.EVENT_FOCUS_BONUS) ?: emptyList()
            val focusXpEvent = XpEvaluator.evaluateFocusSessionXp(
                durationMs = durationMs,
                taskId = taskId,
                date = dateStr,
                existingFocusEventsToday = existingTodayFocusEvents
            )
            if (focusXpEvent != null) {
                xpRepository?.insertEvent(focusXpEvent)
                _xpToastAmount.value = focusXpEvent.amount
            }
        }

        withContext(kotlinx.coroutines.Dispatchers.Main) {
            resetFocusTimer()
            onSaved?.invoke()
        }
    }

    fun completeTaskFromFocus(
        task: ActivityTask,
        startTime: Long,
        endTime: Long,
        durationMs: Long,
        onComplete: (() -> Unit)? = null
    ) = viewModelScope.launch {
        // Use existing task completion flow:
        updateTask(task.copy(isCompleted = true))
        saveFocusSession(
            taskId = task.id,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs,
            isTaskCompleted = true,
            onSaved = onComplete
        )
    }

    fun deleteFocusSession(session: FocusSession) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        focusSessionRepository?.deleteSession(session)
    }
}

class ActivityTaskViewModelFactory(
    private val repository: ActivityTaskRepository,
    private val categoryRepository: CategoryRepository? = null,
    private val taskGroupRepository: TaskGroupRepository? = null,
    private val templateRepository: TaskGroupTemplateRepository? = null,
    private val dailyScoreRepository: com.example.data.repository.DailyScoreRepository? = null,
    private val xpRepository: XpRepository? = null,
    private val personalBestRepository: com.example.data.repository.PersonalBestRepository? = null,
    private val endOfDayReviewRepository: EndOfDayReviewRepository? = null,
    private val focusSessionRepository: FocusSessionRepository? = null,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityTaskViewModel(
                repository = repository,
                categoryRepository = categoryRepository,
                taskGroupRepository = taskGroupRepository,
                templateRepository = templateRepository,
                dailyScoreRepository = dailyScoreRepository,
                xpRepository = xpRepository,
                personalBestRepository = personalBestRepository,
                endOfDayReviewRepository = endOfDayReviewRepository,
                focusSessionRepository = focusSessionRepository,
                context = context?.applicationContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
