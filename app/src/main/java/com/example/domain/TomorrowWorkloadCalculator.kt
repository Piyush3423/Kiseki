package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
    val tasks: List<ActivityTask> = emptyList(),
    val prioritySummary: String = ""
)

object TomorrowWorkloadCalculator {
    const val STANDARD_WORKLOAD_MINUTES = 360 // 6 hours = 100%

    const val FALLBACK_MINUTES_LOW = 15
    const val FALLBACK_MINUTES_MEDIUM = 30
    const val FALLBACK_MINUTES_HIGH = 45

    /**
     * Determines whether an incomplete task produces an occurrence scheduled for the given target date.
     */
    fun isTaskScheduledForDate(task: ActivityTask, targetDate: LocalDate): Boolean {
        if (task.isCompleted) return false
        val dueDateMillis = task.dueDate ?: return false

        val zoneId = ZoneId.systemDefault()
        val taskDate = Instant.ofEpochMilli(dueDateMillis).atZone(zoneId).toLocalDate()

        return when {
            taskDate == targetDate -> true
            taskDate > targetDate -> false
            else -> { // taskDate < targetDate
                when (task.repeatType) {
                    RepeatType.None -> false
                    RepeatType.Daily -> true
                    RepeatType.Weekly -> {
                        val daysBetween = ChronoUnit.DAYS.between(taskDate, targetDate)
                        daysBetween > 0 && daysBetween % 7 == 0L
                    }
                    RepeatType.Monthly -> {
                        val monthsBetween = ChronoUnit.MONTHS.between(taskDate, targetDate)
                        monthsBetween > 0 && taskDate.plusMonths(monthsBetween) == targetDate
                    }
                    RepeatType.Custom -> {
                        val interval = task.customDays?.takeIf { it > 0 } ?: 1
                        val daysBetween = ChronoUnit.DAYS.between(taskDate, targetDate)
                        daysBetween > 0 && daysBetween % interval.toLong() == 0L
                    }
                }
            }
        }
    }

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
     * Calculates tomorrow workload summary by filtering allTasks for baseDate + 1 day.
     */
    fun calculate(allTasks: List<ActivityTask>, baseDate: LocalDate): TomorrowWorkloadSummary {
        val tomorrowDate = baseDate.plusDays(1)
        val tomorrowTasks = allTasks.filter { isTaskScheduledForDate(it, tomorrowDate) }
        return calculate(tomorrowTasks)
    }

    /**
     * Calculates workload summary based on the provided list of tasks scheduled for tomorrow.
     */
    fun calculate(tasks: List<ActivityTask>): TomorrowWorkloadSummary {
        val activeTasks = tasks.filter { !it.isCompleted }
        val taskCount = activeTasks.size
        val highPriorityCount = activeTasks.count { it.priority == Priority.High }
        val mediumPriorityCount = activeTasks.count { it.priority == Priority.Medium }

        val prioritySummary = when {
            taskCount == 0 -> ""
            highPriorityCount > 0 -> if (highPriorityCount == 1) "1 High" else "$highPriorityCount High"
            mediumPriorityCount > 0 -> "Medium"
            else -> "Low"
        }

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
            tasks = activeTasks,
            prioritySummary = prioritySummary
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
