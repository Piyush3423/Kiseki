package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.XpEvent
import com.example.data.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XpSystemTest {

    @Test
    fun testXpForEachPriority() {
        val lowTask = ActivityTask(title = "Low", priority = Priority.Low, isCompleted = true, completedAt = 1000L, dueDate = 1000L)
        val medTask = ActivityTask(title = "Med", priority = Priority.Medium, isCompleted = true, completedAt = 1000L, dueDate = 1000L)
        val highTask = ActivityTask(title = "High", priority = Priority.High, isCompleted = true, completedAt = 1000L, dueDate = 1000L)

        val lowEvents = XpEvaluator.evaluateTaskCompletion(lowTask, emptyList())
        val medEvents = XpEvaluator.evaluateTaskCompletion(medTask, emptyList())
        val highEvents = XpEvaluator.evaluateTaskCompletion(highTask, emptyList())

        val lowBaseEvent = lowEvents.find { it.eventType == "TASK_COMPLETED" }
        val medBaseEvent = medEvents.find { it.eventType == "TASK_COMPLETED" }
        val highBaseEvent = highEvents.find { it.eventType == "TASK_COMPLETED" }

        assertEquals(10, lowBaseEvent?.amount)
        assertEquals(20, medBaseEvent?.amount)
        assertEquals(35, highBaseEvent?.amount)
    }

    @Test
    fun testOnTimeBonus() {
        val onTimeTask = ActivityTask(title = "On Time", priority = Priority.Medium, isCompleted = true, completedAt = 1000L, dueDate = 2000L)
        val lateTask = ActivityTask(title = "Late", priority = Priority.Medium, isCompleted = true, completedAt = 3000L, dueDate = 2000L)

        val onTimeEvents = XpEvaluator.evaluateTaskCompletion(onTimeTask, emptyList())
        val lateEvents = XpEvaluator.evaluateTaskCompletion(lateTask, emptyList())

        assertTrue(onTimeEvents.any { it.eventType == "ON_TIME_BONUS" && it.amount == 5 })
        assertTrue(lateEvents.none { it.eventType == "ON_TIME_BONUS" })
    }

    @Test
    fun testDailyBonuses() {
        val task1 = ActivityTask(id = "1", title = "Task 1", isCompleted = true)
        val task2 = ActivityTask(id = "2", title = "Task 2", isCompleted = true)

        // Perfect Day bonus
        val perfectDayEvents = XpEvaluator.evaluateDailyBonuses(
            date = "2026-08-09",
            tasksForDay = listOf(task1, task2),
            dailyScore = 75,
            existingEventsForDate = emptyList()
        )
        assertTrue(perfectDayEvents.any { it.eventType == "PERFECT_DAY" && it.amount == 30 })

        // High Score Day bonus >= 80
        val score85Events = XpEvaluator.evaluateDailyBonuses(
            date = "2026-08-09",
            tasksForDay = listOf(task1, task2),
            dailyScore = 85,
            existingEventsForDate = emptyList()
        )
        assertTrue(score85Events.any { it.eventType == "HIGH_SCORE_DAY" && it.amount == 20 })

        // High Score Day bonus >= 95
        val score98Events = XpEvaluator.evaluateDailyBonuses(
            date = "2026-08-09",
            tasksForDay = listOf(task1, task2),
            dailyScore = 98,
            existingEventsForDate = emptyList()
        )
        assertTrue(score98Events.any { it.eventType == "HIGH_SCORE_DAY" && it.amount == 40 })
    }

    @Test
    fun testDuplicateCompletionAndTaskToggling() {
        val task = ActivityTask(id = "task-100", title = "Toggle Task", priority = Priority.Medium, isCompleted = true)

        // First completion generates XP
        val firstEvents = XpEvaluator.evaluateTaskCompletion(task, emptyList())
        assertEquals(2, firstEvents.size) // TASK_COMPLETED (20) + ON_TIME_BONUS (5)

        // Existing persisted events recorded for this task
        val existingEvents = listOf(
            XpEvent(amount = 20, eventType = "TASK_COMPLETED", taskId = "task-100", date = "2026-08-09"),
            XpEvent(amount = 5, eventType = "ON_TIME_BONUS", taskId = "task-100", date = "2026-08-09")
        )

        // Toggling back to completed when events already exist produces NO new events
        val secondEvents = XpEvaluator.evaluateTaskCompletion(task, existingEvents)
        assertEquals(0, secondEvents.size)
    }

    @Test
    fun testLevelThresholdsFormula() {
        // Level 1 starts at 0 XP
        val level1 = LevelCalculator.calculateLevelInfo(0)
        assertEquals(1, level1.level)
        assertEquals(0, level1.currentLevelXp)
        assertEquals(100, level1.requiredXpForNextLevel)
        assertEquals(0f, level1.progress, 0.001f)

        // 50 XP -> Level 1, 50 / 100
        val level1Half = LevelCalculator.calculateLevelInfo(50)
        assertEquals(1, level1Half.level)
        assertEquals(50, level1Half.currentLevelXp)
        assertEquals(100, level1Half.requiredXpForNextLevel)
        assertEquals(0.5f, level1Half.progress, 0.001f)

        // 100 XP -> Level 2 (100 required for Level 1->2)
        val level2 = LevelCalculator.calculateLevelInfo(100)
        assertEquals(2, level2.level)
        assertEquals(0, level2.currentLevelXp)
        assertEquals(140, level2.requiredXpForNextLevel)

        // 240 XP -> Level 3 (100 + 140 = 240)
        val level3 = LevelCalculator.calculateLevelInfo(240)
        assertEquals(3, level3.level)
        assertEquals(0, level3.currentLevelXp)
        assertEquals(180, level3.requiredXpForNextLevel)

        // 420 XP -> Level 4 (100 + 140 + 180 = 420)
        val level4 = LevelCalculator.calculateLevelInfo(420)
        assertEquals(4, level4.level)
        assertEquals(0, level4.currentLevelXp)
        assertEquals(220, level4.requiredXpForNextLevel)
    }

    @Test
    fun testPersistenceSum() {
        val persistedEvents = listOf(
            XpEvent(amount = 20, eventType = "TASK_COMPLETED", date = "2026-08-09"),
            XpEvent(amount = 5, eventType = "ON_TIME_BONUS", date = "2026-08-09"),
            XpEvent(amount = 30, eventType = "PERFECT_DAY", date = "2026-08-09")
        )

        val totalXp = persistedEvents.sumOf { it.amount }
        assertEquals(55, totalXp)

        val levelInfo = LevelCalculator.calculateLevelInfo(totalXp)
        assertEquals(1, levelInfo.level)
        assertEquals(55, levelInfo.currentLevelXp)
        assertEquals(100, levelInfo.requiredXpForNextLevel)
        assertEquals(0.55f, levelInfo.progress, 0.001f)
    }
}
