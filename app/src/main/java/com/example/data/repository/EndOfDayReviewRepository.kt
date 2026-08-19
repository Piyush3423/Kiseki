package com.example.data.repository

import com.example.data.dao.EndOfDayReviewDao
import com.example.data.entity.EndOfDayReview
import kotlinx.coroutines.flow.Flow

class EndOfDayReviewRepository(private val dao: EndOfDayReviewDao) {

    val allReviews: Flow<List<EndOfDayReview>> = dao.getAllReviewsFlow()

    fun getReviewForDateFlow(date: String): Flow<EndOfDayReview?> {
        return dao.getReviewForDateFlow(date)
    }

    suspend fun getReviewForDate(date: String): EndOfDayReview? {
        return dao.getReviewForDate(date)
    }

    suspend fun saveReview(review: EndOfDayReview) {
        dao.insertReview(review)
    }

    suspend fun deleteReview(review: EndOfDayReview) {
        dao.deleteReview(review)
    }

    suspend fun deleteReviewForDate(date: String) {
        dao.deleteReviewForDate(date)
    }
}
