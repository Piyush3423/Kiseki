package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.DailyScore
import com.example.data.entity.PersonalBest
import com.example.data.entity.XpEvent
import com.example.data.model.Priority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PersonalBestEvaluationResult(
    val isNewRecord: Boolean,
    val record: PersonalBest?,
    val previousValue: Int
)

object PersonalBestEvaluator {

    const val KEY_MOST_TASKS = "MOST_TASKS_COMPLETED"
    const val KEY_HIGHEST_SCORE = "HIGHEST_DAILY_SCORE"
    const val KEY_MOST_XP = "MOST_XP_IN_DAY"
    const val KEY_LONGEST_STREAK = "LONGEST_STREAK"
    const val KEY_MOST_HIGH_PRIORITY = "MOST_HIGH_PRIORITY_COMPLETED"

    fun getRecordTitle(key: String): String {
        return when (key) {
            KEY_MOST_TASKS -> "Most Tasks"
            KEY_HIGHEST_SCORE -> "Highest Daily Score"
            KEY_MOST_XP -> "Best XP Day"
            KEY_LONGEST_STREAK -> "Longest Streak"
            KEY_MOST_HIGH_PRIORITY -> "Most High Priority"
            else -> "Personal Best"
        }
    }

    fun formatRecordValue(key: String, value: Int): String {
        return when (key) {
            KEY_MOST_TASKS -> "$value tasks completed"
            KEY_HIGHEST_SCORE -> "$value Daily Score"
            KEY_MOST_XP -> "$value XP"
            KEY_LONGEST_STREAK -> "$value days streak"
            KEY_MOST_HIGH_PRIORITY -> "$value high-priority tasks"
            else -> "$value"
        }
    }

    /**
     * Evaluates a candidate value against an existing stored record.
     * Tied or lower values do NOT trigger a new record.
     * A new record occurs strictly when newValue > (currentRecord?.value ?: 0) and newValue > 0.
     */
    fun evaluateRecord(
        recordKey: String,
        newValue: Int,
        dateStr: String,
        currentRecord: PersonalBest?
    ): PersonalBestEvaluationResult {
        val prevValue = currentRecord?.value ?: 0
        return if (newValue > prevValue && newValue > 0) {
            PersonalBestEvaluationResult(
                isNewRecord = true,
                record = PersonalBest(
                    recordKey = recordKey,
                    value = newValue,
                    dateAchieved = dateStr,
                    previousValue = prevValue,
                    acknowledged = false
                ),
                previousValue = prevValue
            )
        } else {
            PersonalBestEvaluationResult(
                isNewRecord = false,
                record = currentRecord,
                previousValue = prevValue
            )
        }
    }

    /**
     * Calculates all historical records from existing database history.
     */
    fun calculateHistoricalPersonalBests(
        tasks: List<ActivityTask>,
        dailyScores: List<DailyScore>,
        xpEvents: List<XpEvent>,
        todayStr: String = LocalDate.now().toString()
    ): Map<String, PersonalBest> {
        val result = mutableMapOf<String, PersonalBest>()
        val zoneId = ZoneId.systemDefault()

        // 1. Most tasks completed in one day
        val completedTasks = tasks.filter { it.isCompleted && it.completedAt != null }
        val tasksByDay = completedTasks.groupBy { task ->
            Instant.ofEpochMilli(task.completedAt!!).atZone(zoneId).toLocalDate().toString()
        }
        var maxTaskCount = 0
        var maxTaskDate = todayStr
        tasksByDay.forEach { (date, dayTasks) ->
            if (dayTasks.size > maxTaskCount) {
                maxTaskCount = dayTasks.size
                maxTaskDate = date
            }
        }
        if (maxTaskCount > 0) {
            result[KEY_MOST_TASKS] = PersonalBest(
                recordKey = KEY_MOST_TASKS,
                value = maxTaskCount,
                dateAchieved = maxTaskDate,
                previousValue = 0,
                acknowledged = true
            )
        }

        // 2. Highest Daily Score
        val bestScoreObj = dailyScores.filter { it.score > 0 }.maxByOrNull { it.score }
        if (bestScoreObj != null) {
            result[KEY_HIGHEST_SCORE] = PersonalBest(
                recordKey = KEY_HIGHEST_SCORE,
                value = bestScoreObj.score,
                dateAchieved = bestScoreObj.date,
                previousValue = 0,
                acknowledged = true
            )
        }

        // 3. Most XP earned in one day
        val xpByDate = xpEvents.groupBy { it.date }
        var maxXp = 0
        var maxXpDate = todayStr
        xpByDate.forEach { (date, events) ->
            val totalXpForDay = events.sumOf { it.amount }
            if (totalXpForDay > maxXp) {
                maxXp = totalXpForDay
                maxXpDate = date
            }
        }
        if (maxXp > 0) {
            result[KEY_MOST_XP] = PersonalBest(
                recordKey = KEY_MOST_XP,
                value = maxXp,
                dateAchieved = maxXpDate,
                previousValue = 0,
                acknowledged = true
            )
        }

        // 4. Longest productive streak
        val completedDatesSet = completedTasks
            .map { Instant.ofEpochMilli(it.completedAt!!).atZone(zoneId).toLocalDate() }
            .toSet()

        val longestStreakVal = calculateLongestStreak(completedDatesSet)
        if (longestStreakVal > 0) {
            result[KEY_LONGEST_STREAK] = PersonalBest(
                recordKey = KEY_LONGEST_STREAK,
                value = longestStreakVal,
                dateAchieved = todayStr,
                previousValue = 0,
                acknowledged = true
            )
        }

        // 5. Most high-priority tasks completed in one day
        val highPriorityTasks = completedTasks.filter { it.priority == Priority.High }
        val hpTasksByDay = highPriorityTasks.groupBy { task ->
            Instant.ofEpochMilli(task.completedAt!!).atZone(zoneId).toLocalDate().toString()
        }
        var maxHpCount = 0
        var maxHpDate = todayStr
        hpTasksByDay.forEach { (date, dayTasks) ->
            if (dayTasks.size > maxHpCount) {
                maxHpCount = dayTasks.size
                maxHpDate = date
            }
        }
        if (maxHpCount > 0) {
            result[KEY_MOST_HIGH_PRIORITY] = PersonalBest(
                recordKey = KEY_MOST_HIGH_PRIORITY,
                value = maxHpCount,
                dateAchieved = maxHpDate,
                previousValue = 0,
                acknowledged = true
            )
        }

        return result
    }

    fun calculateLongestStreak(completedDates: Set<LocalDate>): Int {
        if (completedDates.isEmpty()) return 0
        val sortedDates = completedDates.sorted()
        var maxStreak = 0
        var currentStreak = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (prevDate == null || date == prevDate.plusDays(1)) {
                currentStreak++
            } else {
                currentStreak = 1
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
            prevDate = date
        }
        return maxStreak
    }
}
