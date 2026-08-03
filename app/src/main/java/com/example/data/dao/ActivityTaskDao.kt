package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ActivityTask
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTaskDao {
    @Query("SELECT * FROM activity_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<ActivityTask>>

    @Query("SELECT * FROM activity_tasks ORDER BY createdAt DESC")
    suspend fun getAllTasksOneShot(): List<ActivityTask>

    @Query("SELECT * FROM activity_tasks WHERE id = :id")
    fun getTaskById(id: String): Flow<ActivityTask?>

    @Query("SELECT * FROM activity_tasks WHERE id = :id")
    suspend fun getTaskByIdOneShot(id: String): ActivityTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: ActivityTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<ActivityTask>)

    @Update
    suspend fun update(task: ActivityTask)

    @Delete
    suspend fun delete(task: ActivityTask)

    @Query("DELETE FROM activity_tasks")
    suspend fun deleteAllTasks()

    @Query("UPDATE activity_tasks SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryNameForTasks(oldName: String, newName: String)

    @Query("UPDATE activity_tasks SET category = :targetCategoryName WHERE category = :oldCategoryName")
    suspend fun reassignTaskCategory(oldCategoryName: String, targetCategoryName: String)

    @Query("SELECT * FROM activity_tasks WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getTasksForGroup(groupId: String): Flow<List<ActivityTask>>

    @Query("SELECT * FROM activity_tasks WHERE groupId = :groupId ORDER BY createdAt DESC")
    suspend fun getTasksForGroupOneShot(groupId: String): List<ActivityTask>

    @Transaction
    suspend fun batchUpdateTasksInGroup(
        updatedTasks: List<ActivityTask>,
        insertedTasks: List<ActivityTask> = emptyList()
    ) {
        for (task in updatedTasks) {
            update(task)
        }
        if (insertedTasks.isNotEmpty()) {
            insertAll(insertedTasks)
        }
    }

    @Query("UPDATE activity_tasks SET groupId = :groupId WHERE id = :taskId")
    suspend fun assignTaskToGroup(taskId: String, groupId: String)

    @Query("UPDATE activity_tasks SET groupId = NULL WHERE id = :taskId")
    suspend fun removeTaskFromGroup(taskId: String)

    @Query("UPDATE activity_tasks SET groupId = NULL WHERE groupId = :groupId")
    suspend fun removeAllTasksFromGroup(groupId: String)
}
