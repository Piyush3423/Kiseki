package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "xp_events")
data class XpEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Int,
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val taskId: String? = null,
    val date: String
)
