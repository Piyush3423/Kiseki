package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TomorrowWorkloadCalculatorTest {

    @Test
    fun calculate_emptyTasks_returnsLightWorkload() {
        val summary = TomorrowWorkloadCalculator.calculate(emptyList())

        assertEquals(0, summary.taskCount)
        assertEquals(0, summary.highPriorityCount)
        assertEquals(0, summary.totalEstimatedMinutes)
        assertEquals(0, summary.loadPercentage)
        assertEquals(0, summary.clampedLoadPercentage)
        assertEquals(WorkloadCategory.LIGHT, summary.category)
        assertFalse(summary.isHeavyOrOverloaded)
        assertEquals("0m", summary.formattedDuration)
    }

    @Test
    fun calculate_usesFallbackDurationsWhenNotProvided() {
        val tasks = listOf(
            ActivityTask(title = "Low Task", priority = Priority.Low),       // 15 min
            ActivityTask(title = "Medium Task", priority = Priority.Medium), // 30 min
            ActivityTask(title = "High Task", priority = Priority.High)      // 45 min
        )

        val summary = TomorrowWorkloadCalculator.calculate(tasks)

        assertEquals(3, summary.taskCount)
        assertEquals(1, summary.highPriorityCount)
        assertEquals(90, summary.totalEstimatedMinutes) // 15 + 30 + 45 = 90
        assertEquals(25, summary.loadPercentage)       // 90 / 360 = 25%
        assertEquals(25, summary.clampedLoadPercentage)
        assertEquals(WorkloadCategory.LIGHT, summary.category)
        assertFalse(summary.isHeavyOrOverloaded)
        assertEquals("1h 30m", summary.formattedDuration)
    }

    @Test
    fun calculate_usesExplicitDurationWhenProvided() {
        val tasks = listOf(
            ActivityTask(title = "Task with custom duration", priority = Priority.Low, estimatedDurationMinutes = 120),
            ActivityTask(title = "Task with fallback", priority = Priority.Medium) // 30 min fallback
        )

        val summary = TomorrowWorkloadCalculator.calculate(tasks)

        assertEquals(2, summary.taskCount)
        assertEquals(150, summary.totalEstimatedMinutes) // 120 + 30 = 150
        assertEquals(42, summary.loadPercentage)       // 150 / 360 = 41.6% -> 42%
        assertEquals(WorkloadCategory.BALANCED, summary.category)
        assertFalse(summary.isHeavyOrOverloaded)
        assertEquals("2h 30m", summary.formattedDuration)
    }

    @Test
    fun calculate_exactUserExample_7Tasks_2HighPriority_4h20m() {
        // 4h 20m = 260m (280m / 360m = 77.7% -> 78%)
        val tasks = listOf(
            ActivityTask(title = "High 1", priority = Priority.High, estimatedDurationMinutes = 60),
            ActivityTask(title = "High 2", priority = Priority.High, estimatedDurationMinutes = 60),
            ActivityTask(title = "Med 1", priority = Priority.Medium, estimatedDurationMinutes = 40),
            ActivityTask(title = "Med 2", priority = Priority.Medium, estimatedDurationMinutes = 40),
            ActivityTask(title = "Med 3", priority = Priority.Medium, estimatedDurationMinutes = 30),
            ActivityTask(title = "Low 1", priority = Priority.Low, estimatedDurationMinutes = 15),
            ActivityTask(title = "Low 2", priority = Priority.Low, estimatedDurationMinutes = 15)
        )

        val summary = TomorrowWorkloadCalculator.calculate(tasks)

        assertEquals(7, summary.taskCount)
        assertEquals(2, summary.highPriorityCount)
        assertEquals(260, summary.totalEstimatedMinutes)
        assertEquals(72, summary.loadPercentage)
        assertEquals(WorkloadCategory.HEAVY, summary.category)
        assertTrue(summary.isHeavyOrOverloaded)
        assertEquals("4h 20m", summary.formattedDuration)
    }

    @Test
    fun calculate_78PercentLoad_isHeavyCategory() {
        // 280 minutes out of 360 = 77.78% -> 78%
        val tasks = listOf(
            ActivityTask(title = "High 1", priority = Priority.High, estimatedDurationMinutes = 70),
            ActivityTask(title = "High 2", priority = Priority.High, estimatedDurationMinutes = 70),
            ActivityTask(title = "Task 3", priority = Priority.Medium, estimatedDurationMinutes = 35),
            ActivityTask(title = "Task 4", priority = Priority.Medium, estimatedDurationMinutes = 35),
            ActivityTask(title = "Task 5", priority = Priority.Medium, estimatedDurationMinutes = 35),
            ActivityTask(title = "Task 6", priority = Priority.Low, estimatedDurationMinutes = 20),
            ActivityTask(title = "Task 7", priority = Priority.Low, estimatedDurationMinutes = 15)
        )

        val summary = TomorrowWorkloadCalculator.calculate(tasks)

        assertEquals(7, summary.taskCount)
        assertEquals(2, summary.highPriorityCount)
        assertEquals(280, summary.totalEstimatedMinutes)
        assertEquals(78, summary.loadPercentage)
        assertEquals(WorkloadCategory.HEAVY, summary.category)
        assertTrue(summary.isHeavyOrOverloaded)
        assertEquals("4h 40m", summary.formattedDuration)
    }

    @Test
    fun calculate_workloadCategories_boundaries() {
        // <= 40% -> LIGHT (144 min / 360 min = 40%)
        val lightTasks = listOf(ActivityTask(title = "T1", priority = Priority.Low, estimatedDurationMinutes = 144))
        assertEquals(WorkloadCategory.LIGHT, TomorrowWorkloadCalculator.calculate(lightTasks).category)

        // 41-70% -> BALANCED (148 min / 360 min = 41.1% -> 41%)
        val balancedTasks = listOf(ActivityTask(title = "T2", priority = Priority.Low, estimatedDurationMinutes = 148))
        assertEquals(WorkloadCategory.BALANCED, TomorrowWorkloadCalculator.calculate(balancedTasks).category)

        // 71-90% -> HEAVY (256 min / 360 min = 71.1% -> 71%)
        val heavyTasks = listOf(ActivityTask(title = "T3", priority = Priority.Low, estimatedDurationMinutes = 256))
        assertEquals(WorkloadCategory.HEAVY, TomorrowWorkloadCalculator.calculate(heavyTasks).category)

        // > 90% -> OVERLOADED (328 min / 360 min = 91.1% -> 91%)
        val overloadedTasks = listOf(ActivityTask(title = "T4", priority = Priority.Low, estimatedDurationMinutes = 328))
        assertEquals(WorkloadCategory.OVERLOADED, TomorrowWorkloadCalculator.calculate(overloadedTasks).category)
    }

    @Test
    fun calculate_overloaded_clampsVisualizationAt100() {
        val tasks = listOf(
            ActivityTask(title = "Massive task 1", priority = Priority.High, estimatedDurationMinutes = 240),
            ActivityTask(title = "Massive task 2", priority = Priority.High, estimatedDurationMinutes = 200)
        )

        val summary = TomorrowWorkloadCalculator.calculate(tasks)

        assertEquals(2, summary.taskCount)
        assertEquals(440, summary.totalEstimatedMinutes)
        assertEquals(122, summary.loadPercentage)        // Raw load is 122%
        assertEquals(100, summary.clampedLoadPercentage) // Clamped load is 100%
        assertEquals(WorkloadCategory.OVERLOADED, summary.category)
        assertTrue(summary.isHeavyOrOverloaded)
        assertEquals("7h 20m", summary.formattedDuration)
    }

    @Test
    fun isTaskScheduledForDate_filteringTests() {
        val selectedDate = LocalDate.of(2026, 8, 23)
        val tomorrowDate = selectedDate.plusDays(1) // 2026-08-24
        val zoneId = ZoneId.systemDefault()

        fun toEpoch(date: LocalDate): Long {
            return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }

        // One-time task scheduled for tomorrow
        val oneTimeTomorrow = ActivityTask(
            title = "One Time Tomorrow",
            dueDate = toEpoch(tomorrowDate),
            repeatType = RepeatType.None
        )
        assertTrue(TomorrowWorkloadCalculator.isTaskScheduledForDate(oneTimeTomorrow, tomorrowDate))

        // One-time task scheduled for today (should NOT match tomorrow)
        val oneTimeToday = ActivityTask(
            title = "One Time Today",
            dueDate = toEpoch(selectedDate),
            repeatType = RepeatType.None
        )
        assertFalse(TomorrowWorkloadCalculator.isTaskScheduledForDate(oneTimeToday, tomorrowDate))

        // Daily task due today (should produce occurrence tomorrow)
        val dailyTask = ActivityTask(
            title = "Daily Gym",
            dueDate = toEpoch(selectedDate),
            repeatType = RepeatType.Daily
        )
        assertTrue(TomorrowWorkloadCalculator.isTaskScheduledForDate(dailyTask, tomorrowDate))

        // Weekly task due 7 days prior to tomorrow (should match)
        val weeklyTaskMatch = ActivityTask(
            title = "Weekly Match",
            dueDate = toEpoch(tomorrowDate.minusWeeks(1)),
            repeatType = RepeatType.Weekly
        )
        assertTrue(TomorrowWorkloadCalculator.isTaskScheduledForDate(weeklyTaskMatch, tomorrowDate))

        // Weekly task due 6 days prior (should NOT match)
        val weeklyTaskNoMatch = ActivityTask(
            title = "Weekly No Match",
            dueDate = toEpoch(tomorrowDate.minusDays(6)),
            repeatType = RepeatType.Weekly
        )
        assertFalse(TomorrowWorkloadCalculator.isTaskScheduledForDate(weeklyTaskNoMatch, tomorrowDate))

        // Completed tomorrow task (should be excluded)
        val completedTomorrow = ActivityTask(
            title = "Completed Tomorrow",
            dueDate = toEpoch(tomorrowDate),
            isCompleted = true
        )
        assertFalse(TomorrowWorkloadCalculator.isTaskScheduledForDate(completedTomorrow, tomorrowDate))
    }

    @Test
    fun calculate_withBaseDate_filtersTasksCorrectly() {
        val baseDate = LocalDate.of(2026, 8, 23)
        val zoneId = ZoneId.systemDefault()
        fun toEpoch(date: LocalDate): Long = date.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val allTasks = listOf(
            ActivityTask(title = "Task Aug 24", dueDate = toEpoch(baseDate.plusDays(1)), priority = Priority.Medium),
            ActivityTask(title = "Task Aug 24 Second", dueDate = toEpoch(baseDate.plusDays(1)), priority = Priority.Medium),
            ActivityTask(title = "Task Aug 25", dueDate = toEpoch(baseDate.plusDays(2)), priority = Priority.High)
        )

        val summary = TomorrowWorkloadCalculator.calculate(allTasks, baseDate = baseDate)

        assertEquals(2, summary.taskCount)
        assertEquals(60, summary.totalEstimatedMinutes) // 2 Medium fallback = 30 + 30 = 60
        assertEquals("Medium", summary.prioritySummary)
    }

    @Test
    fun calculate_300Minutes_returns83PercentLoadAndHeavyCategory() {
        val tasks = listOf(
            ActivityTask(title = "Task 1", estimatedDurationMinutes = 150, priority = Priority.Medium),
            ActivityTask(title = "Task 2", estimatedDurationMinutes = 150, priority = Priority.Medium)
        )

        val summary = TomorrowWorkloadCalculator.calculate(tasks)

        assertEquals(2, summary.taskCount)
        assertEquals(300, summary.totalEstimatedMinutes)
        assertEquals(83, summary.loadPercentage) // 300 / 360 = 83.33% -> 83%
        assertEquals(WorkloadCategory.HEAVY, summary.category)
        assertTrue(summary.isHeavyOrOverloaded)
        assertEquals("5h", summary.formattedDuration)
    }

    @Test
    fun formatDuration_testCases() {
        assertEquals("0m", TomorrowWorkloadCalculator.formatDuration(0))
        assertEquals("15m", TomorrowWorkloadCalculator.formatDuration(15))
        assertEquals("1h", TomorrowWorkloadCalculator.formatDuration(60))
        assertEquals("1h 30m", TomorrowWorkloadCalculator.formatDuration(90))
        assertEquals("4h 20m", TomorrowWorkloadCalculator.formatDuration(260))
    }
}
