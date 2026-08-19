package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.XpEvent
import com.example.data.model.Priority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object XpEvaluator {

    fun getTaskXpAmount(priority: Priority): Int {
        return when (priority) {
            Priority.Low -> 10
            Priority.Medium -> 20
            Priority.High -> 35
            else -> 10
        }
    }

    fun isTaskOnTime(task: ActivityTask): Boolean {
        if (!task.isCompleted) return false
        val dueDate = task.dueDate ?: return true
        val completedAt = task.completedAt ?: return true
        return completedAt <= dueDate
    }

    fun getTaskDateStr(task: ActivityTask): String {
        val zoneId = ZoneId.systemDefault()
        if (task.dueDate != null) {
            return Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate().toString()
        }
        if (task.completedAt != null) {
            return Instant.ofEpochMilli(task.completedAt).atZone(zoneId).toLocalDate().toString()
        }
        return LocalDate.now().toString()
    }

    fun evaluateTaskCompletion(
        task: ActivityTask,
        existingEventsForTask: List<XpEvent>
    ): List<XpEvent> {
        if (!task.isCompleted) return emptyList()

        val newEvents = mutableListOf<XpEvent>()
        val dateStr = getTaskDateStr(task)

        // 1. Task Completed XP
        val hasCompletedEvent = existingEventsForTask.any { it.eventType == "TASK_COMPLETED" }
        if (!hasCompletedEvent) {
            val amount = getTaskXpAmount(task.priority)
            newEvents.add(
                XpEvent(
                    amount = amount,
                    eventType = "TASK_COMPLETED",
                    taskId = task.id,
                    date = dateStr
                )
            )
        }

        // 2. On-Time Bonus (+5 XP)
        if (isTaskOnTime(task)) {
            val hasOnTimeEvent = existingEventsForTask.any { it.eventType == "ON_TIME_BONUS" }
            if (!hasOnTimeEvent) {
                newEvents.add(
                    XpEvent(
                        amount = 5,
                        eventType = "ON_TIME_BONUS",
                        taskId = task.id,
                        date = dateStr
                    )
                )
            }
        }

        return newEvents
    }

    fun evaluateDailyBonuses(
        date: String,
        tasksForDay: List<ActivityTask>,
        dailyScore: Int,
        existingEventsForDate: List<XpEvent>
    ): List<XpEvent> {
        val newEvents = mutableListOf<XpEvent>()

        // 1. Perfect Day (+30 XP)
        if (tasksForDay.isNotEmpty() && tasksForDay.all { it.isCompleted }) {
            val hasPerfectDay = existingEventsForDate.any { it.eventType == "PERFECT_DAY" }
            if (!hasPerfectDay) {
                newEvents.add(
                    XpEvent(
                        amount = 30,
                        eventType = "PERFECT_DAY",
                        date = date
                    )
                )
            }
        }

        // 2. High Score Day
        val existingHighScoreEvent = existingEventsForDate.find { it.eventType == "HIGH_SCORE_DAY" }
        if (dailyScore >= 95) {
            if (existingHighScoreEvent == null) {
                newEvents.add(
                    XpEvent(
                        amount = 40,
                        eventType = "HIGH_SCORE_DAY",
                        date = date
                    )
                )
            } else if (existingHighScoreEvent.amount < 40) {
                newEvents.add(
                    XpEvent(
                        amount = 40 - existingHighScoreEvent.amount, // Delta +20
                        eventType = "HIGH_SCORE_DAY",
                        date = date
                    )
                )
            }
        } else if (dailyScore >= 80) {
            if (existingHighScoreEvent == null) {
                newEvents.add(
                    XpEvent(
                        amount = 20,
                        eventType = "HIGH_SCORE_DAY",
                        date = date
                    )
                )
            }
        }

        return newEvents
    }

    const val EVENT_FOCUS_BONUS = "FOCUS_BONUS"
    const val MAX_DAILY_FOCUS_XP = 20
    const val FOCUS_SESSION_MIN_MS_FOR_XP = 25 * 60 * 1000L // 25 minutes

    fun evaluateFocusSessionXp(
        durationMs: Long,
        taskId: String,
        date: String,
        existingFocusEventsToday: List<XpEvent>
    ): XpEvent? {
        if (durationMs < FOCUS_SESSION_MIN_MS_FOR_XP) return null
        val currentTotalToday = existingFocusEventsToday.sumOf { it.amount }
        if (currentTotalToday >= MAX_DAILY_FOCUS_XP) return null

        val awardAmount = minOf(5, MAX_DAILY_FOCUS_XP - currentTotalToday)
        if (awardAmount <= 0) return null

        return XpEvent(
            amount = awardAmount,
            eventType = EVENT_FOCUS_BONUS,
            taskId = taskId,
            date = date
        )
    }
}
