package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_scores")
data class DailyScore(
    @PrimaryKey val date: String,
    val score: Int,
    val completionScore: Float,
    val priorityPerformance: Float,
    val onTimeScore: Float,
    val consistencyScore: Float
)
