package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import org.json.JSONArray
import org.json.JSONObject

data class TemplateTaskItem(
    val title: String,
    val description: String = "",
    val category: String = "",
    val priority: Priority = Priority.Medium,
    val repeatType: RepeatType = RepeatType.None,
    val customDays: Int? = null,
    val dueDayOffset: Int? = null,
    val isReminderEnabled: Boolean = false,
    val reminderTimeOfDayMinutes: Int? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("description", description)
            put("category", category)
            put("priority", priority.name)
            put("repeatType", repeatType.name)
            put("customDays", customDays ?: JSONObject.NULL)
            put("dueDayOffset", dueDayOffset ?: JSONObject.NULL)
            put("isReminderEnabled", isReminderEnabled)
            put("reminderTimeOfDayMinutes", reminderTimeOfDayMinutes ?: JSONObject.NULL)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): TemplateTaskItem {
            val title = obj.optString("title", "")
            val description = obj.optString("description", "")
            val category = obj.optString("category", "")
            val priority = try {
                Priority.valueOf(obj.optString("priority", "Medium"))
            } catch (e: Exception) {
                Priority.Medium
            }
            val repeatType = try {
                RepeatType.valueOf(obj.optString("repeatType", "None"))
            } catch (e: Exception) {
                RepeatType.None
            }
            val customDays = if (obj.isNull("customDays")) null else obj.optInt("customDays")
            val dueDayOffset = if (obj.isNull("dueDayOffset")) null else obj.optInt("dueDayOffset")
            val isReminderEnabled = obj.optBoolean("isReminderEnabled", false)
            val reminderTimeOfDayMinutes = if (obj.isNull("reminderTimeOfDayMinutes")) null else obj.optInt("reminderTimeOfDayMinutes")

            return TemplateTaskItem(
                title = title,
                description = description,
                category = category,
                priority = priority,
                repeatType = repeatType,
                customDays = customDays,
                dueDayOffset = dueDayOffset,
                isReminderEnabled = isReminderEnabled,
                reminderTimeOfDayMinutes = reminderTimeOfDayMinutes
            )
        }
    }
}

@Entity(tableName = "task_group_templates")
data class TaskGroupTemplate(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val itemsJson: String = "[]"
) {
    fun parseItems(): List<TemplateTaskItem> {
        val list = mutableListOf<TemplateTaskItem>()
        try {
            val array = JSONArray(itemsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(TemplateTaskItem.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        fun createItemsJson(items: List<TemplateTaskItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}
