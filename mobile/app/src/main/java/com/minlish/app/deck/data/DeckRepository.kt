package com.minlish.app.deck.data

import com.minlish.app.core.model.DeckRequest
import com.minlish.app.core.model.DeckUpdateRequest
import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.safeApiCall

class DeckRepository(private val apiClient: ApiClient) {
    suspend fun list() = safeApiCall(apiClient.json) { apiClient.service.decks() }
    suspend fun get(id: String) = safeApiCall(apiClient.json) { apiClient.service.deck(id) }
    suspend fun create(request: DeckRequest) = safeApiCall(apiClient.json) { apiClient.service.createDeck(request) }
    suspend fun update(id: String, request: DeckUpdateRequest) = safeApiCall(apiClient.json) { apiClient.service.updateDeck(id, request) }
    suspend fun delete(id: String) = safeApiCall(apiClient.json) { apiClient.service.deleteDeck(id) }
}
