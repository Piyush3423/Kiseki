package com.example.domain

import com.example.data.entity.FocusSession
import com.example.data.entity.XpEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FocusAnalyticsEvaluatorTest {

    private val zoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 8, 14)

    @Test
    fun testEmptyFocusSessions() {
        val analytics = FocusAnalyticsEvaluator.calculateAnalytics(
            sessions = emptyList(),
            today = today,
            zoneId = zoneId
        )

        assertEquals(0L, analytics.focusTodayMs)
        assertEquals(0L, analytics.focusWeekMs)
        assertEquals(0L, analytics.averageSessionMs)
        assertEquals(0, analytics.totalSessionsCount)
        assertEquals("0m", analytics.focusTodayFormatted)
        assertEquals("0m", analytics.focusWeekFormatted)
        assertEquals("0m", analytics.averageSessionFormatted)
    }

    @Test
    fun testFocusAnalyticsCalculation() {
        // Today timestamp in UTC: 2026-08-14T10:00:00Z -> 1786701600000L approx
        val todayStartEpochMs = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val session1Today = FocusSession(
            id = "1",
            taskId = "task_1",
            startTime = todayStartEpochMs + 3600000L, // 1:00 AM
            endTime = todayStartEpochMs + 3600000L + (45 * 60 * 1000L),
            duration = 45 * 60 * 1000L, // 45 min
            completed = true
        )
        val session2Today = FocusSession(
            id = "2",
            taskId = "task_2",
            startTime = todayStartEpochMs + 7200000L,
            endTime = todayStartEpochMs + 7200000L + (25 * 60 * 1000L),
            duration = 25 * 60 * 1000L, // 25 min
            completed = false
        )

        // 3 days ago (within week)
        val threeDaysAgoEpochMs = today.minusDays(3).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val session3Week = FocusSession(
            id = "3",
            taskId = "task_3",
            startTime = threeDaysAgoEpochMs + 3600000L,
            endTime = threeDaysAgoEpochMs + 3600000L + (60 * 60 * 1000L),
            duration = 60 * 60 * 1000L, // 60 min
            completed = true
        )

        // 10 days ago (outside week)
        val tenDaysAgoEpochMs = today.minusDays(10).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val session4Old = FocusSession(
            id = "4",
            taskId = "task_4",
            startTime = tenDaysAgoEpochMs + 3600000L,
            endTime = tenDaysAgoEpochMs + 3600000L + (30 * 60 * 1000L),
            duration = 30 * 60 * 1000L, // 30 min
            completed = false
        )

        val sessions = listOf(session1Today, session2Today, session3Week, session4Old)

        val analytics = FocusAnalyticsEvaluator.calculateAnalytics(
            sessions = sessions,
            today = today,
            zoneId = zoneId
        )

        // Today: 45m + 25m = 70m (1h 10m)
        assertEquals(70 * 60 * 1000L, analytics.focusTodayMs)
        assertEquals("1h 10m", analytics.focusTodayFormatted)

        // Week: 45m + 25m + 60m = 130m (2h 10m)
        assertEquals(130 * 60 * 1000L, analytics.focusWeekMs)
        assertEquals("2h 10m", analytics.focusWeekFormatted)

        // Total ms: 70m + 60m + 30m = 160m. Avg for 4 sessions: 160 / 4 = 40m
        assertEquals(40 * 60 * 1000L, analytics.averageSessionMs)
        assertEquals("40m", analytics.averageSessionFormatted)
        assertEquals(4, analytics.totalSessionsCount)
    }

    @Test
    fun testFocusDurationFormatting() {
        assertEquals("0m", FocusAnalyticsEvaluator.formatDuration(0L))
        assertEquals("< 1m", FocusAnalyticsEvaluator.formatDuration(20 * 1000L))
        assertEquals("25m", FocusAnalyticsEvaluator.formatDuration(25 * 60 * 1000L))
        assertEquals("1h", FocusAnalyticsEvaluator.formatDuration(60 * 60 * 1000L))
        assertEquals("2h 15m", FocusAnalyticsEvaluator.formatDuration(135 * 60 * 1000L))
        assertEquals("8h 40m", FocusAnalyticsEvaluator.formatDuration(520 * 60 * 1000L))
    }

    @Test
    fun testFocusXpEvaluation() {
        val date = "2026-08-14"
        val taskId = "test_task_1"

        // Under 25 minutes -> No XP bonus
        val shortSessionXp = XpEvaluator.evaluateFocusSessionXp(
            durationMs = 24 * 60 * 1000L,
            taskId = taskId,
            date = date,
            existingFocusEventsToday = emptyList()
        )
        assertNull(shortSessionXp)

        // Exactly 25 minutes -> +5 XP bonus
        val exact25MinXp = XpEvaluator.evaluateFocusSessionXp(
            durationMs = 25 * 60 * 1000L,
            taskId = taskId,
            date = date,
            existingFocusEventsToday = emptyList()
        )
        assertNotNull(exact25MinXp)
        assertEquals(5, exact25MinXp?.amount)
        assertEquals(XpEvaluator.EVENT_FOCUS_BONUS, exact25MinXp?.eventType)

        // 45 minutes -> +5 XP bonus
        val session45MinXp = XpEvaluator.evaluateFocusSessionXp(
            durationMs = 45 * 60 * 1000L,
            taskId = taskId,
            date = date,
            existingFocusEventsToday = emptyList()
        )
        assertNotNull(session45MinXp)
        assertEquals(5, session45MinXp?.amount)

        // Daily cap of 20 XP: if user already earned 15 XP today, 4th session gives 5 XP (total 20)
        val existing15Xp = listOf(
            XpEvent(amount = 5, eventType = XpEvaluator.EVENT_FOCUS_BONUS, date = date),
            XpEvent(amount = 5, eventType = XpEvaluator.EVENT_FOCUS_BONUS, date = date),
            XpEvent(amount = 5, eventType = XpEvaluator.EVENT_FOCUS_BONUS, date = date)
        )
        val fourthSessionXp = XpEvaluator.evaluateFocusSessionXp(
            durationMs = 30 * 60 * 1000L,
            taskId = taskId,
            date = date,
            existingFocusEventsToday = existing15Xp
        )
        assertNotNull(fourthSessionXp)
        assertEquals(5, fourthSessionXp?.amount)

        // If user already earned 20 XP today -> No more XP awarded
        val existing20Xp = existing15Xp + XpEvent(amount = 5, eventType = XpEvaluator.EVENT_FOCUS_BONUS, date = date)
        val fifthSessionXp = XpEvaluator.evaluateFocusSessionXp(
            durationMs = 60 * 60 * 1000L,
            taskId = taskId,
            date = date,
            existingFocusEventsToday = existing20Xp
        )
        assertNull(fifthSessionXp)
    }
}
