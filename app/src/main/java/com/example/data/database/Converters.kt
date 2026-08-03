package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.Priority
import com.example.data.model.RepeatType

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(priority: String): Priority {
        return Priority.valueOf(priority)
    }

    @TypeConverter
    fun fromRepeatType(repeatType: RepeatType): String {
        return repeatType.name
    }

    @TypeConverter
    fun toRepeatType(repeatType: String): RepeatType {
        return RepeatType.valueOf(repeatType)
    }
}
