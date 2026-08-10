package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyScoreCalculatorTest {

    @Test
    fun testZeroTasks() {
        val scores = DailyScoreCalculator.calculateAllScores(emptyList())
        assertEquals(0, scores.size)
    }

    @Test
    fun testOneTaskNotCompleted() {
        val task = ActivityTask(title = "Test", dueDate = System.currentTimeMillis())
        val scores = DailyScoreCalculator.calculateAllScores(listOf(task))
        assertEquals(1, scores.size)
        assertEquals(0, scores[0].score)
        assertEquals(0f, scores[0].completionScore, 0.01f)
    }

    @Test
    fun testAllCompleted() {
        val task1 = ActivityTask(title = "Task 1", isCompleted = true, completedAt = System.currentTimeMillis())
        val task2 = ActivityTask(title = "Task 2", isCompleted = true, completedAt = System.currentTimeMillis())
        val scores = DailyScoreCalculator.calculateAllScores(listOf(task1, task2))
        assertEquals(1, scores.size)
        // 40% completion + 25% priority + 20% timing + 0% consistency = 85
        assertEquals(85, scores[0].score)
        assertEquals(1f, scores[0].completionScore, 0.01f)
    }

    @Test
    fun testNoneCompleted() {
        val task1 = ActivityTask(title = "Task 1", dueDate = System.currentTimeMillis())
        val task2 = ActivityTask(title = "Task 2", dueDate = System.currentTimeMillis())
        val scores = DailyScoreCalculator.calculateAllScores(listOf(task1, task2))
        assertEquals(1, scores.size)
        assertEquals(0, scores[0].score)
    }

    @Test
    fun testOnlyLowPriority() {
        val task1 = ActivityTask(title = "Task 1", priority = Priority.Low, isCompleted = true, completedAt = System.currentTimeMillis())
        val task2 = ActivityTask(title = "Task 2", priority = Priority.Low, isCompleted = true, completedAt = System.currentTimeMillis())
        val scores = DailyScoreCalculator.calculateAllScores(listOf(task1, task2))
        assertEquals(1, scores.size)
        // 40 + 25 + 20 + 0 = 85
        assertEquals(85, scores[0].score)
    }

    @Test
    fun testMixedPriorities() {
        // High priority = 2.0, Low = 1.0. Total = 3.0
        // Completed Low = 1.0. Priority score = 1.0 / 3.0 = 0.333
        val task1 = ActivityTask(title = "Task 1", priority = Priority.High, dueDate = System.currentTimeMillis())
        val task2 = ActivityTask(title = "Task 2", priority = Priority.Low, isCompleted = true, completedAt = System.currentTimeMillis())
        // Manually set dueDate to same day so they group together
        val scores = DailyScoreCalculator.calculateAllScores(listOf(
            task1.copy(dueDate = 0L), // Epoch 0
            task2.copy(dueDate = 0L, completedAt = 0L)
        ))
        
        assertEquals(1, scores.size)
        // Completion: 1/2 = 0.5 (20)
        // Priority: 1.0 / 3.0 = 0.333 (8.33)
        // Timing: 1/1 = 1.0 (20)
        // Consistency = 0
        // Total = 48
        assertEquals(48, scores[0].score)
    }

    @Test
    fun testNoHistoricalActivity() {
        val task = ActivityTask(title = "Test", isCompleted = true, completedAt = System.currentTimeMillis())
        val scores = DailyScoreCalculator.calculateAllScores(listOf(task))
        assertEquals(0f, scores[0].consistencyScore, 0.01f)
    }

    @Test
    fun testDailyScoreToRankBoundaries() {
        assertEquals("E", dailyScoreToRank(39))
        assertEquals("D", dailyScoreToRank(40))
        assertEquals("D", dailyScoreToRank(54))
        assertEquals("C", dailyScoreToRank(55))
        assertEquals("C", dailyScoreToRank(69))
        assertEquals("B", dailyScoreToRank(70))
        assertEquals("B", dailyScoreToRank(79))
        assertEquals("A", dailyScoreToRank(80))
        assertEquals("A", dailyScoreToRank(94))
        assertEquals("S", dailyScoreToRank(95))
        assertEquals("S", dailyScoreToRank(100))
    }
}
