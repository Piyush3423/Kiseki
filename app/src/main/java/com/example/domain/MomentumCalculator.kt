package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.entity.DailyScore
import java.time.LocalDate
import kotlin.math.roundToInt

enum class MomentumTrend {
    IMPROVING, // ↑
    STABLE,    // →
    DECLINING  // ↓
}

data class MomentumResult(
    val currentMomentum: Int, // 0..100
    val lastWeekMomentum: Int, // 0..100
    val change: Int, // percentage point difference
    val trend: MomentumTrend,
    val hasEnoughHistory: Boolean,
    val dailyMomentumHistory: List<Pair<String, Int>> = emptyList()
)

object MomentumCalculator {

    fun calculate(
        tasks: List<ActivityTask>,
        dailyScores: List<DailyScore>,
        today: LocalDate = LocalDate.now()
    ): MomentumResult {
        val hasEnough = tasks.isNotEmpty() || dailyScores.isNotEmpty()
        if (!hasEnough) {
            return MomentumResult(
                currentMomentum = 0,
                lastWeekMomentum = 0,
                change = 0,
                trend = MomentumTrend.STABLE,
                hasEnoughHistory = false,
                dailyMomentumHistory = emptyList()
            )
        }

        val computedScores = DailyScoreCalculator.calculateAllScores(tasks).associateBy { it.date }
        val storedScores = dailyScores.associateBy { it.date }

        val allDates = computedScores.keys + storedScores.keys
        val scoresMap = mutableMapOf<String, Int>()
        allDates.forEach { dateStr ->
            val score = storedScores[dateStr]?.score ?: computedScores[dateStr]?.score ?: 0
            scoresMap[dateStr] = score
        }

        val currentMomentum = calculateMomentumForDate(today, scoresMap)
        val lastWeekMomentum = calculateMomentumForDate(today.minusDays(7), scoresMap)
        val change = currentMomentum - lastWeekMomentum

        val trend = when {
            change > 2 -> MomentumTrend.IMPROVING
            change < -2 -> MomentumTrend.DECLINING
            else -> MomentumTrend.STABLE
        }

        val history = (13 downTo 0).map { daysAgo ->
            val d = today.minusDays(daysAgo.toLong())
            val mom = calculateMomentumForDate(d, scoresMap)
            d.toString() to mom
        }

        return MomentumResult(
            currentMomentum = currentMomentum,
            lastWeekMomentum = lastWeekMomentum,
            change = change,
            trend = trend,
            hasEnoughHistory = true,
            dailyMomentumHistory = history
        )
    }

    fun calculateMomentumForDate(
        targetDate: LocalDate,
        scoresMap: Map<String, Int>
    ): Int {
        var weightedSum = 0f
        var totalWeight = 0f

        for (offset in 0..13) {
            val dateStr = targetDate.minusDays(offset.toLong()).toString()
            val weight = getWeightForDayOffset(offset)
            val score = scoresMap[dateStr] ?: 0
            val normalized = (score / 100f).coerceIn(0f, 1f)

            weightedSum += normalized * weight
            totalWeight += weight
        }

        if (totalWeight <= 0f) return 0
        val average = weightedSum / totalWeight
        return (average.coerceIn(0f, 1f) * 100f).roundToInt()
    }

    private fun getWeightForDayOffset(offset: Int): Float {
        return when (offset) {
            0, 1, 2 -> 1.5f   // Days 1-3
            3, 4, 5, 6 -> 1.2f // Days 4-7
            in 7..13 -> 1.0f   // Days 8-14
            else -> 1.0f
        }
    }
}
