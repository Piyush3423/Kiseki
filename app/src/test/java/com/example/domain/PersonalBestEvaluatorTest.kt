package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.DailyScore
import com.example.data.entity.PersonalBest
import com.example.data.entity.XpEvent
import com.example.data.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersonalBestEvaluatorTest {

    @Test
    fun testFirstValidDayRecord() {
        val eval = PersonalBestEvaluator.evaluateRecord(
            recordKey = PersonalBestEvaluator.KEY_MOST_TASKS,
            newValue = 5,
            dateStr = "2026-08-09",
            currentRecord = null
        )

        assertTrue(eval.isNewRecord)
        assertEquals(0, eval.previousValue)
        assertNotNull(eval.record)
        assertEquals(5, eval.record?.value)
        assertEquals(false, eval.record?.acknowledged)
    }

    @Test
    fun testNewRecordBeaten() {
        val existing = PersonalBest(
            recordKey = PersonalBestEvaluator.KEY_MOST_TASKS,
            value = 10,
            dateAchieved = "2026-08-01",
            previousValue = 5,
            acknowledged = true
        )

        val eval = PersonalBestEvaluator.evaluateRecord(
            recordKey = PersonalBestEvaluator.KEY_MOST_TASKS,
            newValue = 12,
            dateStr = "2026-08-09",
            currentRecord = existing
        )

        assertTrue(eval.isNewRecord)
        assertEquals(10, eval.previousValue)
        assertNotNull(eval.record)
        assertEquals(12, eval.record?.value)
        assertEquals(10, eval.record?.previousValue)
        assertEquals(false, eval.record?.acknowledged)
    }

    @Test
    fun testTiedRecordNotNew() {
        val existing = PersonalBest(
            recordKey = PersonalBestEvaluator.KEY_MOST_TASKS,
            value = 10,
            dateAchieved = "2026-08-01",
            previousValue = 5,
            acknowledged = true
        )

        val eval = PersonalBestEvaluator.evaluateRecord(
            recordKey = PersonalBestEvaluator.KEY_MOST_TASKS,
            newValue = 10,
            dateStr = "2026-08-09",
            currentRecord = existing
        )

        assertFalse(eval.isNewRecord)
        assertEquals(10, eval.previousValue)
    }

    @Test
    fun testLowerResultNotNew() {
        val existing = PersonalBest(
            recordKey = PersonalBestEvaluator.KEY_MOST_TASKS,
            value = 10,
            dateAchieved = "2026-08-01",
            previousValue = 5,
            acknowledged = true
        )

        val eval = PersonalBestEvaluator.evaluateRecord(
            recordKey = PersonalBestEvaluator.KEY_MOST_TASKS,
            newValue = 8,
            dateStr = "2026-08-09",
            currentRecord = existing
        )

        assertFalse(eval.isNewRecord)
        assertEquals(10, eval.previousValue)
    }

    @Test
    fun testLongestStreakCalculation() {
        val dates = setOf(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 2),
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 5),
            LocalDate.of(2026, 8, 6)
        )

        val streak = PersonalBestEvaluator.calculateLongestStreak(dates)
        assertEquals(3, streak)
    }

    @Test
    fun testHistoricalCalculation() {
        val epochMillisDay1 = LocalDate.of(2026, 8, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val epochMillisDay2 = LocalDate.of(2026, 8, 2).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val tasks = listOf(
            ActivityTask(title = "Task 1", isCompleted = true, completedAt = epochMillisDay1, priority = Priority.High),
            ActivityTask(title = "Task 2", isCompleted = true, completedAt = epochMillisDay2, priority = Priority.High),
            ActivityTask(title = "Task 3", isCompleted = true, completedAt = epochMillisDay2, priority = Priority.High),
            ActivityTask(title = "Task 4", isCompleted = true, completedAt = epochMillisDay2, priority = Priority.Medium)
        )

        val dailyScores = listOf(
            DailyScore(date = "2026-08-01", score = 75, completionScore = 0.8f, priorityPerformance = 0.7f, onTimeScore = 0.8f, consistencyScore = 0.6f),
            DailyScore(date = "2026-08-02", score = 92, completionScore = 1.0f, priorityPerformance = 0.9f, onTimeScore = 0.9f, consistencyScore = 0.8f)
        )

        val xpEvents = listOf(
            XpEvent(id = "1", amount = 100, eventType = "TASK", timestamp = epochMillisDay1, date = "2026-08-01"),
            XpEvent(id = "2", amount = 150, eventType = "TASK", timestamp = epochMillisDay2, date = "2026-08-02"),
            XpEvent(id = "3", amount = 100, eventType = "BONUS", timestamp = epochMillisDay2, date = "2026-08-02")
        )

        val historical = PersonalBestEvaluator.calculateHistoricalPersonalBests(tasks, dailyScores, xpEvents, "2026-08-09")

        assertEquals(3, historical[PersonalBestEvaluator.KEY_MOST_TASKS]?.value)
        assertEquals(92, historical[PersonalBestEvaluator.KEY_HIGHEST_SCORE]?.value)
        assertEquals(250, historical[PersonalBestEvaluator.KEY_MOST_XP]?.value)
        assertEquals(2, historical[PersonalBestEvaluator.KEY_LONGEST_STREAK]?.value)
        assertEquals(2, historical[PersonalBestEvaluator.KEY_MOST_HIGH_PRIORITY]?.value)
    }
}
