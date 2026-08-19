package com.example.data.repository

import com.example.data.dao.FocusSessionDao
import com.example.data.entity.FocusSession
import kotlinx.coroutines.flow.Flow

class FocusSessionRepository(private val focusSessionDao: FocusSessionDao) {
    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessionsFlow()

    suspend fun insertSession(session: FocusSession) {
        focusSessionDao.insertSession(session)
    }

    suspend fun updateSession(session: FocusSession) {
        focusSessionDao.updateSession(session)
    }

    suspend fun deleteSession(session: FocusSession) {
        focusSessionDao.deleteSession(session)
    }

    suspend fun getAllSessions(): List<FocusSession> {
        return focusSessionDao.getAllSessions()
    }

    fun getSessionsForTask(taskId: String): Flow<List<FocusSession>> {
        return focusSessionDao.getSessionsForTask(taskId)
    }

    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<FocusSession> {
        return focusSessionDao.getSessionsBetween(startTime, endTime)
    }

    suspend fun deleteSessionsForTask(taskId: String) {
        focusSessionDao.deleteSessionsForTask(taskId)
    }
}
