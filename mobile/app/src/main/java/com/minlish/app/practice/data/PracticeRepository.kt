package com.minlish.app.practice.data

import com.minlish.app.core.model.PracticeGenerateRequest
import com.minlish.app.core.model.PracticeSubmitRequest
import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.safeApiCall

class PracticeRepository(private val apiClient: ApiClient) {
    suspend fun generate(limit: Int) = safeApiCall(apiClient.json) { apiClient.service.generatePractice(PracticeGenerateRequest(limit)) }
    suspend fun submit(wordId: String, answer: String) = safeApiCall(apiClient.json) { apiClient.service.submitPractice(PracticeSubmitRequest(wordId, answer)) }
    suspend fun history() = safeApiCall(apiClient.json) { apiClient.service.practiceHistory() }
}
