package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.XpEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface XpEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: XpEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<XpEvent>)

    @Query("SELECT * FROM xp_events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<XpEvent>>

    @Query("SELECT * FROM xp_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<XpEvent>

    @Query("SELECT * FROM xp_events WHERE taskId = :taskId")
    suspend fun getEventsForTask(taskId: String): List<XpEvent>

    @Query("SELECT * FROM xp_events WHERE taskId = :taskId AND eventType = :eventType")
    suspend fun getEventsForTaskAndType(taskId: String, eventType: String): List<XpEvent>

    @Query("SELECT * FROM xp_events WHERE date = :date AND eventType = :eventType")
    suspend fun getEventsForDateAndType(date: String, eventType: String): List<XpEvent>

    @Query("DELETE FROM xp_events WHERE taskId = :taskId")
    suspend fun deleteEventsForTask(taskId: String)
}
