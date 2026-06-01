package com.minlish.app.vocabulary.data

import com.minlish.app.core.model.EnrichWordRequest
import com.minlish.app.core.model.VocabItemRequest
import com.minlish.app.core.model.VocabItemUpdateRequest
import com.minlish.app.core.model.WordLookupRequest
import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.safeApiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class VocabularyRepository(private val apiClient: ApiClient) {
    suspend fun list(deckId: String) = safeApiCall(apiClient.json) { apiClient.service.words(deckId) }
    suspend fun create(deckId: String, request: VocabItemRequest) = safeApiCall(apiClient.json) { apiClient.service.createWord(deckId, request) }
    suspend fun enrich(deckId: String, request: EnrichWordRequest) = safeApiCall(apiClient.json) { apiClient.service.enrichWord(deckId, request) }
    suspend fun lookup(word: String) = safeApiCall(apiClient.json) { apiClient.service.lookupWord(WordLookupRequest(word)) }
    suspend fun update(id: String, request: VocabItemUpdateRequest) = safeApiCall(apiClient.json) { apiClient.service.updateWord(id, request) }
    suspend fun delete(id: String) = safeApiCall(apiClient.json) { apiClient.service.deleteWord(id) }
    suspend fun export(deckId: String) = safeApiCall(apiClient.json) { apiClient.service.exportWords(deckId).string() }
    suspend fun import(deckId: String, fileName: String, content: ByteArray) = safeApiCall(apiClient.json) {
        apiClient.service.importWords(
            deckId,
            MultipartBody.Part.createFormData("file", fileName, content.toRequestBody("text/csv".toMediaType())),
        )
    }
}
