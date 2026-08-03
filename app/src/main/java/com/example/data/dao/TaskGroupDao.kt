package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.TaskGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskGroupDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: TaskGroup): Long

    @Update
    suspend fun updateGroup(group: TaskGroup)

    @Delete
    suspend fun deleteGroup(group: TaskGroup)

    @Query("SELECT * FROM task_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<TaskGroup>>

    @Query("SELECT * FROM task_groups")
    suspend fun getAllGroupsOneShot(): List<TaskGroup>

    @Query("SELECT * FROM task_groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: String): TaskGroup?

    @Query("SELECT COUNT(*) FROM task_groups WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun checkGroupNameExists(name: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<TaskGroup>)

    @Query("DELETE FROM task_groups")
    suspend fun deleteAllGroups()
}
