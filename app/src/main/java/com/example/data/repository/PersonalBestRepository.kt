package com.example.data.repository

import com.example.data.dao.PersonalBestDao
import com.example.data.entity.PersonalBest
import kotlinx.coroutines.flow.Flow

class PersonalBestRepository(private val dao: PersonalBestDao) {
    val allRecords: Flow<List<PersonalBest>> = dao.getAllRecords()

    suspend fun getAllRecordsOneShot(): List<PersonalBest> = dao.getAllRecordsOneShot()

    suspend fun getRecordByKey(key: String): PersonalBest? = dao.getRecordByKey(key)

    suspend fun saveRecord(record: PersonalBest) = dao.insertOrUpdate(record)

    suspend fun markAcknowledged(key: String) = dao.markAcknowledged(key)
}
