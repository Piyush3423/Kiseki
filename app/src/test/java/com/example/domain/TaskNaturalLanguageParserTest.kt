package com.example.domain

import com.example.data.model.Priority
import com.example.data.model.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskNaturalLanguageParserTest {

    // Friday, Aug 14, 2026 at 11:45 AM UTC
    private val fixedDate = LocalDate.of(2026, 8, 14) // This is a Friday
    private val fixedTime = LocalTime.of(11, 45)
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testGymTomorrow7pmHighPriority() {
        val input = "Gym tomorrow 7pm high priority"
        val result = TaskNaturalLanguageParser.parse(
            input = input,
            referenceDate = fixedDate,
            referenceTime = fixedTime,
            zoneId = zoneId
        )

        assertEquals("Gym", result.title)
        assertEquals(fixedDate.plusDays(1), result.date) // Saturday, Aug 15
        assertEquals(LocalTime.of(19, 0), result.time)
        assertEquals(Priority.High, result.priority)
        assertNull(result.repeatType)
        assertNull(result.estimatedDurationMinutes)
        assertTrue(result.hasExtractedAnyField)
    }

    @Test
    fun testStudyMLEveryWeekdayAt6pm() {
        val input = "Study ML every weekday at 6pm"
        val result = TaskNaturalLanguageParser.parse(
            input = input,
            referenceDate = fixedDate,
            referenceTime = fixedTime,
            zoneId = zoneId
        )

        assertEquals("Study ML", result.title)
        assertEquals(RepeatType.Daily, result.repeatType)
        assertEquals(LocalTime.of(18, 0), result.time)
        assertNull(result.priority)
        assertTrue(result.hasExtractedAnyField)
    }

    @Test
    fun testSubmitAssignmentFridayHigh() {
        // fixedDate is already Friday (Aug 14). Next occurrence should be calculated properly.
        val input = "Submit assignment Friday high"
        val result = TaskNaturalLanguageParser.parse(
            input = input,
            referenceDate = fixedDate,
            referenceTime = fixedTime,
            zoneId = zoneId
        )

        assertEquals("Submit assignment", result.title)
        assertNotNull(result.date)
        assertEquals(DayOfWeek.FRIDAY, result.date?.dayOfWeek)
        assertEquals(Priority.High, result.priority)
        assertTrue(result.hasExtractedAnyField)
    }

    @Test
    fun testReadFor30MinutesTonight() {
        val input = "Read for 30 minutes tonight"
        val result = TaskNaturalLanguageParser.parse(
            input = input,
            referenceDate = fixedDate,
            referenceTime = fixedTime,
            zoneId = zoneId
        )

        assertEquals("Read", result.title)
        assertEquals(30, result.estimatedDurationMinutes)
        assertEquals(fixedDate, result.date)
        assertEquals(LocalTime.of(20, 0), result.time) // tonight -> 8:00 PM
        assertTrue(result.hasExtractedAnyField)
    }

    @Test
    fun testTimeOfDayTerms() {
        // Morning
        val morningResult = TaskNaturalLanguageParser.parse("Dentist appointment tomorrow morning", fixedDate, fixedTime, zoneId)
        assertEquals("Dentist appointment", morningResult.title)
        assertEquals(fixedDate.plusDays(1), morningResult.date)
        assertEquals(LocalTime.of(9, 0), morningResult.time)

        // Afternoon
        val afternoonResult = TaskNaturalLanguageParser.parse("Team sync today afternoon low priority", fixedDate, fixedTime, zoneId)
        assertEquals("Team sync", afternoonResult.title)
        assertEquals(fixedDate, afternoonResult.date)
        assertEquals(LocalTime.of(14, 0), afternoonResult.time)
        assertEquals(Priority.Low, afternoonResult.priority)

        // Evening
        val eveningResult = TaskNaturalLanguageParser.parse("Cook dinner tomorrow evening", fixedDate, fixedTime, zoneId)
        assertEquals("Cook dinner", eveningResult.title)
        assertEquals(fixedDate.plusDays(1), eveningResult.date)
        assertEquals(LocalTime.of(19, 0), eveningResult.time)
    }

    @Test
    fun testRepeatPatterns() {
        // Daily
        val dailyResult = TaskNaturalLanguageParser.parse("Drink 2L water daily", fixedDate, fixedTime, zoneId)
        assertEquals("Drink 2L water", dailyResult.title)
        assertEquals(RepeatType.Daily, dailyResult.repeatType)

        // Every day
        val everyDayResult = TaskNaturalLanguageParser.parse("Meditation 15 mins every day", fixedDate, fixedTime, zoneId)
        assertEquals("Meditation", everyDayResult.title)
        assertEquals(15, everyDayResult.estimatedDurationMinutes)
        assertEquals(RepeatType.Daily, everyDayResult.repeatType)

        // Weekends
        val weekendsResult = TaskNaturalLanguageParser.parse("Hiking on weekends", fixedDate, fixedTime, zoneId)
        assertEquals("Hiking", weekendsResult.title)
        assertEquals(RepeatType.Weekly, weekendsResult.repeatType)

        // Weekly
        val weeklyResult = TaskNaturalLanguageParser.parse("Review analytics weekly", fixedDate, fixedTime, zoneId)
        assertEquals("Review analytics", weeklyResult.title)
        assertEquals(RepeatType.Weekly, weeklyResult.repeatType)

        // Monthly
        val monthlyResult = TaskNaturalLanguageParser.parse("Pay electricity bill monthly", fixedDate, fixedTime, zoneId)
        assertEquals("Pay electricity bill", monthlyResult.title)
        assertEquals(RepeatType.Monthly, monthlyResult.repeatType)

        // Custom days (every 3 days)
        val customResult = TaskNaturalLanguageParser.parse("Water plants every 3 days", fixedDate, fixedTime, zoneId)
        assertEquals("Water plants", customResult.title)
        assertEquals(RepeatType.Custom, customResult.repeatType)
        assertEquals(3, customResult.customRepeatDays)
    }

    @Test
    fun testDurationParsing() {
        // Fractional hours (1.5 hours -> 90 mins)
        val r1 = TaskNaturalLanguageParser.parse("Deep work session 1.5 hours tomorrow", fixedDate, fixedTime, zoneId)
        assertEquals("Deep work session", r1.title)
        assertEquals(90, r1.estimatedDurationMinutes)

        // 1 hr
        val r2 = TaskNaturalLanguageParser.parse("Run in park for 1 hour tonight", fixedDate, fixedTime, zoneId)
        assertEquals("Run in park", r2.title)
        assertEquals(60, r2.estimatedDurationMinutes)

        // 45 mins
        val r3 = TaskNaturalLanguageParser.parse("Piano practice 45 mins", fixedDate, fixedTime, zoneId)
        assertEquals("Piano practice", r3.title)
        assertEquals(45, r3.estimatedDurationMinutes)
    }

    @Test
    fun testPriorityLevels() {
        val high = TaskNaturalLanguageParser.parse("Submit tax return urgent", fixedDate, fixedTime, zoneId)
        assertEquals("Submit tax return", high.title)
        assertEquals(Priority.High, high.priority)

        val med = TaskNaturalLanguageParser.parse("Organize desk medium priority", fixedDate, fixedTime, zoneId)
        assertEquals("Organize desk", med.title)
        assertEquals(Priority.Medium, med.priority)

        val low = TaskNaturalLanguageParser.parse("Water lawn low", fixedDate, fixedTime, zoneId)
        assertEquals("Water lawn", low.title)
        assertEquals(Priority.Low, low.priority)
    }

    @Test
    fun testUnknownWordsRemainInTitle() {
        val input = "Buy organic avocados, oat milk and matcha powder tomorrow at 4pm"
        val result = TaskNaturalLanguageParser.parse(input, fixedDate, fixedTime, zoneId)

        assertEquals("Buy organic avocados, oat milk and matcha powder", result.title)
        assertEquals(fixedDate.plusDays(1), result.date)
        assertEquals(LocalTime.of(16, 0), result.time)
    }

    @Test
    fun testPlainTitleOnly() {
        val input = "Call landlord"
        val result = TaskNaturalLanguageParser.parse(input, fixedDate, fixedTime, zoneId)

        assertEquals("Call landlord", result.title)
        assertNull(result.date)
        assertNull(result.time)
        assertNull(result.priority)
        assertNull(result.repeatType)
        assertNull(result.estimatedDurationMinutes)
    }
}
