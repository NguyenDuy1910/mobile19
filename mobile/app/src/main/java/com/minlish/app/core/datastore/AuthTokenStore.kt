package com.minlish.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth")

data class AuthTokens(
    val accessToken: String?,
    val refreshToken: String?,
) {
    val isAuthenticated: Boolean get() = !accessToken.isNullOrBlank()
}

class AuthTokenStore(
    private val context: Context,
) {
    private val cache = MutableStateFlow(AuthTokens(null, null))

    val tokens: Flow<AuthTokens> = context.authDataStore.data.map { preferences ->
        AuthTokens(
            accessToken = preferences[ACCESS_TOKEN_KEY],
            refreshToken = preferences[REFRESH_TOKEN_KEY],
        )
    }

    val currentAccessToken: String? get() = cache.value.accessToken
    val currentRefreshToken: String? get() = cache.value.refreshToken

    suspend fun initialize(): AuthTokens {
        val stored = tokens.first()
        cache.value = stored
        return stored
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.authDataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
        cache.value = AuthTokens(accessToken, refreshToken)
    }

    suspend fun clear() {
        context.authDataStore.edit { preferences ->
            preferences.clear()
        }
        cache.value = AuthTokens(null, null)
    }

    private companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
}
