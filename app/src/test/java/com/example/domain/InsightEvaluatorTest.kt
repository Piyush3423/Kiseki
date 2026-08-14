package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.DailyScore
import com.example.data.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class InsightEvaluatorTest {

    private val baseDate = LocalDate.of(2026, 8, 10) // Monday
    private val zoneId = ZoneId.of("UTC")

    private fun createScore(dateStr: String, score: Int) = DailyScore(
        date = dateStr,
        score = score,
        completionScore = 0f,
        priorityPerformance = 0f,
        onTimeScore = 0f,
        consistencyScore = 0f
    )

    @Test
    fun testWeekOverWeekProductivity_Improvement() {
        val scoresMap = mutableMapOf<String, DailyScore>()

        // Current week (0..6 days ago): avg score = 80 (5 active days)
        for (i in 0..4) {
            val dateStr = baseDate.minusDays(i.toLong()).toString()
            scoresMap[dateStr] = createScore(dateStr, 80)
        }

        // Prior week (7..13 days ago): avg score = 60 (5 active days)
        for (i in 7..11) {
            val dateStr = baseDate.minusDays(i.toLong()).toString()
            scoresMap[dateStr] = createScore(dateStr, 60)
        }

        val insight = InsightEvaluator.evaluateWeekOverWeekProductivity(scoresMap, baseDate)

        assertNotNull(insight)
        assertEquals(InsightType.MAJOR_IMPROVEMENT, insight!!.type)
        assertTrue(insight.title.contains("33%"))
        assertEquals("Compared with last week", insight.period)
    }

    @Test
    fun testWeekOverWeekProductivity_Decline() {
        val scoresMap = mutableMapOf<String, DailyScore>()

        // Current week: avg score = 50 (5 active days)
        for (i in 0..4) {
            val dateStr = baseDate.minusDays(i.toLong()).toString()
            scoresMap[dateStr] = createScore(dateStr, 50)
        }

        // Prior week: avg score = 80 (5 active days)
        for (i in 7..11) {
            val dateStr = baseDate.minusDays(i.toLong()).toString()
            scoresMap[dateStr] = createScore(dateStr, 80)
        }

        val insight = InsightEvaluator.evaluateWeekOverWeekProductivity(scoresMap, baseDate)

        assertNotNull(insight)
        assertEquals(InsightType.MAJOR_DECLINE, insight!!.type)
        assertTrue(insight.title.contains("37%") || insight.title.contains("38%"))
        assertEquals("Compared with last week", insight.period)
    }

    @Test
    fun testWeekOverWeekProductivity_InsufficientData() {
        val scoresMap = mutableMapOf<String, DailyScore>()

        // Only 2 active days in current week
        for (i in 0..1) {
            val dateStr = baseDate.minusDays(i.toLong()).toString()
            scoresMap[dateStr] = createScore(dateStr, 80)
        }

        // 5 active days in prior week
        for (i in 7..11) {
            val dateStr = baseDate.minusDays(i.toLong()).toString()
            scoresMap[dateStr] = createScore(dateStr, 60)
        }

        val insight = InsightEvaluator.evaluateWeekOverWeekProductivity(scoresMap, baseDate)

        assertNull(insight)
    }

    @Test
    fun testConsecutiveWeeksImprovement() {
        val scoresMap = mutableMapOf<String, DailyScore>()

        // Week 1 (days 0..6): avg 85 (5 active days)
        for (i in 0..4) {
            val d = baseDate.minusDays(i.toLong()).toString()
            scoresMap[d] = createScore(d, 85)
        }
        // Week 2 (days 7..13): avg 70 (5 active days)
        for (i in 7..11) {
            val d = baseDate.minusDays(i.toLong()).toString()
            scoresMap[d] = createScore(d, 70)
        }
        // Week 3 (days 14..20): avg 55 (5 active days)
        for (i in 14..18) {
            val d = baseDate.minusDays(i.toLong()).toString()
            scoresMap[d] = createScore(d, 55)
        }

        val insight = InsightEvaluator.evaluateConsecutiveWeeksImprovement(scoresMap, baseDate)

        assertNotNull(insight)
        assertEquals(InsightType.MAJOR_IMPROVEMENT, insight!!.type)
        assertTrue(insight.title.contains("3 consecutive weeks"))
        assertEquals("Last 3 weeks", insight.period)
    }

    @Test
    fun testCategoryTrends_Increase() {
        val tasks = mutableListOf<ActivityTask>()
        val category = "Study"

        for (i in 1..6) {
            tasks.add(ActivityTask(title = "Task $i", category = category))
        }

        val currTime = ZonedDateTime.of(2026, 8, 9, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        val prevTime = ZonedDateTime.of(2026, 8, 2, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()

        for (i in 0..4) {
            tasks.add(
                ActivityTask(
                    title = "Study Current $i",
                    category = category,
                    isCompleted = true,
                    completedAt = currTime
                )
            )
        }

        for (i in 0..1) {
            tasks.add(
                ActivityTask(
                    title = "Study Prev $i",
                    category = category,
                    isCompleted = true,
                    completedAt = prevTime
                )
            )
        }

        val insights = InsightEvaluator.evaluateCategoryTrends(tasks, baseDate, zoneId)

        assertTrue(insights.isNotEmpty())
        val item = insights.first { it.title.contains("Study") }
        assertEquals(InsightType.MAJOR_IMPROVEMENT, item.type)
        assertTrue(item.title.contains("150% more Study tasks"))
    }

    @Test
    fun testDayOfWeekAnalysis_WeakestDay() {
        val scoresMap = mutableMapOf<String, DailyScore>()

        for (dayOffset in 0..27) {
            val date = baseDate.minusDays(dayOffset.toLong())
            val scoreVal = if (date.dayOfWeek == DayOfWeek.TUESDAY) 40 else 80
            scoresMap[date.toString()] = createScore(date.toString(), scoreVal)
        }

        val insights = InsightEvaluator.evaluateDayOfWeekAnalysis(scoresMap, baseDate)

        assertTrue(insights.isNotEmpty())
        val weakestInsight = insights.first { it.title.contains("Tuesday") }
        assertEquals(InsightType.ACTIONABLE_OBSERVATION, weakestInsight.type)
        assertTrue(weakestInsight.title.contains("Tuesday is currently your weakest day"))
    }

    @Test
    fun testPriorityConsistency_HighPriorityBetter() {
        val tasks = mutableListOf<ActivityTask>()

        for (i in 1..6) {
            tasks.add(
                ActivityTask(
                    title = "High Task $i",
                    priority = Priority.High,
                    isCompleted = i <= 5
                )
            )
        }

        for (i in 1..6) {
            tasks.add(
                ActivityTask(
                    title = "Med Task $i",
                    priority = Priority.Medium,
                    isCompleted = i <= 2
                )
            )
        }

        val insight = InsightEvaluator.evaluatePriorityConsistency(tasks)

        assertNotNull(insight)
        assertEquals(InsightType.REPEATED_PATTERN, insight!!.type)
        assertTrue(insight.title.contains("High-priority tasks are completed more consistently"))
    }

    @Test
    fun testTimeOfDayAnalysis_EveningSkipped() {
        val tasks = mutableListOf<ActivityTask>()

        val dayTimeMillis = ZonedDateTime.of(2026, 8, 9, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        for (i in 1..6) {
            tasks.add(
                ActivityTask(
                    title = "Day Task $i",
                    dueDate = dayTimeMillis,
                    isCompleted = i <= 5
                )
            )
        }

        val eveningTimeMillis = ZonedDateTime.of(2026, 8, 9, 20, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        for (i in 1..6) {
            tasks.add(
                ActivityTask(
                    title = "Evening Task $i",
                    dueDate = eveningTimeMillis,
                    isCompleted = i <= 2
                )
            )
        }

        val insight = InsightEvaluator.evaluateTimeOfDayAnalysis(tasks, zoneId)

        assertNotNull(insight)
        assertEquals(InsightType.ACTIONABLE_OBSERVATION, insight!!.type)
        assertTrue(insight.title.contains("Tasks scheduled in the evening are skipped more often"))
    }

    @Test
    fun testEvaluateInsights_LimitToTop5() {
        val tasks = mutableListOf<ActivityTask>()
        val scoresMap = mutableMapOf<String, DailyScore>()

        for (dayOffset in 0..27) {
            val date = baseDate.minusDays(dayOffset.toLong())
            val scoreVal = if (date.dayOfWeek == DayOfWeek.TUESDAY) 30 else 80
            scoresMap[date.toString()] = createScore(date.toString(), scoreVal)
        }

        val results = InsightEvaluator.evaluateInsights(
            tasks = tasks,
            dailyScores = scoresMap.values.toList(),
            now = baseDate,
            zoneId = zoneId
        )

        assertTrue(results.size <= 5)
    }
}
