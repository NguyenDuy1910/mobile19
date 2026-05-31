package com.minlish.app.core.network

import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val code: String = "UNKNOWN_ERROR", val statusCode: Int? = null) : ApiResult<Nothing>
}

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

internal suspend fun <T> safeApiCall(json: Json, block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (exception: HttpException) {
    val detail = runCatching {
        json.decodeFromString<com.minlish.app.core.model.ErrorEnvelope>(
            exception.response()?.errorBody()?.string().orEmpty(),
        ).detail
    }.getOrNull()
    ApiResult.Error(
        message = detail?.message ?: userFriendlyHttpMessage(exception.code()),
        code = detail?.code ?: "HTTP_${exception.code()}",
        statusCode = exception.code(),
    )
} catch (_: IOException) {
    ApiResult.Error("Cannot reach MinLish. Check your connection and backend URL.", "NETWORK_ERROR")
} catch (_: SerializationException) {
    ApiResult.Error("MinLish received an unexpected server response.", "INVALID_RESPONSE")
} catch (_: Exception) {
    ApiResult.Error("Something went wrong. Please try again.", "UNKNOWN_ERROR")
}

private fun userFriendlyHttpMessage(code: Int): String = when (code) {
    401 -> "Your session expired. Please sign in again."
    403 -> "You do not have access to this item."
    404 -> "The requested item was not found."
    422 -> "Please check the fields and try again."
    in 500..599 -> "The MinLish backend is unavailable right now."
    else -> "Request failed. Please try again."
}
