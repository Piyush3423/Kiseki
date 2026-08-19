package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long, // in milliseconds
    val completed: Boolean = false
)
