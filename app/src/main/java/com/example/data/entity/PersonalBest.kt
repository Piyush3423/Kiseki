package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_bests")
data class PersonalBest(
    @PrimaryKey val recordKey: String,
    val value: Int,
    val dateAchieved: String,
    val previousValue: Int = 0,
    val acknowledged: Boolean = false
)
