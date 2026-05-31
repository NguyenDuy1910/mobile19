package com.minlish.app.core.network

import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiResultTest {
    @Test
    fun ioFailureBecomesFriendlyNetworkError() = runBlocking {
        val result = safeApiCall<String>(Json) { throw IOException("offline") }

        assertEquals("NETWORK_ERROR", (result as ApiResult.Error).code)
        assertEquals("Cannot reach MinLish. Check your connection and backend URL.", result.message)
    }
}
