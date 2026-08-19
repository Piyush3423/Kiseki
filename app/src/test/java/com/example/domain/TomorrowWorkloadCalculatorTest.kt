package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
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
    fun formatDuration_testCases() {
        assertEquals("0m", TomorrowWorkloadCalculator.formatDuration(0))
        assertEquals("15m", TomorrowWorkloadCalculator.formatDuration(15))
        assertEquals("1h", TomorrowWorkloadCalculator.formatDuration(60))
        assertEquals("1h 30m", TomorrowWorkloadCalculator.formatDuration(90))
        assertEquals("4h 20m", TomorrowWorkloadCalculator.formatDuration(260))
    }
}
