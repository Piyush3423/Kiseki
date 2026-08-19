package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import kotlin.math.roundToInt

enum class WorkloadCategory(val displayName: String) {
    LIGHT("Light"),
    BALANCED("Balanced"),
    HEAVY("Heavy"),
    OVERLOADED("Overloaded")
}

data class TomorrowWorkloadSummary(
    val taskCount: Int,
    val highPriorityCount: Int,
    val totalEstimatedMinutes: Int,
    val loadPercentage: Int,
    val clampedLoadPercentage: Int,
    val category: WorkloadCategory,
    val isHeavyOrOverloaded: Boolean,
    val formattedDuration: String,
    val tasks: List<ActivityTask> = emptyList()
)

object TomorrowWorkloadCalculator {
    const val STANDARD_WORKLOAD_MINUTES = 360 // 6 hours = 100%

    const val FALLBACK_MINUTES_LOW = 15
    const val FALLBACK_MINUTES_MEDIUM = 30
    const val FALLBACK_MINUTES_HIGH = 45

    /**
     * Returns the estimated duration for workload calculation purposes.
     * Uses explicit task duration if set by the user; otherwise uses priority-based fallback.
     */
    fun getEstimatedMinutesForTask(task: ActivityTask): Int {
        return task.estimatedDurationMinutes?.takeIf { it > 0 } ?: when (task.priority) {
            Priority.Low -> FALLBACK_MINUTES_LOW
            Priority.Medium -> FALLBACK_MINUTES_MEDIUM
            Priority.High -> FALLBACK_MINUTES_HIGH
        }
    }

    /**
     * Calculates the tomorrow workload summary based on the provided list of tasks scheduled for tomorrow.
     */
    fun calculate(tasks: List<ActivityTask>): TomorrowWorkloadSummary {
        val activeTasks = tasks.filter { !it.isCompleted }
        val taskCount = activeTasks.size
        val highPriorityCount = activeTasks.count { it.priority == Priority.High }

        val totalMinutes = activeTasks.sumOf { getEstimatedMinutesForTask(it) }

        val loadPercentage = if (STANDARD_WORKLOAD_MINUTES > 0) {
            ((totalMinutes.toDouble() / STANDARD_WORKLOAD_MINUTES.toDouble()) * 100.0).roundToInt()
        } else 0

        val clampedLoadPercentage = loadPercentage.coerceIn(0, 100)

        val category = when {
            loadPercentage <= 40 -> WorkloadCategory.LIGHT
            loadPercentage <= 70 -> WorkloadCategory.BALANCED
            loadPercentage <= 90 -> WorkloadCategory.HEAVY
            else -> WorkloadCategory.OVERLOADED
        }

        val isHeavyOrOverloaded = category == WorkloadCategory.HEAVY || category == WorkloadCategory.OVERLOADED

        val formattedDuration = formatDuration(totalMinutes)

        return TomorrowWorkloadSummary(
            taskCount = taskCount,
            highPriorityCount = highPriorityCount,
            totalEstimatedMinutes = totalMinutes,
            loadPercentage = loadPercentage,
            clampedLoadPercentage = clampedLoadPercentage,
            category = category,
            isHeavyOrOverloaded = isHeavyOrOverloaded,
            formattedDuration = formattedDuration,
            tasks = activeTasks
        )
    }

    fun formatDuration(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0m"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
