package com.minlish.app.learning.data

import com.minlish.app.core.model.ReviewRequest
import com.minlish.app.core.model.SessionEndRequest
import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.safeApiCall

class LearningRepository(private val apiClient: ApiClient) {
    suspend fun plan() = safeApiCall(apiClient.json) { apiClient.service.dailyPlan() }
    suspend fun dueWords() = safeApiCall(apiClient.json) { apiClient.service.dueWords() }
    suspend fun review(wordId: String, rating: String) = safeApiCall(apiClient.json) { apiClient.service.review(ReviewRequest(wordId, rating)) }
    suspend fun startSession() = safeApiCall(apiClient.json) { apiClient.service.startSession() }
    suspend fun endSession(request: SessionEndRequest) = safeApiCall(apiClient.json) { apiClient.service.endSession(request) }
}
