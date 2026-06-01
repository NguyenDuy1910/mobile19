package com.minlish.app.auth.data

import com.minlish.app.core.datastore.AuthTokenStore
import com.minlish.app.core.model.LoginRequest
import com.minlish.app.core.model.MeDto
import com.minlish.app.core.model.ProfileDto
import com.minlish.app.core.model.ProfileUpdateRequest
import com.minlish.app.core.model.RegisterRequest
import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.ApiResult
import com.minlish.app.core.network.safeApiCall

class AuthRepository(private val apiClient: ApiClient, private val tokenStore: AuthTokenStore) {
    val tokens = tokenStore.tokens

    suspend fun initialize() = tokenStore.initialize()

    suspend fun login(email: String, password: String): ApiResult<MeDto> =
        authenticate { apiClient.service.login(LoginRequest(email.trim(), password)) }

    suspend fun register(email: String, password: String): ApiResult<MeDto> =
        authenticate { apiClient.service.register(RegisterRequest(email.trim(), password)) }

    suspend fun me(): ApiResult<MeDto> = safeApiCall(apiClient.json) { apiClient.service.me() }

    suspend fun updateProfile(request: ProfileUpdateRequest): ApiResult<ProfileDto> =
        safeApiCall(apiClient.json) { apiClient.service.updateProfile(request) }

    suspend fun logout() {
        safeApiCall(apiClient.json) { apiClient.service.logout() }
        tokenStore.clear()
    }

    private suspend fun authenticate(block: suspend () -> com.minlish.app.core.model.TokenResponse): ApiResult<MeDto> =
        when (val result = safeApiCall(apiClient.json, block)) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                tokenStore.saveTokens(result.data.accessToken, result.data.refreshToken)
                me()
            }
        }
}
