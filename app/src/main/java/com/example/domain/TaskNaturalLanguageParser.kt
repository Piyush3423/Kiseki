package com.example.domain

import com.example.data.model.Priority
import com.example.data.model.RepeatType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.regex.Pattern

/**
 * Result of parsing natural language task input.
 */
data class ParsedTaskResult(
    val originalText: String,
    val title: String,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val combinedDueDateTime: LocalDateTime? = null,
    val dueDateMillis: Long? = null,
    val priority: Priority? = null,
    val repeatType: RepeatType? = null,
    val customRepeatDays: Int? = null,
    val repeatDescription: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val hasExtractedAnyField: Boolean = false
)

/**
 * Local deterministic Natural Language Parser for Kiseki task input.
 * Extracts title, relative & explicit dates, times of day, priorities,
 * repeat patterns, and estimated durations without any external AI API.
 */
object TaskNaturalLanguageParser {

    /**
     * Parses natural language input text into structured task fields.
     *
     * @param input Raw text string from user.
     * @param referenceDate Base date to calculate relative dates from (defaults to today).
     * @param referenceTime Base time to calculate relative times from (defaults to now).
     * @param zoneId Timezone to convert local datetime to epoch millis.
     */
    fun parse(
        input: String,
        referenceDate: LocalDate = LocalDate.now(),
        referenceTime: LocalTime = LocalTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ParsedTaskResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return ParsedTaskResult(originalText = input, title = "")
        }

        // We will keep track of token spans to remove from title
        var workingText = " $trimmed "
        val matchedSpans = mutableListOf<String>()

        // 1. Parse Duration (e.g. "for 30 minutes", "1.5 hours", "45 mins", "1h 30m")
        val (parsedDuration, durationMatch) = extractDuration(workingText)
        if (parsedDuration != null && durationMatch != null) {
            matchedSpans.add(durationMatch)
            workingText = replaceFirstWordBoundary(workingText, durationMatch)
        }

        // 2. Parse Repeat Pattern (e.g. "every weekday", "daily", "every day", "weekly", "monthly", "every 3 days")
        val (parsedRepeatType, customDays, repeatDesc, repeatMatch) = extractRepeatPattern(workingText)
        if (parsedRepeatType != null && repeatMatch != null) {
            matchedSpans.add(repeatMatch)
            workingText = replaceFirstWordBoundary(workingText, repeatMatch)
        }

        // 3. Parse Priority (e.g. "high priority", "urgent", "low", "medium", "p1")
        val (parsedPriority, priorityMatch) = extractPriority(workingText)
        if (parsedPriority != null && priorityMatch != null) {
            matchedSpans.add(priorityMatch)
            workingText = replaceFirstWordBoundary(workingText, priorityMatch)
        }

        // 4. Parse Date (e.g. "tomorrow", "today", "tonight", "Friday", "next Tuesday", "on Monday")
        val (parsedDate, dateImpliedTime, dateMatch) = extractDate(workingText, referenceDate, referenceTime)
        if (parsedDate != null && dateMatch != null) {
            matchedSpans.add(dateMatch)
            workingText = replaceFirstWordBoundary(workingText, dateMatch)
        }

        // 5. Parse Explicit Time or Time of Day (e.g. "7pm", "at 6:30am", "19:00", "afternoon", "morning", "evening")
        val (parsedExplicitTime, timeMatch) = extractTime(workingText)
        if (parsedExplicitTime != null && timeMatch != null) {
            matchedSpans.add(timeMatch)
            workingText = replaceFirstWordBoundary(workingText, timeMatch)
        }

        // Finalize time: explicit time takes precedence over implied time from date (like "tonight" -> 8pm)
        val finalTime: LocalTime? = parsedExplicitTime ?: dateImpliedTime

        // If a time was found but no date, and the time is already in the past today, it could imply tomorrow or today
        val finalDate: LocalDate? = parsedDate ?: if (finalTime != null) referenceDate else null

        // Combine date and time
        val combinedDateTime: LocalDateTime? = when {
            finalDate != null && finalTime != null -> LocalDateTime.of(finalDate, finalTime)
            finalDate != null -> LocalDateTime.of(finalDate, LocalTime.of(9, 0)) // default 9 AM due time if date only
            else -> null
        }

        val dueDateMillis: Long? = combinedDateTime?.atZone(zoneId)?.toInstant()?.toEpochMilli()

