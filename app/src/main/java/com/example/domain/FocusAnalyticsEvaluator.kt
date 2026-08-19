package com.example.domain

import com.example.data.entity.FocusSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class FocusAnalyticsData(
    val focusTodayMs: Long,
    val focusWeekMs: Long,
    val averageSessionMs: Long,
    val totalSessionsCount: Int,
    val focusTodayFormatted: String,
    val focusWeekFormatted: String,
    val averageSessionFormatted: String
)

object FocusAnalyticsEvaluator {

    fun calculateAnalytics(
        sessions: List<FocusSession>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FocusAnalyticsData {
        if (sessions.isEmpty()) {
            return FocusAnalyticsData(
                focusTodayMs = 0L,
                focusWeekMs = 0L,
                averageSessionMs = 0L,
                totalSessionsCount = 0,
                focusTodayFormatted = "0m",
                focusWeekFormatted = "0m",
                averageSessionFormatted = "0m"
            )
        }

        val sevenDaysAgo = today.minusDays(6) // today + past 6 days = 7 days

        var todayMs = 0L
        var weekMs = 0L
        var totalMs = 0L

        sessions.forEach { session ->
            val sessionDate = Instant.ofEpochMilli(session.startTime).atZone(zoneId).toLocalDate()
            totalMs += session.duration

            if (sessionDate == today) {
                todayMs += session.duration
            }

            if (!sessionDate.isBefore(sevenDaysAgo) && !sessionDate.isAfter(today)) {
                weekMs += session.duration
            }
        }

        val avgMs = if (sessions.isNotEmpty()) totalMs / sessions.size else 0L

        return FocusAnalyticsData(
            focusTodayMs = todayMs,
            focusWeekMs = weekMs,
            averageSessionMs = avgMs,
            totalSessionsCount = sessions.size,
            focusTodayFormatted = formatDuration(todayMs),
            focusWeekFormatted = formatDuration(weekMs),
            averageSessionFormatted = formatDuration(avgMs)
        )
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0m"
        val totalSeconds = (durationMs + 500) / 1000 // rounded
        val totalMinutes = totalSeconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}
