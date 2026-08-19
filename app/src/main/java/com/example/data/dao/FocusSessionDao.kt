package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<FocusSession>)

    @Update
    suspend fun updateSession(session: FocusSession)

    @Delete
    suspend fun deleteSession(session: FocusSession)

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<FocusSession>

    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsForTask(taskId: String): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<FocusSession>

    @Query("DELETE FROM focus_sessions WHERE taskId = :taskId")
    suspend fun deleteSessionsForTask(taskId: String)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}
