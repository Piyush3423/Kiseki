package com.example.data.repository

import com.example.data.dao.DailyScoreDao
import com.example.data.entity.DailyScore
import kotlinx.coroutines.flow.Flow

class DailyScoreRepository(private val dailyScoreDao: DailyScoreDao) {
    val allScores: Flow<List<DailyScore>> = dailyScoreDao.getAllScoresFlow()

    fun getScoreForDate(date: String): Flow<DailyScore?> {
        return dailyScoreDao.getScoreForDateFlow(date)
    }

    suspend fun getScoreForDateOneShot(date: String): DailyScore? {
        return dailyScoreDao.getScoreForDate(date)
    }

    suspend fun insertScore(score: DailyScore) {
        dailyScoreDao.insertScore(score)
    }
    
    suspend fun insertScores(scores: List<DailyScore>) {
        dailyScoreDao.insertScores(scores)
    }
    
    suspend fun getAllScores(): List<DailyScore> {
        return dailyScoreDao.getAllScores()
    }
}
