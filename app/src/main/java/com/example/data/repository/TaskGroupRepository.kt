package com.example.data.repository

import com.example.data.dao.ActivityTaskDao
import com.example.data.dao.TaskGroupDao
import com.example.data.entity.ActivityTask
import com.example.data.entity.TaskGroup
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TaskGroupRepository(
    private val taskGroupDao: TaskGroupDao,
    private val activityTaskDao: ActivityTaskDao
) {

    val allGroups: Flow<List<TaskGroup>> = taskGroupDao.getAllGroups()

    suspend fun createGroup(name: String, color: Int? = null): Result<TaskGroup> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }

        val exists = taskGroupDao.checkGroupNameExists(trimmedName) > 0
        if (exists) {
            return Result.failure(IllegalArgumentException("A group with this name already exists"))
        }

        val group = TaskGroup(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            color = color,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskGroupDao.insertGroup(group)
        return Result.success(group)
    }

    suspend fun updateGroup(group: TaskGroup): Result<Unit> {
        val trimmedName = group.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }

        val existingGroup = taskGroupDao.getGroupById(group.id)
            ?: return Result.failure(IllegalArgumentException("Group not found"))

        if (!existingGroup.name.equals(trimmedName, ignoreCase = true)) {
            val exists = taskGroupDao.checkGroupNameExists(trimmedName) > 0
            if (exists) {
                return Result.failure(IllegalArgumentException("A group with this name already exists"))
            }
        }

        val updatedGroup = group.copy(
            name = trimmedName,
            updatedAt = System.currentTimeMillis()
        )
        taskGroupDao.updateGroup(updatedGroup)
        return Result.success(Unit)
    }

    suspend fun deleteGroup(groupId: String) {
        val group = taskGroupDao.getGroupById(groupId) ?: return
        
        // Remove all tasks from this group first
        activityTaskDao.removeAllTasksFromGroup(groupId)
        
        // Then delete the group
        taskGroupDao.deleteGroup(group)
    }

    fun getTasksForGroup(groupId: String): Flow<List<ActivityTask>> {
        return activityTaskDao.getTasksForGroup(groupId)
    }

    suspend fun getGroupById(groupId: String): TaskGroup? {
        return taskGroupDao.getGroupById(groupId)
    }

    suspend fun assignTaskToGroup(taskId: String, groupId: String) {
        activityTaskDao.assignTaskToGroup(taskId, groupId)
    }

    suspend fun removeTaskFromGroup(taskId: String) {
        activityTaskDao.removeTaskFromGroup(taskId)
    }
}
