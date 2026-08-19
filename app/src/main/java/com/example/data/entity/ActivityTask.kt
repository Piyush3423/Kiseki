package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import java.util.UUID

/**
 * Core data model representing an activity or task in the application.
 * Designed to be easily extensible for future database integration.
 *
 * @param id Unique identifier for the task.
 * @param title The main title of the task.
 * @param description Optional details about the task.
 * @param category A label for organizing tasks (e.g., Work, Health).
 * @param priority Importance level of the task.
 * @param isCompleted Whether the task has been marked as done.
 * @param completedAt Optional timestamp when the task was completed.
 * @param createdAt Timestamp when the task was created.
 * @param dueDate Optional timestamp for when the task is due.
 * @param repeatType How often the task recurs.
 * @param parentTaskId Optional ID linking to a parent task for subtask structures.
 */
@Entity(tableName = "activity_tasks")
data class ActivityTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: String = "",
    val priority: Priority = Priority.Medium,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val repeatType: RepeatType = RepeatType.None,
    val parentTaskId: String? = null,
    val customDays: Int? = null,
    val isReminderEnabled: Boolean = false,
    val reminderTime: Long? = null,
    val groupId: String? = null,
    val rescheduleCount: Int = 0,
    val missCount: Int = 0,
    val lateCompletionCount: Int = 0,
    val frictionScore: Float = 0f,
    val frictionSuppressedUntil: Long? = null,
    val estimatedDurationMinutes: Int? = null
)