        // Clean up remaining words to form the title
        val cleanedTitle = cleanupTitle(workingText)

        // Fallback: if title ended up empty (e.g. input was just "tomorrow 7pm"), use original input
        val finalTitle = if (cleanedTitle.isNotBlank()) cleanedTitle else trimmed

        val hasExtractedAnyField = parsedDate != null ||
                finalTime != null ||
                parsedPriority != null ||
                parsedRepeatType != null ||
                parsedDuration != null

        return ParsedTaskResult(
            originalText = trimmed,
            title = finalTitle,
            date = finalDate,
            time = finalTime,
            combinedDueDateTime = combinedDateTime,
            dueDateMillis = dueDateMillis,
            priority = parsedPriority,
            repeatType = parsedRepeatType,
            customRepeatDays = customDays,
            repeatDescription = repeatDesc,
            estimatedDurationMinutes = parsedDuration,
            hasExtractedAnyField = hasExtractedAnyField
        )
    }

    /**
     * Extracts duration in minutes.
     * Matches patterns:
     * - "for 30 minutes", "30 mins", "30 min", "30m"
     * - "for 1 hour", "1 hr", "2 hours", "1.5 hours", "1h 30m", "90 mins"
     */
    private fun extractDuration(text: String): Pair<Int?, String?> {
        // Pattern for "1h 30m" or "1 hr 30 mins"
        val hrMinPattern = Pattern.compile(
            """(?i)\b(?:for\s+)?(\d+)\s*(?:h|hr|hrs|hours?)\s*(?:and\s+)?(\d+)\s*(?:m|min|mins|minutes?)\b"""
        )
        val hrMinMatcher = hrMinPattern.matcher(text)
        if (hrMinMatcher.find()) {
            val hours = hrMinMatcher.group(1)?.toIntOrNull() ?: 0
            val mins = hrMinMatcher.group(2)?.toIntOrNull() ?: 0
            return Pair(hours * 60 + mins, hrMinMatcher.group(0))
        }

        // Pattern for fractional hours like "1.5 hours", "2.5 hrs", "0.5 hr"
        val fractionalHrPattern = Pattern.compile(
            """(?i)\b(?:for\s+)?(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hours?)\b"""
        )
        val fracMatcher = fractionalHrPattern.matcher(text)
        if (fracMatcher.find()) {
            val hours = fracMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            val mins = (hours * 60).toInt()
            if (mins > 0) {
                return Pair(mins, fracMatcher.group(0))
            }
        }

        // Pattern for minutes like "for 30 minutes", "45 mins", "15m"
        val minPattern = Pattern.compile(
            """(?i)\b(?:for\s+)?(\d+)\s*(?:m|min|mins|minutes?)\b"""
        )
        val minMatcher = minPattern.matcher(text)
        if (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toIntOrNull() ?: 0
            if (mins > 0) {
                return Pair(mins, minMatcher.group(0))
            }
        }

        return Pair(null, null)
    }

    /**
     * Extracts repeat pattern.
     * Matches:
     * - "every weekday", "on weekdays", "weekdays", "Monday-Friday", "mon-fri" -> Daily (or custom)
     * - "every weekend", "on weekends", "weekends" -> Weekly
     * - "daily", "every day", "everyday" -> Daily
     * - "weekly", "every week" -> Weekly
     * - "monthly", "every month" -> Monthly
     * - "every (\d+) days" -> Custom
     * - "every (Monday|Tuesday|...)" -> Weekly
     */
    private data class RepeatExtraction(
        val type: RepeatType?,
        val customDays: Int?,
        val description: String?,
        val matchedText: String?
    )

    private fun extractRepeatPattern(text: String): RepeatExtraction {
        // Weekdays: "every weekday", "every weekdays", "on weekdays", "weekdays", "monday-friday", "mon-fri"
        val weekdayPattern = Pattern.compile(
            """(?i)\b(?:every\s+weekdays?|on\s+weekdays?|weekdays|monday\s*-\s*friday|mon\s*-\s*fri)\b"""
        )
        val weekdayMatcher = weekdayPattern.matcher(text)
        if (weekdayMatcher.find()) {
            return RepeatExtraction(RepeatType.Daily, null, "Weekdays (Mon-Fri)", weekdayMatcher.group(0))
        }

        // Weekends: "every weekend", "on weekends", "weekends", "saturday-sunday", "sat-sun"
        val weekendPattern = Pattern.compile(
            """(?i)\b(?:every\s+weekends?|on\s+weekends?|weekends|saturday\s*-\s*sunday|sat\s*-\s*sun)\b"""
        )
        val weekendMatcher = weekendPattern.matcher(text)
        if (weekendMatcher.find()) {
            return RepeatExtraction(RepeatType.Weekly, null, "Weekends (Sat-Sun)", weekendMatcher.group(0))
        }

        // Custom days: "every 3 days", "every 2 days"
        val customDaysPattern = Pattern.compile(
            """(?i)\bevery\s+(\d+)\s+days?\b"""
        )
        val customDaysMatcher = customDaysPattern.matcher(text)
        if (customDaysMatcher.find()) {
            val days = customDaysMatcher.group(1)?.toIntOrNull() ?: 1
            return RepeatExtraction(RepeatType.Custom, days, "Every $days days", customDaysMatcher.group(0))
        }

        // Daily: "daily", "every day", "everyday"
        val dailyPattern = Pattern.compile(
            """(?i)\b(?:daily|every\s+day|everyday)\b"""
        )
        val dailyMatcher = dailyPattern.matcher(text)
        if (dailyMatcher.find()) {
            return RepeatExtraction(RepeatType.Daily, null, "Daily", dailyMatcher.group(0))
        }

        // Weekly on specific day: "every monday", "every tue", etc.
        val weeklyDayPattern = Pattern.compile(
            """(?i)\bevery\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\b"""
        )
        val weeklyDayMatcher = weeklyDayPattern.matcher(text)
        if (weeklyDayMatcher.find()) {
            val dayName = weeklyDayMatcher.group(1).lowercase().replaceFirstChar { it.uppercase() }
            return RepeatExtraction(RepeatType.Weekly, null, "Weekly on $dayName", weeklyDayMatcher.group(0))
        }

        // Weekly: "weekly", "every week"
        val weeklyPattern = Pattern.compile(
            """(?i)\b(?:weekly|every\s+week)\b"""
        )
        val weeklyMatcher = weeklyPattern.matcher(text)
        if (weeklyMatcher.find()) {
            return RepeatExtraction(RepeatType.Weekly, null, "Weekly", weeklyMatcher.group(0))
        }

        // Monthly: "monthly", "every month"
        val monthlyPattern = Pattern.compile(
            """(?i)\b(?:monthly|every\s+month)\b"""
        )
        val monthlyMatcher = monthlyPattern.matcher(text)
        if (monthlyMatcher.find()) {
            return RepeatExtraction(RepeatType.Monthly, null, "Monthly", monthlyMatcher.group(0))
        }

        return RepeatExtraction(null, null, null, null)
    }

    /**
     * Extracts priority level.
     * Matches:
     * - "high priority", "highest priority", "urgent", "high", "critical", "p1" -> High
     * - "medium priority", "normal priority", "medium", "p2" -> Medium
     * - "low priority", "lowest priority", "low", "p3" -> Low
     */
    private fun extractPriority(text: String): Pair<Priority?, String?> {
        // High Priority
        val highPattern = Pattern.compile(
            """(?i)\b(?:high\s+priority|highest\s+priority|urgent|critical|high|p1)\b"""
        )
        val highMatcher = highPattern.matcher(text)
        if (highMatcher.find()) {
            return Pair(Priority.High, highMatcher.group(0))
        }

        // Low Priority
        val lowPattern = Pattern.compile(
            """(?i)\b(?:low\s+priority|lowest\s+priority|low|p3)\b"""
        )
        val lowMatcher = lowPattern.matcher(text)
        if (lowMatcher.find()) {
            return Pair(Priority.Low, lowMatcher.group(0))
        }

        // Medium Priority
        val medPattern = Pattern.compile(
            """(?i)\b(?:medium\s+priority|normal\s+priority|medium|med|p2)\b"""
        )
        val medMatcher = medPattern.matcher(text)
        if (medMatcher.find()) {
            return Pair(Priority.Medium, medMatcher.group(0))
        }

        return Pair(null, null)
    }

    /**
     * Extracts date and possible implied time (like "tonight" -> 8:00 PM).
     * Matches:
     * - "today", "tonight", "this morning", "this evening", "this afternoon"
     * - "tomorrow", "tomorrow morning", "tomorrow night"
     * - "next Monday", "this Friday", "on Friday", "Friday"
     */
    private data class DateExtraction(
        val date: LocalDate?,
        val impliedTime: LocalTime?,
        val matchedText: String?
    )

    private fun extractDate(
        text: String,
        referenceDate: LocalDate,
        referenceTime: LocalTime
    ): DateExtraction {
        // 1. "tomorrow morning/afternoon/evening/night" or just "tomorrow"
        val tomorrowDetailedPattern = Pattern.compile(
            """(?i)\btomorrow(?:\s+(morning|afternoon|evening|night))?\b"""
        )
        val tomorrowMatcher = tomorrowDetailedPattern.matcher(text)
        if (tomorrowMatcher.find()) {
            val period = tomorrowMatcher.group(1)?.lowercase()
            val impliedTime = when (period) {
                "morning" -> LocalTime.of(9, 0)
                "afternoon" -> LocalTime.of(14, 0)
                "evening" -> LocalTime.of(19, 0)
                "night" -> LocalTime.of(20, 0)
                else -> null
            }
            return DateExtraction(referenceDate.plusDays(1), impliedTime, tomorrowMatcher.group(0))
        }

        // 2. "tonight"
        val tonightPattern = Pattern.compile("""(?i)\btonight\b""")
        val tonightMatcher = tonightPattern.matcher(text)
        if (tonightMatcher.find()) {
            return DateExtraction(referenceDate, LocalTime.of(20, 0), tonightMatcher.group(0))
        }

        // 3. "today"
        val todayDetailedPattern = Pattern.compile(
            """(?i)\btoday(?:\s+(morning|afternoon|evening|night))?\b"""
        )
        val todayMatcher = todayDetailedPattern.matcher(text)
        if (todayMatcher.find()) {
            val period = todayMatcher.group(1)?.lowercase()
            val impliedTime = when (period) {
                "morning" -> LocalTime.of(9, 0)
                "afternoon" -> LocalTime.of(14, 0)
                "evening" -> LocalTime.of(19, 0)
                "night" -> LocalTime.of(20, 0)
                else -> null
            }
            return DateExtraction(referenceDate, impliedTime, todayMatcher.group(0))
        }

        // 4. Days of week: e.g. "next Friday", "this Friday", "on Friday", "Friday"
        val dayOfWeekPattern = Pattern.compile(
            """(?i)\b(?:(next|this|on|coming)\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)(?:\s+(morning|afternoon|evening|night))?\b"""
        )
        val dowMatcher = dayOfWeekPattern.matcher(text)
        if (dowMatcher.find()) {
            val prefix = dowMatcher.group(1)?.lowercase()
            val dayName = dowMatcher.group(2).lowercase()
            val period = dowMatcher.group(3)?.lowercase()

            val targetDow = parseDayOfWeek(dayName)
            if (targetDow != null) {
                var targetDate = referenceDate.with(TemporalAdjusters.nextOrSame(targetDow))
                // If prefix is "next", or if target is today but user specified a relative day like "next Friday"
                if (prefix == "next" || (targetDate == referenceDate && prefix != "this" && prefix != "on")) {
                    targetDate = referenceDate.with(TemporalAdjusters.next(targetDow))
                }

                val impliedTime = when (period) {
                    "morning" -> LocalTime.of(9, 0)
                    "afternoon" -> LocalTime.of(14, 0)
                    "evening" -> LocalTime.of(19, 0)
                    "night" -> LocalTime.of(20, 0)
                    else -> null
                }

                return DateExtraction(targetDate, impliedTime, dowMatcher.group(0))
            }
        }

        return DateExtraction(null, null, null)
    }

    /**
     * Extracts explicit time or general time-of-day.
     * Matches:
     * - "7pm", "7:30pm", "7:30 pm", "7 pm", "7am", "10:30am", "11:45 am"
     * - "at 7pm", "at 7:30", "@ 6pm"
     * - "19:00", "07:30", "14:45"
     * - "morning", "afternoon", "evening", "night", "noon", "midday"
     */
    private fun extractTime(text: String): Pair<LocalTime?, String?> {
        // 1. Explicit 12-hour format: "7pm", "7:30pm", "7:30 pm", "at 7pm", "@ 6:30 am", "11am", "12pm", "12am"
        val time12HrPattern = Pattern.compile(
            """(?i)\b(?:at\s+|@\s*)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b"""
        )
        val time12Matcher = time12HrPattern.matcher(text)
        if (time12Matcher.find()) {
            val rawHour = time12Matcher.group(1).toIntOrNull() ?: 0
            val rawMin = time12Matcher.group(2)?.toIntOrNull() ?: 0
            val amPm = time12Matcher.group(3).lowercase()

            if (rawHour in 1..12 && rawMin in 0..59) {
                var hour24 = rawHour
                if (amPm == "pm" && rawHour < 12) hour24 += 12
                if (amPm == "am" && rawHour == 12) hour24 = 0
                return Pair(LocalTime.of(hour24, rawMin), time12Matcher.group(0))
            }
        }

        // 2. Explicit 24-hour format: "at 19:00", "14:30", "08:15"
        val time24HrPattern = Pattern.compile(
            """(?i)\b(?:at\s+|@\s*)?(\d{1,2}):(\d{2})\b"""
        )
        val time24Matcher = time24HrPattern.matcher(text)
        if (time24Matcher.find()) {
            val hour = time24Matcher.group(1).toIntOrNull() ?: -1
            val min = time24Matcher.group(2).toIntOrNull() ?: -1
            if (hour in 0..23 && min in 0..59) {
                return Pair(LocalTime.of(hour, min), time24Matcher.group(0))
            }
        }

        // 3. "at 7 o'clock", "at 7 oclock"
        val oclockPattern = Pattern.compile(
            """(?i)\b(?:at\s+|@\s*)?(\d{1,2})\s*o'?clock\b"""
        )
        val oclockMatcher = oclockPattern.matcher(text)
        if (oclockMatcher.find()) {
            val hour = oclockMatcher.group(1).toIntOrNull() ?: -1
            if (hour in 1..12) {
                val hour24 = if (hour in 1..7) hour + 12 else hour // heuristic: 1-7 implies PM for o'clock unless specified
                return Pair(LocalTime.of(hour24, 0), oclockMatcher.group(0))
            }
        }

        // 4. Time of day terms: "morning", "afternoon", "evening", "night", "noon", "midday"
        val timeOfDayPattern = Pattern.compile(
            """(?i)\b(?:in\s+the\s+|at\s+)?(morning|afternoon|evening|night|noon|midday)\b"""
        )
        val todMatcher = timeOfDayPattern.matcher(text)
        if (todMatcher.find()) {
            val term = todMatcher.group(1).lowercase()
            val time = when (term) {
                "morning" -> LocalTime.of(9, 0)
                "noon", "midday" -> LocalTime.of(12, 0)
                "afternoon" -> LocalTime.of(14, 0)
                "evening" -> LocalTime.of(19, 0)
                "night" -> LocalTime.of(20, 0)
                else -> null
            }
            if (time != null) {
                return Pair(time, todMatcher.group(0))
            }
        }

        return Pair(null, null)
    }

    private fun parseDayOfWeek(str: String): DayOfWeek? {
        return when (str.lowercase()) {
            "monday", "mon" -> DayOfWeek.MONDAY
            "tuesday", "tue", "tues" -> DayOfWeek.TUESDAY
            "wednesday", "wed" -> DayOfWeek.WEDNESDAY
            "thursday", "thu", "thur", "thurs" -> DayOfWeek.THURSDAY
            "friday", "fri" -> DayOfWeek.FRIDAY
            "saturday", "sat" -> DayOfWeek.SATURDAY
            "sunday", "sun" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    /**
     * Replaces the first exact word boundary occurrence of target with empty space.
     */
    private fun replaceFirstWordBoundary(text: String, target: String): String {
        val pattern = Pattern.compile("""(?i)\b${Pattern.quote(target.trim())}\b""")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            text.substring(0, matcher.start()) + " " + text.substring(matcher.end())
        } else {
            text.replace(target, " ", ignoreCase = true)
        }
    }

    /**
     * Cleans up remaining words to form the human-readable task title.
     * Cleans extraneous preposition fragments ("at", "on", "for", "in the", "by") left at ends.
     */
    private fun cleanupTitle(text: String): String {
        var result = text.trim()
            .replace(Regex("""\s+"""), " ") // collapse multiple spaces

        // Remove dangling prepositions or filler conjunctions from the end or start
        result = result
            .replace(Regex("""(?i)^(?:at|on|for|by|in|to)\s+"""), "")
            .replace(Regex("""(?i)\s+(?:at|on|for|by|in|due)\b$"""), "")
            .replace(Regex("""(?i)\s+(?:in\s+the)\b$"""), "")
            .trim()

        // Capitalize first letter of title if not empty
        return if (result.isNotEmpty()) {
            result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            result
        }
    }
}
