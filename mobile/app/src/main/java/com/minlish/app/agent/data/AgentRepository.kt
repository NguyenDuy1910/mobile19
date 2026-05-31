package com.minlish.app.agent.data

import com.minlish.app.core.model.ExplainWordRequest
import com.minlish.app.core.model.GenerateDeckRequest
import com.minlish.app.core.model.GenerateExamplesRequest
import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.safeApiCall

class AgentRepository(private val apiClient: ApiClient) {
    suspend fun explain(word: String, deckId: String? = null) = safeApiCall(apiClient.json) { apiClient.service.explainWord(ExplainWordRequest(word, deckId)) }
    suspend fun examples(word: String, count: Int = 3, deckId: String? = null) = safeApiCall(apiClient.json) {
        apiClient.service.generateExamples(GenerateExamplesRequest(word, count, deckId))
    }
    suspend fun deck(topic: String, limit: Int = 5, deckId: String? = null) = safeApiCall(apiClient.json) {
        apiClient.service.generateDeck(GenerateDeckRequest(topic, limit, deckId))
    }
}
