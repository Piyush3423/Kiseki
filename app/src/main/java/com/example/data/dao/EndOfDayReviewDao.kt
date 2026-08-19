package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.EndOfDayReview
import kotlinx.coroutines.flow.Flow

@Dao
interface EndOfDayReviewDao {
    @Query("SELECT * FROM end_of_day_reviews WHERE date = :date")
    suspend fun getReviewForDate(date: String): EndOfDayReview?

    @Query("SELECT * FROM end_of_day_reviews WHERE date = :date")
    fun getReviewForDateFlow(date: String): Flow<EndOfDayReview?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: EndOfDayReview)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<EndOfDayReview>)

    @Query("SELECT * FROM end_of_day_reviews ORDER BY date DESC")
    fun getAllReviewsFlow(): Flow<List<EndOfDayReview>>

    @Query("SELECT * FROM end_of_day_reviews ORDER BY date DESC")
    suspend fun getAllReviews(): List<EndOfDayReview>

    @Delete
    suspend fun deleteReview(review: EndOfDayReview)

    @Query("DELETE FROM end_of_day_reviews WHERE date = :date")
    suspend fun deleteReviewForDate(date: String)

    @Query("DELETE FROM end_of_day_reviews")
    suspend fun deleteAllReviews()
}
