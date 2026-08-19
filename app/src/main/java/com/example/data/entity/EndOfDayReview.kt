package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "end_of_day_reviews")
data class EndOfDayReview(
    @PrimaryKey val date: String,
    val completedTasks: Int,
    val totalTasks: Int,
    val score: Int,
    val rank: String,
    val xpEarned: Int,
    val obstacles: List<String> = emptyList(),
    val note: String? = null,
    val reviewedAt: Long = System.currentTimeMillis()
)
