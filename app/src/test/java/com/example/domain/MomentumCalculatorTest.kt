package com.example.domain

import com.example.data.entity.DailyScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MomentumCalculatorTest {

    private val baseDate = LocalDate.of(2026, 8, 9)

    @Test
    fun test14PerfectDays() {
        val scores = (0..20).map { offset ->
            val dateStr = baseDate.minusDays(offset.toLong()).toString()
            DailyScore(date = dateStr, score = 100, completionScore = 1f, priorityPerformance = 1f, onTimeScore = 1f, consistencyScore = 1f)
        }

        val result = MomentumCalculator.calculate(
            tasks = emptyList(),
            dailyScores = scores,
            today = baseDate
        )

        assertTrue(result.hasEnoughHistory)
        assertEquals(100, result.currentMomentum)
        assertEquals(100, result.lastWeekMomentum)
        assertEquals(0, result.change)
        assertEquals(MomentumTrend.STABLE, result.trend)
    }

    @Test
    fun testMixedDays() {
        val scores = (0..13).map { offset ->
            val dateStr = baseDate.minusDays(offset.toLong()).toString()
            val scoreVal = if (offset % 2 == 0) 100 else 50
            DailyScore(date = dateStr, score = scoreVal, completionScore = 0.5f, priorityPerformance = 0.5f, onTimeScore = 0.5f, consistencyScore = 0.5f)
        }

        val result = MomentumCalculator.calculate(
            tasks = emptyList(),
            dailyScores = scores,
            today = baseDate
        )

        assertTrue(result.hasEnoughHistory)
        assertTrue("Momentum should be between 50 and 100", result.currentMomentum in 51..99)
    }

    @Test
    fun testMissedDayDoesNotResetToZero() {
        val scores = (1..13).map { offset ->
            val dateStr = baseDate.minusDays(offset.toLong()).toString()
            DailyScore(date = dateStr, score = 100, completionScore = 1f, priorityPerformance = 1f, onTimeScore = 1f, consistencyScore = 1f)
        } + DailyScore(date = baseDate.toString(), score = 0, completionScore = 0f, priorityPerformance = 0f, onTimeScore = 0f, consistencyScore = 0f)

        val result = MomentumCalculator.calculate(
            tasks = emptyList(),
            dailyScores = scores,
            today = baseDate
        )

        assertTrue(result.hasEnoughHistory)
        assertTrue("Momentum should gradually reduce, not reset to zero", result.currentMomentum > 80)
        assertEquals("Momentum expected ~91%", 91, result.currentMomentum)
    }

    @Test
    fun testInsufficientHistory() {
        val result = MomentumCalculator.calculate(
            tasks = emptyList(),
            dailyScores = emptyList(),
            today = baseDate
        )

        assertFalse(result.hasEnoughHistory)
        assertEquals(0, result.currentMomentum)
        assertEquals(0, result.lastWeekMomentum)
        assertEquals(0, result.change)
        assertEquals(MomentumTrend.STABLE, result.trend)
    }

    @Test
    fun testImprovingTrend() {
        val scores = (0..20).map { offset ->
            val dateStr = baseDate.minusDays(offset.toLong()).toString()
            val scoreVal = if (offset <= 6) 100 else 20
            DailyScore(date = dateStr, score = scoreVal, completionScore = 1f, priorityPerformance = 1f, onTimeScore = 1f, consistencyScore = 1f)
        }

        val result = MomentumCalculator.calculate(
            tasks = emptyList(),
            dailyScores = scores,
            today = baseDate
        )

        assertTrue(result.hasEnoughHistory)
        assertTrue("Change should be positive > +2%", result.change > 2)
        assertEquals(MomentumTrend.IMPROVING, result.trend)
    }

    @Test
    fun testDecliningTrend() {
        val scores = (0..20).map { offset ->
            val dateStr = baseDate.minusDays(offset.toLong()).toString()
            val scoreVal = if (offset <= 6) 10 else 90
            DailyScore(date = dateStr, score = scoreVal, completionScore = 0.1f, priorityPerformance = 0.1f, onTimeScore = 0.1f, consistencyScore = 0.1f)
        }

        val result = MomentumCalculator.calculate(
            tasks = emptyList(),
            dailyScores = scores,
            today = baseDate
        )

        assertTrue(result.hasEnoughHistory)
        assertTrue("Change should be negative < -2%", result.change < -2)
        assertEquals(MomentumTrend.DECLINING, result.trend)
    }
}
