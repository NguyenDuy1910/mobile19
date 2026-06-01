package com.minlish.app.progress.data

import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.safeApiCall

class ProgressRepository(private val apiClient: ApiClient) {
    suspend fun summary() = safeApiCall(apiClient.json) { apiClient.service.progressSummary() }
    suspend fun activity() = safeApiCall(apiClient.json) { apiClient.service.dailyActivity() }
    suspend fun retention() = safeApiCall(apiClient.json) { apiClient.service.retention() }
    suspend fun deck(deckId: String) = safeApiCall(apiClient.json) { apiClient.service.deckProgress(deckId) }
}
