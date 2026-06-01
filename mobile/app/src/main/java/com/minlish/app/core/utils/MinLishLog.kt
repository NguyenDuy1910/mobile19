package com.minlish.app.core.utils

import android.util.Log

/**
 * Centralized debug logger for MinLish.
 * Filter in Logcat with tag: "MinLish"
 *
 * Usage:
 *   MinLishLog.d("HomeScreen", "Loaded ${decks.size} decks")
 *   MinLishLog.e("AuthVM", "Login failed: $message")
 */
object MinLishLog {
    private const val TAG = "MinLish"

    fun d(screen: String, message: String) {
        Log.d(TAG, "[$screen] $message")
    }

    fun i(screen: String, message: String) {
        Log.i(TAG, "[$screen] $message")
    }

    fun w(screen: String, message: String) {
        Log.w(TAG, "[$screen] $message")
    }

    fun e(screen: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$screen] $message", throwable)
        } else {
            Log.e(TAG, "[$screen] $message")
        }
    }

    /** Log screen navigation event */
    fun nav(route: String) {
        Log.d(TAG, "[NAV] → $route")
    }

    /** Log state change in ViewModel */
    fun state(viewModel: String, state: String) {
        Log.d(TAG, "[STATE] $viewModel: $state")
    }

    /** Log API call */
    fun api(endpoint: String, result: String) {
        Log.d(TAG, "[API] $endpoint → $result")
    }
}

