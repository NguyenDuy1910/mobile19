package com.minlish.app.core.network

import com.minlish.app.core.datastore.AuthTokenStore
import com.minlish.app.core.model.RefreshRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiClient(
    private val tokenStore: AuthTokenStore,
    private val baseUrl: String = NetworkConfig.baseUrl,
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val service: ApiService by lazy {
        retrofit(authenticatedClient()).create(ApiService::class.java)
    }

    private fun authenticatedClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .authenticator(TokenAuthenticator(tokenStore, refreshService()))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun refreshService(): RefreshApiService =
        retrofit(OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build())
            .create(RefreshApiService::class.java)

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

private class AuthInterceptor(private val tokenStore: AuthTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = tokenStore.currentAccessToken?.let { chain.request().authorized(it) } ?: chain.request()
        return chain.proceed(request)
    }
}

private class TokenAuthenticator(
    private val tokenStore: AuthTokenStore,
    private val refreshApi: RefreshApiService,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        synchronized(this) {
            val staleToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val cachedToken = tokenStore.currentAccessToken
            if (!cachedToken.isNullOrBlank() && cachedToken != staleToken) {
                return response.request.authorized(cachedToken)
            }
            val refreshToken = tokenStore.currentRefreshToken ?: return null
            return runCatching {
                val refreshed = refreshApi.refresh(RefreshRequest(refreshToken)).execute()
                val tokens = refreshed.body()
                if (!refreshed.isSuccessful || tokens == null) {
                    runBlocking { tokenStore.clear() }
                    null
                } else {
                    runBlocking { tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken) }
                    response.request.authorized(tokens.accessToken)
                }
            }.getOrElse {
                runBlocking { tokenStore.clear() }
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}

private fun Request.authorized(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
