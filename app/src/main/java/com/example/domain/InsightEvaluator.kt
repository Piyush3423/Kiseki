package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.DailyScore
import com.example.data.model.Priority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class InsightType {
    MAJOR_IMPROVEMENT,
    MAJOR_DECLINE,
    REPEATED_PATTERN,
    ACTIONABLE_OBSERVATION
}

data class InsightItem(
    val type: InsightType,
    val title: String,
    val description: String,
    val dataCount: Int,
    val period: String,
    val metricChangePercent: Int? = null
)

object InsightEvaluator {

    fun evaluateInsights(
        tasks: List<ActivityTask>,
        dailyScores: List<DailyScore>,
        now: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<InsightItem> {
        val candidates = mutableListOf<InsightItem>()

        val computedScoresMap = DailyScoreCalculator.calculateAllScores(tasks).associateBy { it.date }
        val allScoresMap = mutableMapOf<String, DailyScore>()
        computedScoresMap.forEach { (date, score) -> allScoresMap[date] = score }
        dailyScores.forEach { score -> allScoresMap[score.date] = score }

        evaluateWeekOverWeekProductivity(allScoresMap, now)?.let { candidates.add(it) }

        evaluateConsecutiveWeeksImprovement(allScoresMap, now)?.let { candidates.add(it) }

        evaluateCategoryTrends(tasks, now, zoneId).forEach { candidates.add(it) }

        evaluateDayOfWeekAnalysis(allScoresMap, now).forEach { candidates.add(it) }

        evaluatePriorityConsistency(tasks)?.let { candidates.add(it) }

        evaluateTimeOfDayAnalysis(tasks, zoneId)?.let { candidates.add(it) }

        val sorted = candidates.sortedWith(
            compareBy<InsightItem> { item ->
                when (item.type) {
                    InsightType.MAJOR_IMPROVEMENT -> 0
                    InsightType.MAJOR_DECLINE -> 1
                    InsightType.REPEATED_PATTERN -> 2
                    InsightType.ACTIONABLE_OBSERVATION -> 3
                }
            }.thenByDescending { item ->
                abs(item.metricChangePercent ?: 0)
            }.thenByDescending { item ->
                item.dataCount
            }
        )

        return sorted.take(5)
    }

    // --- Rule 1: Week-over-Week Productivity Comparison ---
    // Minimum threshold: at least 4 active days in both current 7 days and prior 7 days.
    fun evaluateWeekOverWeekProductivity(
        scoresMap: Map<String, DailyScore>,
        now: LocalDate
    ): InsightItem? {
        val currWeekDays = (0..6).map { now.minusDays(it.toLong()) }
        val prevWeekDays = (7..13).map { now.minusDays(it.toLong()) }

        val currWeekScores = currWeekDays.mapNotNull { scoresMap[it.toString()] }.filter { it.score > 0 }
        val prevWeekScores = prevWeekDays.mapNotNull { scoresMap[it.toString()] }.filter { it.score > 0 }

        if (currWeekScores.size < 4 || prevWeekScores.size < 4) return null

        val currAvg = currWeekScores.map { it.score }.average()
        val prevAvg = prevWeekScores.map { it.score }.average()

        if (prevAvg <= 0) return null

        val changePct = (((currAvg - prevAvg) / prevAvg) * 100).roundToInt()

        if (abs(changePct) < 5) return null

        val totalDataCount = currWeekScores.size + prevWeekScores.size

        return if (changePct > 0) {
            InsightItem(
                type = InsightType.MAJOR_IMPROVEMENT,
                title = "Productivity improved $changePct%",
                description = "Your average Daily Score increased to ${currAvg.roundToInt()} (up from ${prevAvg.roundToInt()} last week).",
                dataCount = totalDataCount,
                period = "Compared with last week",
                metricChangePercent = changePct
            )
        } else {
            InsightItem(
                type = InsightType.MAJOR_DECLINE,
                title = "Productivity dropped ${abs(changePct)}%",
                description = "Your average Daily Score decreased to ${currAvg.roundToInt()} (down from ${prevAvg.roundToInt()} last week).",
                dataCount = totalDataCount,
                period = "Compared with last week",
                metricChangePercent = changePct
            )
        }
    }

    // --- Rule 2: Consecutive Weeks Score Improvement ---
    // Minimum threshold: at least 4 active days in each of the last 3 consecutive 7-day weeks.
    fun evaluateConsecutiveWeeksImprovement(
        scoresMap: Map<String, DailyScore>,
        now: LocalDate
    ): InsightItem? {
        val week1Days = (0..6).map { now.minusDays(it.toLong()) }
        val week2Days = (7..13).map { now.minusDays(it.toLong()) }
        val week3Days = (14..20).map { now.minusDays(it.toLong()) }

        val w1Scores = week1Days.mapNotNull { scoresMap[it.toString()] }.filter { it.score > 0 }
        val w2Scores = week2Days.mapNotNull { scoresMap[it.toString()] }.filter { it.score > 0 }
        val w3Scores = week3Days.mapNotNull { scoresMap[it.toString()] }.filter { it.score > 0 }

        if (w1Scores.size < 4 || w2Scores.size < 4 || w3Scores.size < 4) return null

        val w1Avg = w1Scores.map { it.score }.average()
        val w2Avg = w2Scores.map { it.score }.average()
        val w3Avg = w3Scores.map { it.score }.average()

        if (w1Avg > w2Avg && w2Avg > w3Avg) {
            val totalIncreasePct = (((w1Avg - w3Avg) / w3Avg) * 100).roundToInt()
            val totalData = w1Scores.size + w2Scores.size + w3Scores.size
            return InsightItem(
                type = InsightType.MAJOR_IMPROVEMENT,
                title = "Your average Daily Score has improved for 3 consecutive weeks",
                description = "Steady progress over 3 weeks: ${w3Avg.roundToInt()} → ${w2Avg.roundToInt()} → ${w1Avg.roundToInt()}.",
                dataCount = totalData,
                period = "Last 3 weeks",
                metricChangePercent = totalIncreasePct
            )
        }
        return null
    }

    // --- Rule 3: Category Completion Trend ---
    // Minimum threshold: at least 5 tasks total in the category.
    fun evaluateCategoryTrends(
        tasks: List<ActivityTask>,
        now: LocalDate,
        zoneId: ZoneId
    ): List<InsightItem> {
        val insights = mutableListOf<InsightItem>()
        val currStart = now.minusDays(6)
        val prevStart = now.minusDays(13)
        val prevEnd = now.minusDays(7)

        val completedTasks = tasks.filter { it.isCompleted && it.completedAt != null }

        val currCategoryCount = mutableMapOf<String, Int>()
        val prevCategoryCount = mutableMapOf<String, Int>()
        val totalCategoryCount = mutableMapOf<String, Int>()

        tasks.forEach { task ->
            val cat = task.category.ifBlank { "General" }
            totalCategoryCount[cat] = (totalCategoryCount[cat] ?: 0) + 1
        }

        completedTasks.forEach { task ->
            val taskDate = Instant.ofEpochMilli(task.completedAt!!).atZone(zoneId).toLocalDate()
            val cat = task.category.ifBlank { "General" }

            if (!taskDate.isBefore(currStart) && !taskDate.isAfter(now)) {
                currCategoryCount[cat] = (currCategoryCount[cat] ?: 0) + 1
            } else if (!taskDate.isBefore(prevStart) && !taskDate.isAfter(prevEnd)) {
                prevCategoryCount[cat] = (prevCategoryCount[cat] ?: 0) + 1
            }
        }

        totalCategoryCount.forEach { (cat, totalCount) ->
            if (totalCount >= 5) {
                val curr = currCategoryCount[cat] ?: 0
                val prev = prevCategoryCount[cat] ?: 0

                if (prev >= 2 && curr != prev) {
                    val changePct = (((curr - prev).toFloat() / prev) * 100).roundToInt()

                    if (changePct >= 15) {
                        insights.add(
                            InsightItem(
                                type = InsightType.MAJOR_IMPROVEMENT,
                                title = "You completed $changePct% more $cat tasks this week",
                                description = "You completed $curr $cat tasks this week compared with $prev last week.",
                                dataCount = curr + prev,
                                period = "Compared with last week",
                                metricChangePercent = changePct
                            )
                        )
                    } else if (changePct <= -15) {
                        insights.add(
                            InsightItem(
                                type = InsightType.MAJOR_DECLINE,
                                title = "$cat task completions dropped ${abs(changePct)}%",
                                description = "You completed $curr $cat tasks this week compared with $prev last week.",
                                dataCount = curr + prev,
                                period = "Compared with last week",
                                metricChangePercent = changePct
                            )
                        )
                    }
                }
            }
        }

        return insights
    }

    // --- Rule 4: Day-of-Week Analysis ---
    // Minimum threshold: at least 3 observations for that weekday.
    fun evaluateDayOfWeekAnalysis(
        scoresMap: Map<String, DailyScore>,
        now: LocalDate
    ): List<InsightItem> {
        val insights = mutableListOf<InsightItem>()

        val daysList = (0..27).map { now.minusDays(it.toLong()) }
        val scoreEntries = daysList.mapNotNull { date ->
            scoresMap[date.toString()]?.takeIf { it.score > 0 }?.let { date to it.score }
        }

        if (scoreEntries.size < 10) return emptyList()

        val byDayOfWeek = scoreEntries.groupBy { it.first.dayOfWeek }

        val validDayAverages = byDayOfWeek.mapNotNull { (dayOfWeek, entries) ->
            if (entries.size >= 3) {
                val avg = entries.map { it.second }.average()
                Triple(dayOfWeek, avg, entries.size)
            } else null
        }

        if (validDayAverages.size < 3) return emptyList()

        val overallAvg = scoreEntries.map { it.second }.average()

        val weakest = validDayAverages.minByOrNull { it.second }
        val strongest = validDayAverages.maxByOrNull { it.second }

        if (weakest != null && (overallAvg - weakest.second) >= 10) {
            val dayName = weakest.first.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val scoreVal = weakest.second.roundToInt()
            insights.add(
                InsightItem(
                    type = InsightType.ACTIONABLE_OBSERVATION,
                    title = "$dayName is currently your weakest day",
                    description = "Average score: $scoreVal (overall average is ${overallAvg.roundToInt()}).",
                    dataCount = weakest.third,
                    period = "Last 4 weeks"
                )
            )
        }

        if (strongest != null && (strongest.second - overallAvg) >= 10 && strongest.first != weakest?.first) {
            val dayName = strongest.first.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val scoreVal = strongest.second.roundToInt()
            insights.add(
                InsightItem(
                    type = InsightType.REPEATED_PATTERN,
                    title = "$dayName is your strongest day",
                    description = "Average score: $scoreVal (overall average is ${overallAvg.roundToInt()}).",
                    dataCount = strongest.third,
                    period = "Last 4 weeks"
                )
            )
        }

        return insights
    }

    // --- Rule 5: Priority Completion Consistency ---
    // Minimum threshold: at least 5 high-priority tasks and at least 5 medium/low-priority tasks.
    fun evaluatePriorityConsistency(tasks: List<ActivityTask>): InsightItem? {
        val highTasks = tasks.filter { it.priority == Priority.High }
        val medLowTasks = tasks.filter { it.priority == Priority.Medium || it.priority == Priority.Low }

        if (highTasks.size < 5 || medLowTasks.size < 5) return null

        val highCompleted = highTasks.count { it.isCompleted }
        val medLowCompleted = medLowTasks.count { it.isCompleted }

        val highRate = highCompleted.toFloat() / highTasks.size
        val medLowRate = medLowCompleted.toFloat() / medLowTasks.size

        val diffPct = ((highRate - medLowRate) * 100).roundToInt()

        return if (diffPct >= 15) {
            InsightItem(
                type = InsightType.REPEATED_PATTERN,
                title = "High-priority tasks are completed more consistently than medium-priority tasks",
                description = "High-priority completion rate is ${(highRate * 100).roundToInt()}% vs ${(medLowRate * 100).roundToInt()}% for medium/low priority tasks.",
                dataCount = highTasks.size + medLowTasks.size,
                period = "Overall task history",
                metricChangePercent = diffPct
            )
        } else if (diffPct <= -15) {
            InsightItem(
                type = InsightType.ACTIONABLE_OBSERVATION,
                title = "High-priority tasks are completed less consistently",
                description = "High-priority completion rate is only ${(highRate * 100).roundToInt()}% vs ${(medLowRate * 100).roundToInt()}% for medium/low priority tasks.",
                dataCount = highTasks.size + medLowTasks.size,
                period = "Overall task history",
                metricChangePercent = diffPct
            )
        } else {
            null
        }
    }

    // --- Rule 6: Time-of-Day Analysis ---
    // Minimum threshold: at least 10 timed tasks.
    fun evaluateTimeOfDayAnalysis(
        tasks: List<ActivityTask>,
        zoneId: ZoneId
    ): InsightItem? {
        val timedTasks = tasks.filter { it.dueDate != null || it.reminderTime != null }
        if (timedTasks.size < 10) return null

        fun getHour(task: ActivityTask): Int {
            val ts = task.reminderTime ?: task.dueDate!!
            return Instant.ofEpochMilli(ts).atZone(zoneId).hour
        }

        val eveningTasks = timedTasks.filter { getHour(it) >= 17 }
        val dayTasks = timedTasks.filter { getHour(it) < 17 }

        if (eveningTasks.size < 4 || dayTasks.size < 4) return null

        val eveningCompleted = eveningTasks.count { it.isCompleted }
        val dayCompleted = dayTasks.count { it.isCompleted }

        val eveningRate = eveningCompleted.toFloat() / eveningTasks.size
        val dayRate = dayCompleted.toFloat() / dayTasks.size

        val diffPct = ((dayRate - eveningRate) * 100).roundToInt()

        if (diffPct >= 15 && eveningRate <= 0.65f) {
            return InsightItem(
                type = InsightType.ACTIONABLE_OBSERVATION,
                title = "Tasks scheduled in the evening are skipped more often",
                description = "Evening task completion rate is ${(eveningRate * 100).roundToInt()}% compared to ${(dayRate * 100).roundToInt()}% for daytime tasks.",
                dataCount = eveningTasks.size + dayTasks.size,
                period = "Based on ${timedTasks.size} timed tasks",
                metricChangePercent = -diffPct
            )
        }

        return null
    }
}
