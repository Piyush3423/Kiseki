package com.example.data.repository

import com.example.data.dao.ActivityTaskDao
import com.example.data.entity.ActivityTask
import kotlinx.coroutines.flow.Flow

class ActivityTaskRepository(private val activityTaskDao: ActivityTaskDao) {

    val allTasks: Flow<List<ActivityTask>> = activityTaskDao.getAllTasks()

    suspend fun getAllTasksOneShot(): List<ActivityTask> {
        return activityTaskDao.getAllTasksOneShot()
    }

    fun getTaskById(id: String): Flow<ActivityTask?> {
        return activityTaskDao.getTaskById(id)
    }

    suspend fun getTaskByIdOneShot(id: String): ActivityTask? {
        return activityTaskDao.getTaskByIdOneShot(id)
    }

    suspend fun insert(task: ActivityTask) {
        activityTaskDao.insert(task)
    }

    suspend fun update(task: ActivityTask) {
        activityTaskDao.update(task)
    }

    suspend fun delete(task: ActivityTask) {
        activityTaskDao.delete(task)
    }

    suspend fun getTasksForGroupOneShot(groupId: String): List<ActivityTask> {
        return activityTaskDao.getTasksForGroupOneShot(groupId)
    }

    suspend fun batchUpdateTasksInGroup(
        updatedTasks: List<ActivityTask>,
        insertedTasks: List<ActivityTask> = emptyList()
    ) {
        activityTaskDao.batchUpdateTasksInGroup(updatedTasks, insertedTasks)
    }

    suspend fun removeAllTasksFromGroup(groupId: String) {
        activityTaskDao.removeAllTasksFromGroup(groupId)
    }
}
