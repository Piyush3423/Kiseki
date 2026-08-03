package com.example.data.repository

import android.content.Context
import com.example.data.dao.ActivityTaskDao
import com.example.data.dao.TaskGroupDao
import com.example.data.dao.TaskGroupTemplateDao
import com.example.data.entity.ActivityTask
import com.example.data.entity.TaskGroup
import com.example.data.entity.TaskGroupTemplate
import com.example.data.entity.TemplateTaskItem
import com.example.util.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class TaskGroupTemplateRepository(
    private val templateDao: TaskGroupTemplateDao,
    private val taskDao: ActivityTaskDao,
    private val groupDao: TaskGroupDao
) {
    val allTemplates: Flow<List<TaskGroupTemplate>> = templateDao.getAllTemplates()

    suspend fun saveGroupAsTemplate(
        group: TaskGroup,
        tasks: List<ActivityTask>,
        customName: String? = null
    ): TaskGroupTemplate {
        val tasksWithDueDate = tasks.filter { it.dueDate != null }
        val minDueDate = tasksWithDueDate.minOfOrNull { it.dueDate!! }

        val templateItems = tasks.map { task ->
            val dueOffset = if (task.dueDate != null) {
                if (minDueDate != null && task.dueDate >= minDueDate) {
                    ((task.dueDate - minDueDate) / 86400000L).toInt()
                } else {
                    0
                }
            } else {
                null
            }

            val reminderTimeOfDayMinutes = if (task.isReminderEnabled && task.reminderTime != null) {
                val zdt = Instant.ofEpochMilli(task.reminderTime).atZone(ZoneId.systemDefault())
                zdt.hour * 60 + zdt.minute
            } else {
                null
            }

            TemplateTaskItem(
                title = task.title,
                description = task.description,
                category = task.category,
                priority = task.priority,
                repeatType = task.repeatType,
                customDays = task.customDays,
                dueDayOffset = dueOffset,
                isReminderEnabled = task.isReminderEnabled,
                reminderTimeOfDayMinutes = reminderTimeOfDayMinutes
            )
        }

        val template = TaskGroupTemplate(
            id = UUID.randomUUID().toString(),
            name = customName?.takeIf { it.isNotBlank() } ?: group.name,
            color = group.color,
            createdAt = System.currentTimeMillis(),
            itemsJson = TaskGroupTemplate.createItemsJson(templateItems)
        )

        templateDao.insert(template)
        return template
    }

    suspend fun createTasksFromTemplate(
        template: TaskGroupTemplate,
        startingDateMillis: Long,
        createNewGroup: Boolean,
        groupNameOverride: String? = null,
        context: Context? = null
    ): List<ActivityTask> {
        val items = template.parseItems()
        if (items.isEmpty()) return emptyList()

        var newGroupId: String? = null
        if (createNewGroup) {
            val groupName = groupNameOverride?.takeIf { it.isNotBlank() } ?: template.name
            val group = TaskGroup(
                id = UUID.randomUUID().toString(),
                name = groupName,
                color = template.color,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            groupDao.insertGroup(group)
            newGroupId = group.id
        }

        val now = System.currentTimeMillis()
        val newTasks = mutableListOf<ActivityTask>()

        for (item in items) {
            val newDueDate = if (item.dueDayOffset != null) {
                startingDateMillis + (item.dueDayOffset * 86400000L)
            } else {
                null
            }

            val newReminderTime = if (item.isReminderEnabled && item.reminderTimeOfDayMinutes != null) {
                val baseTime = newDueDate ?: startingDateMillis
                val zdt = Instant.ofEpochMilli(baseTime)
                    .atZone(ZoneId.systemDefault())
                    .withHour(item.reminderTimeOfDayMinutes / 60)
                    .withMinute(item.reminderTimeOfDayMinutes % 60)
                    .withSecond(0)
                    .withNano(0)
                zdt.toInstant().toEpochMilli()
            } else {
                null
            }

            val newTask = ActivityTask(
                id = UUID.randomUUID().toString(),
                title = item.title,
                description = item.description,
                category = item.category,
                priority = item.priority,
                isCompleted = false,
                completedAt = null,
                createdAt = now,
                dueDate = newDueDate,
                repeatType = item.repeatType,
                customDays = item.customDays,
                isReminderEnabled = item.isReminderEnabled,
                reminderTime = newReminderTime,
                groupId = newGroupId
            )
            newTasks.add(newTask)
        }

        taskDao.insertAll(newTasks)

        context?.let { ctx ->
            newTasks.forEach { ReminderScheduler.scheduleOrCancelReminder(ctx, it) }
        }

        return newTasks
    }

    suspend fun renameTemplate(templateId: String, newName: String) {
        val template = templateDao.getTemplateById(templateId) ?: return
        templateDao.update(template.copy(name = newName))
    }

    suspend fun deleteTemplate(template: TaskGroupTemplate) {
        templateDao.delete(template)
    }
}
