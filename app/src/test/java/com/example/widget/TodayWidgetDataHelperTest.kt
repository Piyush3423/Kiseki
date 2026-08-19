package com.example.widget

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodayWidgetDataHelperTest {

    private val testZoneId: ZoneId = ZoneId.of("UTC")
    private val testDate: LocalDate = LocalDate.of(2026, 8, 18)

    private fun dateToMillis(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(testZoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `empty task list returns zero stats and no tasks message`() {
        val stats = TodayWidgetDataHelper.computeTodayStats(
            tasks = emptyList(),
            targetDate = testDate,
            zoneId = testZoneId
        )

        assertEquals(0, stats.totalCount)
        assertEquals(0, stats.completedCount)
        assertEquals(0, stats.percentage)
        assertEquals("No tasks scheduled", stats.nextTaskTitle)
        assertNull(stats.nextTaskId)
        assertTrue(stats.hasNoTasks)
        assertFalse(stats.isAllCompleted)
    }

    @Test
    fun `computes correct completion percentage and count`() {
        val todayMillis = dateToMillis(2026, 8, 18)
        val tomorrowMillis = dateToMillis(2026, 8, 19)

        val tasks = listOf(
            ActivityTask(
                id = "1",
                title = "Task 1",
                category = "Work",
                isCompleted = true,
                dueDate = todayMillis,
                priority = Priority.Low
            ),
            ActivityTask(
                id = "2",
                title = "Task 2",
                category = "Work",
                isCompleted = true,
                dueDate = todayMillis,
                priority = Priority.Medium
            ),
            ActivityTask(
                id = "3",
                title = "Task 3",
                category = "Work",
                isCompleted = true,
                dueDate = todayMillis,
                priority = Priority.High
            ),
            ActivityTask(
                id = "4",
                title = "Task 4",
                category = "Work",
                isCompleted = true,
                dueDate = todayMillis,
                priority = Priority.Low
            ),
            ActivityTask(
                id = "5",
                title = "Task 5",
                category = "Work",
                isCompleted = true,
                dueDate = todayMillis,
                priority = Priority.Medium
            ),
            ActivityTask(
                id = "6",
                title = "DSA Practice",
                category = "Study",
                isCompleted = false,
                dueDate = todayMillis,
                priority = Priority.High
            ),
            ActivityTask(
                id = "7",
                title = "Task 7",
                category = "Study",
                isCompleted = false,
                dueDate = todayMillis,
                priority = Priority.Medium
            ),
            ActivityTask(
                id = "8",
                title = "Task 8",
                category = "Study",
                isCompleted = false,
                dueDate = todayMillis,
                priority = Priority.Low
            ),
            // Future task should not be counted for today
            ActivityTask(
                id = "9",
                title = "Future Task",
                category = "Study",
                isCompleted = false,
                dueDate = tomorrowMillis,
                priority = Priority.High
            )
        )

        val stats = TodayWidgetDataHelper.computeTodayStats(
            tasks = tasks,
            targetDate = testDate,
            zoneId = testZoneId
        )

        assertEquals(8, stats.totalCount)
        assertEquals(5, stats.completedCount)
        assertEquals(62, stats.percentage) // (5 * 100) / 8 = 62
        assertEquals("DSA Practice", stats.nextTaskTitle)
        assertEquals("6", stats.nextTaskId)
        assertFalse(stats.isAllCompleted)
        assertFalse(stats.hasNoTasks)
    }

    @Test
    fun `picks highest priority incomplete task as next`() {
        val todayMillis = dateToMillis(2026, 8, 18, 14)
        val earlierTodayMillis = dateToMillis(2026, 8, 18, 9)

        val tasks = listOf(
            ActivityTask(
                id = "low-1",
                title = "Water plants",
                category = "Home",
                isCompleted = false,
                dueDate = earlierTodayMillis,
                priority = Priority.Low
            ),
            ActivityTask(
                id = "med-1",
                title = "Review PR",
                category = "Work",
                isCompleted = false,
                dueDate = todayMillis,
                priority = Priority.Medium
            ),
            ActivityTask(
                id = "high-1",
                title = "DSA Practice",
                category = "Study",
                isCompleted = false,
                dueDate = todayMillis,
                priority = Priority.High
            )
        )

        val stats = TodayWidgetDataHelper.computeTodayStats(
            tasks = tasks,
            targetDate = testDate,
            zoneId = testZoneId
        )

        assertEquals(3, stats.totalCount)
        assertEquals(0, stats.completedCount)
        assertEquals(0, stats.percentage)
        assertEquals("DSA Practice", stats.nextTaskTitle)
        assertEquals("high-1", stats.nextTaskId)
    }

    @Test
    fun `all tasks completed shows completed state`() {
        val todayMillis = dateToMillis(2026, 8, 18)

        val tasks = listOf(
            ActivityTask(
                id = "t1",
                title = "Workout",
                category = "Health",
                isCompleted = true,
                dueDate = todayMillis,
                priority = Priority.Medium
            ),
            ActivityTask(
                id = "t2",
                title = "Meditation",
                category = "Health",
                isCompleted = true,
                dueDate = todayMillis,
                priority = Priority.Low
            )
        )

        val stats = TodayWidgetDataHelper.computeTodayStats(
            tasks = tasks,
            targetDate = testDate,
            zoneId = testZoneId
        )

        assertEquals(2, stats.totalCount)
        assertEquals(2, stats.completedCount)
        assertEquals(100, stats.percentage)
        assertEquals("All tasks completed!", stats.nextTaskTitle)
        assertNull(stats.nextTaskId)
        assertTrue(stats.isAllCompleted)
    }
}
