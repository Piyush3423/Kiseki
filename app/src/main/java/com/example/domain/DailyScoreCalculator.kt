package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.DailyScore
import com.example.data.model.Priority
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

fun dailyScoreToRank(score: Int): String {
    return when {
        score >= 95 -> "S"
        score >= 80 -> "A"
        score >= 70 -> "B"
        score >= 55 -> "C"
        score >= 40 -> "D"
        else -> "E"
    }
}

object DailyScoreCalculator {

    fun dailyScoreToRank(score: Int): String = com.example.domain.dailyScoreToRank(score)

    fun calculateAllScores(tasks: List<ActivityTask>): List<DailyScore> {
        val zoneId = ZoneId.systemDefault()
        val todayStr = LocalDate.now().toString()
        
        // Group tasks by their assigned date
        val tasksByDate = mutableMapOf<String, MutableList<ActivityTask>>()
        
        tasks.forEach { task ->
            val dateStr = getTaskDateStr(task, zoneId, todayStr)
            tasksByDate.getOrPut(dateStr) { mutableListOf() }.add(task)
        }

        // Determine active days in the last 7 days for consistency
        // A day is active if there is at least 1 completed task that was completed on that day.
        val completedTaskDates = tasks
            .filter { it.isCompleted && it.completedAt != null }
            .map { Instant.ofEpochMilli(it.completedAt!!).atZone(zoneId).toLocalDate().toString() }
            .toSet()

        val results = mutableListOf<DailyScore>()

        tasksByDate.forEach { (dateStr, dateTasks) ->
            // active days in the 7 days prior to dateStr
            val date = LocalDate.parse(dateStr)
            var activeDaysInLast7 = 0
            for (i in 1..7) {
                val prevDate = date.minusDays(i.toLong()).toString()
                if (completedTaskDates.contains(prevDate)) {
                    activeDaysInLast7++
                }
            }
            results.add(calculateScore(dateStr, dateTasks, activeDaysInLast7))
        }
        
        return results
    }

    private fun getTaskDateStr(task: ActivityTask, zoneId: ZoneId, todayStr: String): String {
        if (task.dueDate != null) {
            return Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate().toString()
        }
        if (task.completedAt != null) {
            return Instant.ofEpochMilli(task.completedAt).atZone(zoneId).toLocalDate().toString()
        }
        return todayStr
    }

    private fun calculateScore(
        date: String,
        tasksForDay: List<ActivityTask>,
        activeDaysInLast7: Int
    ): DailyScore {
        if (tasksForDay.isEmpty()) {
            return DailyScore(date, 0, 0f, 0f, 0f, 0f)
        }

        val totalTasks = tasksForDay.size
        val completedTasksList = tasksForDay.filter { it.isCompleted }
        val completedTasks = completedTasksList.size

        val completionScore = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

        val weightedTotal = tasksForDay.sumOf { weightForPriority(it.priority) }.toFloat()
        val weightedCompleted = completedTasksList.sumOf { weightForPriority(it.priority) }.toFloat()
        val priorityPerformance = if (weightedTotal > 0f) weightedCompleted / weightedTotal else 0f

        var onTimeCompletedTasks = 0
        completedTasksList.forEach { task ->
            if (task.dueDate == null) {
                onTimeCompletedTasks++
            } else if (task.completedAt != null && task.completedAt <= task.dueDate) {
                onTimeCompletedTasks++
            } else if (task.completedAt == null) {
                onTimeCompletedTasks++
            }
        }
        val onTimeScore = if (completedTasks > 0) onTimeCompletedTasks.toFloat() / completedTasks else 0f

        val consistencyScore = (activeDaysInLast7.toFloat() / 7f).coerceIn(0f, 1f)

        var finalScore = (completionScore * 40f) +
                (priorityPerformance * 25f) +
                (onTimeScore * 20f) +
                (consistencyScore * 15f)

        return DailyScore(
            date = date,
            score = finalScore.toInt().coerceIn(0, 100),
            completionScore = completionScore,
            priorityPerformance = priorityPerformance,
            onTimeScore = onTimeScore,
            consistencyScore = consistencyScore
        )
    }

    private fun weightForPriority(priority: Priority): Double {
        return when (priority) {
            Priority.Low -> 1.0
            Priority.Medium -> 1.5
            Priority.High -> 2.0
            else -> 1.0
        }
    }
}
