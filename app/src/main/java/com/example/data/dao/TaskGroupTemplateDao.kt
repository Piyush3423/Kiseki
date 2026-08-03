package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.TaskGroupTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskGroupTemplateDao {
    @Query("SELECT * FROM task_group_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<TaskGroupTemplate>>

    @Query("SELECT * FROM task_group_templates ORDER BY createdAt DESC")
    suspend fun getAllTemplatesOneShot(): List<TaskGroupTemplate>

    @Query("SELECT * FROM task_group_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: String): TaskGroupTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TaskGroupTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TaskGroupTemplate>)

    @Update
    suspend fun update(template: TaskGroupTemplate)

    @Delete
    suspend fun delete(template: TaskGroupTemplate)

    @Query("DELETE FROM task_group_templates")
    suspend fun deleteAllTemplates()
}
