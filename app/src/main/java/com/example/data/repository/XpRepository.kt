package com.example.data.repository

import com.example.data.dao.XpEventDao
import com.example.data.entity.XpEvent
import kotlinx.coroutines.flow.Flow

class XpRepository(private val xpEventDao: XpEventDao) {
    val allEvents: Flow<List<XpEvent>> = xpEventDao.getAllEventsFlow()

    suspend fun getAllEvents(): List<XpEvent> {
        return xpEventDao.getAllEvents()
    }

    suspend fun insertEvent(event: XpEvent) {
        xpEventDao.insertEvent(event)
    }

    suspend fun getEventsForTask(taskId: String): List<XpEvent> {
        return xpEventDao.getEventsForTask(taskId)
    }

    suspend fun getEventsForTaskAndType(taskId: String, eventType: String): List<XpEvent> {
        return xpEventDao.getEventsForTaskAndType(taskId, eventType)
    }

    suspend fun getEventsForDateAndType(date: String, eventType: String): List<XpEvent> {
        return xpEventDao.getEventsForDateAndType(date, eventType)
    }

    suspend fun deleteEventsForTask(taskId: String) {
        xpEventDao.deleteEventsForTask(taskId)
    }
}
