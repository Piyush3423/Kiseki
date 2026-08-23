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
    fun testTaskUncompletionReversesXpAndOnTimeBonus() {
        val completedTask = ActivityTask(
            id = "task-200",
            title = "Reversal Test",
            priority = Priority.Medium,
            isCompleted = true,
            dueDate = 2000L,
            completedAt = 1000L
        )

        // Initial completion
        val initialEvents = XpEvaluator.evaluateTaskCompletion(completedTask, emptyList())
        val taskCompletedEvent = initialEvents.find { it.eventType == XpEvaluator.EVENT_TASK_COMPLETED }
        val onTimeEvent = initialEvents.find { it.eventType == XpEvaluator.EVENT_ON_TIME_BONUS }
        assertEquals(20, taskCompletedEvent?.amount)
        assertEquals(5, onTimeEvent?.amount)

        // Ledger contains initial events
        val historyAfterCompletion = initialEvents

        // Uncomplete task
        val uncompletedTask = completedTask.copy(isCompleted = false, completedAt = null)
        val reversalEvents = XpEvaluator.evaluateTaskCompletion(uncompletedTask, historyAfterCompletion)

        val taskUncompletedEvent = reversalEvents.find { it.eventType == XpEvaluator.EVENT_TASK_UNCOMPLETED }
        val onTimeReversalEvent = reversalEvents.find { it.eventType == XpEvaluator.EVENT_ON_TIME_REVERSAL }
        assertEquals(-20, taskUncompletedEvent?.amount)
        assertEquals(-5, onTimeReversalEvent?.amount)

        // Total net XP should now be 0
        val totalNetXp = (historyAfterCompletion + reversalEvents).sumOf { it.amount }
        assertEquals(0, totalNetXp)
    }

    @Test
    fun testCompleteUncompleteCompleteSequence() {
        val task = ActivityTask(
            id = "task-300",
            title = "Toggle Sequence",
            priority = Priority.High,
            isCompleted = true,
            dueDate = 2000L,
            completedAt = 1000L
        )

        var ledger = mutableListOf<XpEvent>()

        // 1. Complete -> +35 XP + 5 on-time = +40
        val step1 = XpEvaluator.evaluateTaskCompletion(task, ledger)
        ledger.addAll(step1)
        assertEquals(40, ledger.sumOf { it.amount })

        // 2. Uncomplete -> -35 - 5 = -40
        val uncompletedTask = task.copy(isCompleted = false)
        val step2 = XpEvaluator.evaluateTaskCompletion(uncompletedTask, ledger)
        ledger.addAll(step2)
        assertEquals(0, ledger.sumOf { it.amount })

        // 3. Complete again -> +35 + 5 = +40
        val step3 = XpEvaluator.evaluateTaskCompletion(task, ledger)
        ledger.addAll(step3)
        assertEquals(40, ledger.sumOf { it.amount })
    }

    @Test
    fun testRepeatedSameStateUpdateNoDuplicateXp() {
        val completedTask = ActivityTask(
            id = "task-400",
            title = "Idempotency Test",
            priority = Priority.Low,
            isCompleted = true,
            dueDate = 2000L,
            completedAt = 1000L
        )

        val ledger = mutableListOf<XpEvent>()

        // First call
        val step1 = XpEvaluator.evaluateTaskCompletion(completedTask, ledger)
        ledger.addAll(step1)

        // Second call with same state
        val step2 = XpEvaluator.evaluateTaskCompletion(completedTask, ledger)
        assertTrue(step2.isEmpty())
        assertEquals(15, ledger.sumOf { it.amount })

        // Uncomplete repeatedly
        val uncompletedTask = completedTask.copy(isCompleted = false)
        val step3 = XpEvaluator.evaluateTaskCompletion(uncompletedTask, ledger)
        ledger.addAll(step3)
        assertEquals(0, ledger.sumOf { it.amount })

        val step4 = XpEvaluator.evaluateTaskCompletion(uncompletedTask, ledger)
        assertTrue(step4.isEmpty())
        assertEquals(0, ledger.sumOf { it.amount })
    }

    @Test
    fun testPerfectDayAndHighScoreReversals() {
        val date = "2026-08-23"
        val task1 = ActivityTask(id = "p1", title = "P1", isCompleted = true)
        val task2 = ActivityTask(id = "p2", title = "P2", isCompleted = true)

        var dateLedger = mutableListOf<XpEvent>()

        // Perfect Day + High Score 98 (+30 Perfect Day, +40 High Score)
        val initialBonuses = XpEvaluator.evaluateDailyBonuses(
            date = date,
            tasksForDay = listOf(task1, task2),
            dailyScore = 98,
            existingEventsForDate = dateLedger
        )
        dateLedger.addAll(initialBonuses)
        assertEquals(70, dateLedger.sumOf { it.amount })

        // Task 2 uncompleted -> Perfect day broken, score drops to 50
        val uncompletedTask2 = task2.copy(isCompleted = false)
        val reversalBonuses = XpEvaluator.evaluateDailyBonuses(
            date = date,
            tasksForDay = listOf(task1, uncompletedTask2),
            dailyScore = 50,
            existingEventsForDate = dateLedger
        )
        dateLedger.addAll(reversalBonuses)

        val pdReversal = reversalBonuses.find { it.eventType == XpEvaluator.EVENT_PERFECT_DAY_REVERSAL }
        val hsReversal = reversalBonuses.find { it.eventType == XpEvaluator.EVENT_HIGH_SCORE_REVERSAL }

        assertEquals(-30, pdReversal?.amount)
        assertEquals(-40, hsReversal?.amount)
        assertEquals(0, dateLedger.sumOf { it.amount })
    }

    @Test
    fun testLevelUpFollowedByUncomplete() {
        val ledger = mutableListOf<XpEvent>()

        // Total XP 80 -> Level 1
        ledger.add(XpEvent(amount = 80, eventType = "TASK_COMPLETED", taskId = "t1", date = "2026-08-23"))
        var levelInfo = LevelCalculator.calculateLevelInfo(ledger.sumOf { it.amount })
        assertEquals(1, levelInfo.level)

        // Award +30 XP -> Total 110 XP -> Level 2
        ledger.add(XpEvent(amount = 30, eventType = "TASK_COMPLETED", taskId = "t2", date = "2026-08-23"))
        levelInfo = LevelCalculator.calculateLevelInfo(ledger.sumOf { it.amount })
        assertEquals(2, levelInfo.level)

        // Reversal -30 XP -> Total 80 XP -> Drops back to Level 1
        ledger.add(XpEvent(amount = -30, eventType = "TASK_UNCOMPLETED", taskId = "t2", date = "2026-08-23"))
        levelInfo = LevelCalculator.calculateLevelInfo(ledger.sumOf { it.amount })
        assertEquals(1, levelInfo.level)
        assertEquals(80, levelInfo.currentLevelXp)
    }

    @Test
    fun testNoNegativeXpAndLevelCoercion() {
        val ledger = listOf(
            XpEvent(amount = -50, eventType = "TASK_UNCOMPLETED", date = "2026-08-23")
        )

        val rawXp = ledger.sumOf { it.amount }
        assertEquals(-50, rawXp)

        val levelInfo = LevelCalculator.calculateLevelInfo(rawXp)
        assertEquals(1, levelInfo.level)
        assertEquals(0, levelInfo.totalXp)
        assertEquals(0, levelInfo.currentLevelXp)
        assertEquals(0f, levelInfo.progress, 0.001f)
    }

    @Test
    fun testFreshXpLedgerStartsAtZero() {
        val emptyLedger = emptyList<XpEvent>()
        val totalXp = emptyLedger.sumOf { it.amount }
        assertEquals(0, totalXp)

        val levelInfo = LevelCalculator.calculateLevelInfo(totalXp)
        assertEquals(1, levelInfo.level)
        assertEquals(0, levelInfo.totalXp)
        assertEquals(0, levelInfo.currentLevelXp)
        assertEquals(100, levelInfo.requiredXpForNextLevel)
        assertEquals(0f, levelInfo.progress, 0.001f)
    }

    @Test
    fun testOnePlus20XpEvent() {
        val ledger = listOf(
            XpEvent(amount = 20, eventType = XpEvaluator.EVENT_TASK_COMPLETED, date = "2026-08-23")
        )
        val totalXp = ledger.sumOf { it.amount }
        assertEquals(20, totalXp)

        val levelInfo = LevelCalculator.calculateLevelInfo(totalXp)
        assertEquals(1, levelInfo.level)
        assertEquals(20, levelInfo.totalXp)
        assertEquals(20, levelInfo.currentLevelXp)
        assertEquals(100, levelInfo.requiredXpForNextLevel)
        assertEquals(0.2f, levelInfo.progress, 0.001f)
    }

    @Test
    fun testPlus20ThenMinus20Reversal() {
        val ledger = listOf(
            XpEvent(amount = 20, eventType = XpEvaluator.EVENT_TASK_COMPLETED, date = "2026-08-23"),
            XpEvent(amount = -20, eventType = XpEvaluator.EVENT_TASK_UNCOMPLETED, date = "2026-08-23")
        )
        val totalXp = ledger.sumOf { it.amount }
        assertEquals(0, totalXp)

        val levelInfo = LevelCalculator.calculateLevelInfo(totalXp)
        assertEquals(1, levelInfo.level)
        assertEquals(0, levelInfo.totalXp)
        assertEquals(0, levelInfo.currentLevelXp)
        assertEquals(100, levelInfo.requiredXpForNextLevel)
        assertEquals(0f, levelInfo.progress, 0.001f)
    }

    @Test
    fun testEmptyDatabaseProducesLevel1AndZeroXp() {
        val emptyXpEvents = emptyList<XpEvent>()
        val levelInfo = LevelCalculator.calculateLevelInfo(emptyXpEvents.sumOf { it.amount })
        assertEquals(1, levelInfo.level)
        assertEquals(0, levelInfo.totalXp)
        assertEquals(0, levelInfo.currentLevelXp)
        assertEquals(100, levelInfo.requiredXpForNextLevel)
        assertEquals(0f, levelInfo.progress, 0.001f)
    }

    @Test
    fun testNoXpCreatedJustByUncompletedTasksOrAppLaunch() {
        val uncompletedTask = ActivityTask(
            id = "launch-1",
            title = "Uncompleted Task",
            priority = Priority.Medium,
            isCompleted = false
        )
        val newEvents = XpEvaluator.evaluateTaskCompletion(uncompletedTask, emptyList())
        assertTrue(newEvents.isEmpty())

        val dailyBonuses = XpEvaluator.evaluateDailyBonuses(
            date = "2026-08-23",
            tasksForDay = listOf(uncompletedTask),
            dailyScore = 0,
            existingEventsForDate = emptyList()
        )
        assertTrue(dailyBonuses.isEmpty())
    }
}
