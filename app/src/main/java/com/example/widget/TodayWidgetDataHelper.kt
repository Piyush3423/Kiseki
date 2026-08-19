package com.example.widget

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayWidgetStats(
    val totalCount: Int,
    val completedCount: Int,
    val percentage: Int,
    val nextTaskTitle: String,
    val nextTaskId: String?,
    val isAllCompleted: Boolean,
    val hasNoTasks: Boolean
)

object TodayWidgetDataHelper {

    private val priorityWeight = mapOf(
        Priority.High to 3,
        Priority.Medium to 2,
        Priority.Low to 1
    )

    fun computeTodayStats(
        tasks: List<ActivityTask>,
        targetDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TodayWidgetStats {
        val todayTasks = tasks.filter { task ->
            if (task.dueDate != null) {
                val taskDate = Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate()
                taskDate == targetDate
            } else {
                // Undated tasks are part of active today pool
                true
            }
        }

        val totalCount = todayTasks.size
        val completedCount = todayTasks.count { it.isCompleted }
        val percentage = if (totalCount > 0) {
            (completedCount * 100) / totalCount
        } else {
            0
        }

        val incompleteTasks = todayTasks.filter { !it.isCompleted }

        val sortedIncomplete = incompleteTasks.sortedWith(
            Comparator { task1, task2 ->
                val p1 = priorityWeight[task1.priority] ?: 0
                val p2 = priorityWeight[task2.priority] ?: 0
                if (p1 != p2) {
                    p2.compareTo(p1) // High priority first
                } else {
                    val d1 = task1.dueDate ?: Long.MAX_VALUE
                    val d2 = task2.dueDate ?: Long.MAX_VALUE
                    if (d1 != d2) {
                        d1.compareTo(d2) // Earliest due date first
                    } else {
                        task2.createdAt.compareTo(task1.createdAt)
                    }
                }
            }
        )

        val nextTask = sortedIncomplete.firstOrNull()
        val hasNoTasks = totalCount == 0
        val isAllCompleted = totalCount > 0 && incompleteTasks.isEmpty()

        val nextTaskTitle = when {
            hasNoTasks -> "No tasks scheduled"
            isAllCompleted -> "All tasks completed!"
            nextTask != null -> nextTask.title
            else -> "No upcoming tasks"
        }

        return TodayWidgetStats(
            totalCount = totalCount,
            completedCount = completedCount,
            percentage = percentage,
            nextTaskTitle = nextTaskTitle,
            nextTaskId = nextTask?.id,
            isAllCompleted = isAllCompleted,
            hasNoTasks = hasNoTasks
        )
    }
}
