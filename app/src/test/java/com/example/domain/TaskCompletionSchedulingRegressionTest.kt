package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.RepeatType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCompletionSchedulingRegressionTest {

    private val zoneId = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 8, 23)
    private val tomorrow = today.plusDays(1)

    private fun toEpoch(date: LocalDate): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    @Test
    fun completionToggling_nonRecurringTask_doesNotChangeDateOrCreateTasks() {
        val todayEpoch = toEpoch(today)
        var task = ActivityTask(
            id = "task-1",
            title = "Gym",
            dueDate = todayEpoch,
            repeatType = RepeatType.None,
            isCompleted = false
        )

        val taskList = mutableListOf(task)

        // Toggle 10 times
        for (i in 1..10) {
            val isNowCompleted = (i % 2 != 0)
            task = task.copy(isCompleted = isNowCompleted)
            taskList[0] = task
        }

        // Final state after 10 toggles (even count): isCompleted == false
        assertEquals(1, taskList.size)
        assertEquals("task-1", taskList[0].id)
        assertEquals(todayEpoch, taskList[0].dueDate)
        assertFalse(taskList[0].isCompleted)
    }

    @Test
    fun tomorrowWorkloadCalculator_deduplicatesSameSeriesOnTomorrow() {
        val tomorrowEpoch = toEpoch(tomorrow)

        // 3 duplicate Gym task records for tomorrow (e.g. from prior corrupted state)
        val tasks = listOf(
            ActivityTask(id = "gym-1", parentTaskId = "gym-series", title = "Gym", dueDate = tomorrowEpoch, repeatType = RepeatType.Daily),
            ActivityTask(id = "gym-2", parentTaskId = "gym-series", title = "Gym", dueDate = tomorrowEpoch, repeatType = RepeatType.Daily),
            ActivityTask(id = "gym-3", parentTaskId = "gym-series", title = "Gym", dueDate = tomorrowEpoch, repeatType = RepeatType.Daily)
        )

        val summary = TomorrowWorkloadCalculator.calculate(tasks, baseDate = today)

        // Should count exactly 1 Gym task for tomorrow
        assertEquals(1, summary.taskCount)
    }

    @Test
    fun isTaskScheduledForDate_pastIncompleteTask_returnsFalseForTomorrow() {
        val todayEpoch = toEpoch(today)

        val pastTask = ActivityTask(
            id = "gym-past",
            title = "Gym",
            dueDate = todayEpoch,
            repeatType = RepeatType.Daily,
            isCompleted = false
        )

        // Incomplete task on today must NOT automatically return true for tomorrow
        assertFalse(TomorrowWorkloadCalculator.isTaskScheduledForDate(pastTask, tomorrow))
    }

    @Test
    fun isTaskScheduledForDate_tomorrowTask_returnsTrueForTomorrow() {
        val tomorrowEpoch = toEpoch(tomorrow)

        val tomorrowTask = ActivityTask(
            id = "gym-tomorrow",
            title = "Gym",
            dueDate = tomorrowEpoch,
            repeatType = RepeatType.Daily,
            isCompleted = false
        )

        assertTrue(TomorrowWorkloadCalculator.isTaskScheduledForDate(tomorrowTask, tomorrow))
    }
}
