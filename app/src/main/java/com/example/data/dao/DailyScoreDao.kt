package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.entity.DailyScore
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyScoreDao {
    @Query("SELECT * FROM daily_scores WHERE date = :date")
    suspend fun getScoreForDate(date: String): DailyScore?

    @Query("SELECT * FROM daily_scores WHERE date = :date")
    fun getScoreForDateFlow(date: String): Flow<DailyScore?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: DailyScore)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<DailyScore>)

    @Query("SELECT * FROM daily_scores ORDER BY date DESC")
    fun getAllScoresFlow(): Flow<List<DailyScore>>
    
    @Query("SELECT * FROM daily_scores ORDER BY date DESC")
    suspend fun getAllScores(): List<DailyScore>

    @Query("DELETE FROM daily_scores")
    suspend fun deleteAllScores()
}
