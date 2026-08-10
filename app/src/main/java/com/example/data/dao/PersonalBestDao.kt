package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.PersonalBest
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalBestDao {
    @Query("SELECT * FROM personal_bests")
    fun getAllRecords(): Flow<List<PersonalBest>>

    @Query("SELECT * FROM personal_bests")
    suspend fun getAllRecordsOneShot(): List<PersonalBest>

    @Query("SELECT * FROM personal_bests WHERE recordKey = :key LIMIT 1")
    suspend fun getRecordByKey(key: String): PersonalBest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: PersonalBest)

    @Query("UPDATE personal_bests SET acknowledged = 1 WHERE recordKey = :key")
    suspend fun markAcknowledged(key: String)
}
