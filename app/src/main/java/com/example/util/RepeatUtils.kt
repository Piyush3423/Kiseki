package com.example.util

import com.example.data.model.RepeatType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object RepeatUtils {
    /**
     * Calculates the next due date for a recurring task based on repeatType and customDays.
     * Handles month-end dates (e.g., Jan 31 -> Feb 28/29) via ZonedDateTime.plusMonths(1).
     *
     * @param currentDueDate the existing due date timestamp, or null if none was set
     * @param repeatType the repeat pattern (None, Daily, Weekly, Monthly, Custom)
     * @param customDays number of days for Custom repeat type
     * @return timestamp for the next due date, or null if repeatType is None
     */
    fun calculateNextDueDate(
        currentDueDate: Long?,
        repeatType: RepeatType,
        customDays: Int? = null
    ): Long? {
        if (repeatType == RepeatType.None) return null

        val baseMillis = currentDueDate ?: System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val zonedDateTime = Instant.ofEpochMilli(baseMillis).atZone(zoneId)

        val nextDateTime: ZonedDateTime = when (repeatType) {
            RepeatType.None -> return null
            RepeatType.Daily -> zonedDateTime.plusDays(1)
            RepeatType.Weekly -> zonedDateTime.plusWeeks(1)
            RepeatType.Monthly -> zonedDateTime.plusMonths(1)
            RepeatType.Custom -> {
                val days = (customDays?.takeIf { it > 0 } ?: 1).toLong()
                zonedDateTime.plusDays(days)
            }
        }

        return nextDateTime.toInstant().toEpochMilli()
    }
}
