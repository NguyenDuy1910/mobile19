package com.minlish.app.core.network

import com.minlish.app.BuildConfig

object NetworkConfig {
    const val LOCAL_ANDROID_EMULATOR_BASE_URL = "http://10.0.2.2:8000/"
    const val LOCAL_DEVICE_BASE_URL_EXAMPLE = "http://192.168.1.10:8000/"
    const val PRODUCTION_BASE_URL_PLACEHOLDER = "https://api.minlish.example/"

    val baseUrl: String = BuildConfig.API_BASE_URL.ensureTrailingSlash()

    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
}
