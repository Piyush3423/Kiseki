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

class ActivityTaskViewModel(
    private val repository: ActivityTaskRepository,
    private val categoryRepository: CategoryRepository? = null,
    private val taskGroupRepository: TaskGroupRepository? = null,
    private val templateRepository: TaskGroupTemplateRepository? = null,
    private val context: Context? = null
) : BaseViewModel() {

    init {
        categoryRepository?.let { catRepo ->
            viewModelScope.launch {
                catRepo.ensureDefaultCategories()
            }
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

    val allCategories: StateFlow<List<Category>> = (categoryRepository?.allCategories
        ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getTaskById(id: String): StateFlow<ActivityTask?> {
        return repository.getTaskById(id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    fun insertTask(task: ActivityTask) = viewModelScope.launch {
        val taskToInsert = if (task.isCompleted) {
            task.copy(completedAt = task.completedAt ?: System.currentTimeMillis())
        } else {
            task.copy(completedAt = null)
        }
        repository.insert(taskToInsert)
        context?.let { ReminderScheduler.scheduleOrCancelReminder(it, taskToInsert) }
    }

    fun updateTask(task: ActivityTask) = viewModelScope.launch {
        val existingTask = repository.getTaskByIdOneShot(task.id)
        val taskToSave = if (existingTask != null) {
            if (!existingTask.isCompleted && task.isCompleted) {
                // Task marked completed
                task.copy(completedAt = task.completedAt ?: System.currentTimeMillis())
            } else if (existingTask.isCompleted && !task.isCompleted) {
                // Task marked incomplete
                task.copy(completedAt = null)
            } else {
                // Task completion status unchanged (e.g., editing task details)
                if (task.isCompleted) {
                    task.copy(completedAt = task.completedAt ?: existingTask.completedAt ?: System.currentTimeMillis())
                } else {
                    task.copy(completedAt = null)
                }
            }
        } else {
            if (task.isCompleted) {
                task.copy(completedAt = task.completedAt ?: System.currentTimeMillis())
            } else {
                task.copy(completedAt = null)
            }
        }

        repository.update(taskToSave)
        context?.let { ReminderScheduler.scheduleOrCancelReminder(it, taskToSave) }

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

    fun deleteTask(task: ActivityTask) = viewModelScope.launch {
        repository.delete(task)
        context?.let { ReminderScheduler.cancelReminder(it, task.id) }
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
        return (taskGroupRepository?.getTasksForGroup(groupId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
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
        }
    }

    fun bulkSetGroupTasksPriority(groupId: String, priority: Priority) = viewModelScope.launch {
        val groupTasks = repository.getTasksForGroupOneShot(groupId)
        if (groupTasks.isEmpty()) return@launch

        val updatedTasks = groupTasks.map { task ->
            task.copy(priority = priority)
        }

        repository.batchUpdateTasksInGroup(updatedTasks)
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

    fun deleteTemplate(template: TaskGroupTemplate) = viewModelScope.launch {
        templateRepository?.deleteTemplate(template)
    }
}

class ActivityTaskViewModelFactory(
    private val repository: ActivityTaskRepository,
    private val categoryRepository: CategoryRepository? = null,
    private val taskGroupRepository: TaskGroupRepository? = null,
    private val templateRepository: TaskGroupTemplateRepository? = null,
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
                context = context?.applicationContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
