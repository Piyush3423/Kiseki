package com.example.data.repository

import com.example.data.entity.ActivityTask
import com.example.data.entity.Category
import com.example.data.entity.DailyScore
import com.example.data.entity.EndOfDayReview
import com.example.data.entity.FocusSession
import com.example.data.entity.PersonalBest
import com.example.data.entity.TaskGroup
import com.example.data.entity.TaskGroupTemplate
import com.example.data.entity.XpEvent
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupDataTest {

    @Test
    fun `backup data structure defaults are empty and safe`() {
        val backupData = BackupData()
        assertTrue(backupData.categories.isEmpty())
        assertTrue(backupData.tasks.isEmpty())
        assertTrue(backupData.groups.isEmpty())
        assertTrue(backupData.templates.isEmpty())
        assertTrue(backupData.dailyScores.isEmpty())
        assertTrue(backupData.xpEvents.isEmpty())
        assertTrue(backupData.personalBests.isEmpty())
        assertTrue(backupData.endOfDayReviews.isEmpty())
        assertTrue(backupData.focusSessions.isEmpty())
    }

    @Test
    fun `parses legacy v1 backup json without modern fields gracefully`() {
        val legacyJson = JSONObject().apply {
            put("app", "Kiseki")
            put("version", 1)
            put("exportedAt", 1700000000000L)
            put("categories", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "cat-1")
                    put("name", "Work")
                    put("colorHex", "#6750A4")
                    put("isDefault", true)
                })
            })
            put("tasks", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "task-1")
                    put("title", "Legacy Task")
                    put("description", "Old description")
                    put("category", "Work")
                    put("priority", "High")
                    put("isCompleted", false)
                    put("createdAt", 1700000000000L)
                })
            })
        }

        val jsonString = legacyJson.toString()
        val rootObj = JSONObject(jsonString)

        assertTrue(rootObj.has("tasks"))
        assertTrue(rootObj.has("categories"))
        assertFalse(rootObj.has("dailyScores"))
        assertFalse(rootObj.has("xpEvents"))
        assertFalse(rootObj.has("personalBests"))
        assertFalse(rootObj.has("endOfDayReviews"))
        assertFalse(rootObj.has("focusSessions"))

        // Parsing legacy tasks defaults friction and duration safely
        val taskObj = rootObj.getJSONArray("tasks").getJSONObject(0)
        assertEquals("task-1", taskObj.optString("id"))
        assertEquals("Legacy Task", taskObj.optString("title"))
        assertEquals(0, taskObj.optInt("rescheduleCount", 0))
        assertEquals(0, taskObj.optInt("missCount", 0))
        assertEquals(0.0, taskObj.optDouble("frictionScore", 0.0), 0.001)
        assertTrue(taskObj.isNull("estimatedDurationMinutes"))
    }

    @Test
    fun `full v2 backup json preserves all modern entity fields`() {
        val fullJson = JSONObject().apply {
            put("app", "Kiseki")
            put("version", 2)
            put("exportedAt", 1700000000000L)
            put("tasks", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "task-100")
                    put("title", "Modern Task")
                    put("description", "With friction data")
                    put("category", "Study")
                    put("priority", "High")
                    put("isCompleted", true)
                    put("completedAt", 1700000500000L)
                    put("createdAt", 1700000000000L)
                    put("dueDate", 1700000400000L)
                    put("repeatType", "Weekly")
                    put("isReminderEnabled", true)
                    put("reminderTime", 1700000300000L)
                    put("groupId", "group-1")
                    put("rescheduleCount", 2)
                    put("missCount", 1)
                    put("lateCompletionCount", 1)
                    put("frictionScore", 2.5)
                    put("frictionSuppressedUntil", 1700100000000L)
                    put("estimatedDurationMinutes", 45)
                })
            })
            put("dailyScores", JSONArray().apply {
                put(JSONObject().apply {
                    put("date", "2026-08-18")
                    put("score", 92)
                    put("completionScore", 90.0)
                    put("priorityPerformance", 95.0)
                    put("onTimeScore", 90.0)
                    put("consistencyScore", 93.0)
                })
            })
            put("xpEvents", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "xp-1")
                    put("amount", 20)
                    put("eventType", "TASK_COMPLETED_HIGH")
                    put("timestamp", 1700000500000L)
                    put("taskId", "task-100")
                    put("date", "2026-08-18")
                })
            })
            put("personalBests", JSONArray().apply {
                put(JSONObject().apply {
                    put("recordKey", "streak_daily_target")
                    put("value", 14)
                    put("dateAchieved", "2026-08-18")
                    put("previousValue", 10)
                    put("acknowledged", true)
                })
            })
            put("endOfDayReviews", JSONArray().apply {
                put(JSONObject().apply {
                    put("date", "2026-08-18")
                    put("completedTasks", 5)
                    put("totalTasks", 6)
                    put("score", 88)
                    put("rank", "S")
                    put("xpEarned", 50)
                    put("obstacles", "[\"Overestimated time\"]")
                    put("note", "Productive day")
                    put("reviewedAt", 1700000900000L)
                })
            })
            put("focusSessions", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "sess-1")
                    put("taskId", "task-100")
                    put("startTime", 1700000100000L)
                    put("endTime", 1700000400000L)
                    put("duration", 1800000L)
                    put("completed", true)
                })
            })
        }

        val jsonString = fullJson.toString()
        val rootObj = JSONObject(jsonString)

        assertEquals("Kiseki", rootObj.getString("app"))
        assertEquals(2, rootObj.getInt("version"))
        assertEquals(1, rootObj.getJSONArray("tasks").length())
        assertEquals(1, rootObj.getJSONArray("dailyScores").length())
        assertEquals(1, rootObj.getJSONArray("xpEvents").length())
        assertEquals(1, rootObj.getJSONArray("personalBests").length())
        assertEquals(1, rootObj.getJSONArray("endOfDayReviews").length())
        assertEquals(1, rootObj.getJSONArray("focusSessions").length())

        val taskObj = rootObj.getJSONArray("tasks").getJSONObject(0)
        assertEquals(2, taskObj.getInt("rescheduleCount"))
        assertEquals(1, taskObj.getInt("missCount"))
        assertEquals(1, taskObj.getInt("lateCompletionCount"))
        assertEquals(2.5, taskObj.getDouble("frictionScore"), 0.001)
        assertEquals(1700100000000L, taskObj.getLong("frictionSuppressedUntil"))
        assertEquals(45, taskObj.getInt("estimatedDurationMinutes"))
    }
}
