package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority

object TaskFrictionEvaluator {

    const val FRICTION_WARNING_THRESHOLD = 4.0f
    const val DEFAULT_SUPPRESSION_DAYS = 7

    /**
     * Checks if a task is currently considered high friction and should trigger a suggestion warning.
     */
    fun isHighFrictionTask(
        task: ActivityTask,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        if (task.isCompleted) return false

        val suppressedUntil = task.frictionSuppressedUntil
        if (suppressedUntil != null && currentTime < suppressedUntil) {
            return false
        }

        val meetsCountCriteria = task.rescheduleCount >= 3 ||
                task.missCount >= 3 ||
                task.lateCompletionCount >= 3

        val meetsScoreCriteria = task.frictionScore >= FRICTION_WARNING_THRESHOLD

        return meetsCountCriteria || meetsScoreCriteria
    }

    /**
     * Finds active high-friction tasks, ordered by highest friction score / triggers.
     */
    fun evaluateHighFrictionTasks(
        tasks: List<ActivityTask>,
        currentTime: Long = System.currentTimeMillis()
    ): List<ActivityTask> {
        return tasks.filter { isHighFrictionTask(it, currentTime) }
            .sortedWith(
                compareByDescending<ActivityTask> { it.frictionScore }
                    .thenByDescending { it.rescheduleCount + it.missCount + it.lateCompletionCount }
            )
    }

    /**
     * Evaluates task updates when rescheduling due date.
     */
    fun recordReschedule(
        task: ActivityTask,
        newDueDate: Long?,
        suppressWarningDays: Int = DEFAULT_SUPPRESSION_DAYS,
        currentTime: Long = System.currentTimeMillis()
    ): ActivityTask {
        val newRescheduleCount = task.rescheduleCount + 1
        val newFrictionScore = maxOf(0f, task.frictionScore + 1.0f)
        val suppressedUntil = currentTime + (suppressWarningDays * 24 * 3600 * 1000L)

        return task.copy(
            dueDate = newDueDate,
            rescheduleCount = newRescheduleCount,
            frictionScore = newFrictionScore,
            frictionSuppressedUntil = suppressedUntil
        )
    }

    /**
     * Records a missed / skipped task occurrence.
     */
    fun recordMiss(
        task: ActivityTask
    ): ActivityTask {
        val newMissCount = task.missCount + 1
        val newFrictionScore = maxOf(0f, task.frictionScore + 1.5f)

        return task.copy(
            missCount = newMissCount,
            frictionScore = newFrictionScore
        )
    }

    /**
     * Evaluates friction when task completion status changes.
     */
    fun recordCompletion(
        task: ActivityTask,
        completedAt: Long = System.currentTimeMillis()
    ): ActivityTask {
        val isLate = task.dueDate != null && completedAt > task.dueDate

        return if (isLate) {
            val newLateCount = task.lateCompletionCount + 1
            val newScore = maxOf(0f, task.frictionScore + 0.5f)
            task.copy(
                isCompleted = true,
                completedAt = completedAt,
                lateCompletionCount = newLateCount,
                frictionScore = newScore
            )
        } else {
            val newScore = maxOf(0f, task.frictionScore - 1.0f)
            task.copy(
                isCompleted = true,
                completedAt = completedAt,
                frictionScore = newScore
            )
        }
    }

    /**
     * Temporarily suppresses friction warnings for a task (e.g. "Keep as is").
     */
    fun suppressFriction(
        task: ActivityTask,
        days: Int = DEFAULT_SUPPRESSION_DAYS,
        currentTime: Long = System.currentTimeMillis()
    ): ActivityTask {
        val suppressedUntil = currentTime + (days * 24 * 3600 * 1000L)
        return task.copy(
            frictionSuppressedUntil = suppressedUntil
        )
    }

    /**
     * Lowers priority of a task (High -> Medium, Medium -> Low, Low -> Low) and suppresses warning.
     */
    fun lowerPriority(
        task: ActivityTask,
        days: Int = DEFAULT_SUPPRESSION_DAYS,
        currentTime: Long = System.currentTimeMillis()
    ): ActivityTask {
        val newPriority = when (task.priority) {
            Priority.High -> Priority.Medium
            Priority.Medium -> Priority.Low
            Priority.Low -> Priority.Low
        }

        val suppressedUntil = currentTime + (days * 24 * 3600 * 1000L)

        return task.copy(
            priority = newPriority,
            frictionSuppressedUntil = suppressedUntil
        )
    }

    /**
     * Helper to create 2 to 5 subtasks from a high-friction parent task.
     */
    fun createSubtasksForParent(
        parentTask: ActivityTask,
        subtaskTitles: List<String>
    ): List<ActivityTask> {
        val validTitles = subtaskTitles.map { it.trim() }.filter { it.isNotBlank() }.take(5)
        if (validTitles.size < 2) return emptyList()

        return validTitles.map { title ->
            ActivityTask(
                title = title,
                category = parentTask.category,
                priority = parentTask.priority,
                dueDate = parentTask.dueDate,
                parentTaskId = parentTask.id,
                groupId = parentTask.groupId
            )
        }
    }
}
