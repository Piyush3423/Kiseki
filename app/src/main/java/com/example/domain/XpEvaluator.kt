package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.XpEvent
import com.example.data.model.Priority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object XpEvaluator {

    const val EVENT_TASK_COMPLETED = "TASK_COMPLETED"
    const val EVENT_ON_TIME_BONUS = "ON_TIME_BONUS"
    const val EVENT_TASK_UNCOMPLETED = "TASK_UNCOMPLETED"
    const val EVENT_ON_TIME_REVERSAL = "ON_TIME_REVERSAL"

    const val EVENT_PERFECT_DAY = "PERFECT_DAY"
    const val EVENT_PERFECT_DAY_REVERSAL = "PERFECT_DAY_REVERSAL"

    const val EVENT_HIGH_SCORE_DAY = "HIGH_SCORE_DAY"
    const val EVENT_HIGH_SCORE_REVERSAL = "HIGH_SCORE_REVERSAL"

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
        val newEvents = mutableListOf<XpEvent>()
        val dateStr = getTaskDateStr(task)

        val netTaskCompletedXp = existingEventsForTask
            .filter { it.eventType == EVENT_TASK_COMPLETED || it.eventType == EVENT_TASK_UNCOMPLETED }
            .sumOf { it.amount }

        val netOnTimeXp = existingEventsForTask
            .filter { it.eventType == EVENT_ON_TIME_BONUS || it.eventType == EVENT_ON_TIME_REVERSAL }
            .sumOf { it.amount }

        if (task.isCompleted) {
            val expectedAmount = getTaskXpAmount(task.priority)
            if (netTaskCompletedXp <= 0) {
                newEvents.add(
                    XpEvent(
                        amount = expectedAmount,
                        eventType = EVENT_TASK_COMPLETED,
                        taskId = task.id,
                        date = dateStr
                    )
                )
            } else if (netTaskCompletedXp != expectedAmount) {
                val delta = expectedAmount - netTaskCompletedXp
                if (delta > 0) {
                    newEvents.add(
                        XpEvent(
                            amount = delta,
                            eventType = EVENT_TASK_COMPLETED,
                            taskId = task.id,
                            date = dateStr
                        )
                    )
                } else if (delta < 0) {
                    newEvents.add(
                        XpEvent(
                            amount = delta,
                            eventType = EVENT_TASK_UNCOMPLETED,
                            taskId = task.id,
                            date = dateStr
                        )
                    )
                }
            }

            if (isTaskOnTime(task)) {
                if (netOnTimeXp <= 0) {
                    newEvents.add(
                        XpEvent(
                            amount = 5,
                            eventType = EVENT_ON_TIME_BONUS,
                            taskId = task.id,
                            date = dateStr
                        )
                    )
                }
            } else {
                if (netOnTimeXp > 0) {
                    newEvents.add(
                        XpEvent(
                            amount = -netOnTimeXp,
                            eventType = EVENT_ON_TIME_REVERSAL,
                            taskId = task.id,
                            date = dateStr
                        )
                    )
                }
            }
        } else {
            // Task is INCOMPLETE
            if (netTaskCompletedXp > 0) {
                newEvents.add(
                    XpEvent(
                        amount = -netTaskCompletedXp,
                        eventType = EVENT_TASK_UNCOMPLETED,
                        taskId = task.id,
                        date = dateStr
                    )
                )
            }

            if (netOnTimeXp > 0) {
                newEvents.add(
                    XpEvent(
                        amount = -netOnTimeXp,
                        eventType = EVENT_ON_TIME_REVERSAL,
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
        val isPerfectDay = tasksForDay.isNotEmpty() && tasksForDay.all { it.isCompleted }
        val targetPerfectDayXp = if (isPerfectDay) 30 else 0
        val currentPerfectDayXp = existingEventsForDate
            .filter { it.eventType == EVENT_PERFECT_DAY || it.eventType == EVENT_PERFECT_DAY_REVERSAL }
            .sumOf { it.amount }

        val perfectDayDelta = targetPerfectDayXp - currentPerfectDayXp
        if (perfectDayDelta > 0) {
            newEvents.add(
                XpEvent(
                    amount = perfectDayDelta,
                    eventType = EVENT_PERFECT_DAY,
                    date = date
                )
            )
        } else if (perfectDayDelta < 0) {
            newEvents.add(
                XpEvent(
                    amount = perfectDayDelta,
                    eventType = EVENT_PERFECT_DAY_REVERSAL,
                    date = date
                )
            )
        }

        // 2. High Score Day
        val targetHighScoreXp = when {
            dailyScore >= 95 -> 40
            dailyScore >= 80 -> 20
            else -> 0
        }
        val currentHighScoreXp = existingEventsForDate
            .filter { it.eventType == EVENT_HIGH_SCORE_DAY || it.eventType == EVENT_HIGH_SCORE_REVERSAL }
            .sumOf { it.amount }

        val highScoreDelta = targetHighScoreXp - currentHighScoreXp
        if (highScoreDelta > 0) {
            newEvents.add(
                XpEvent(
                    amount = highScoreDelta,
                    eventType = EVENT_HIGH_SCORE_DAY,
                    date = date
                )
            )
        } else if (highScoreDelta < 0) {
            newEvents.add(
                XpEvent(
                    amount = highScoreDelta,
                    eventType = EVENT_HIGH_SCORE_REVERSAL,
                    date = date
                )
            )
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
