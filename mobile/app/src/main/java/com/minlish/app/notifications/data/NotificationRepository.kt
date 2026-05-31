package com.minlish.app.notifications.data

import com.minlish.app.core.model.DeviceRegistrationRequest
import com.minlish.app.core.model.NotificationSettingsRequest
import com.minlish.app.core.network.ApiClient
import com.minlish.app.core.network.safeApiCall

class NotificationRepository(private val apiClient: ApiClient) {
    suspend fun registerPlaceholderDevice(token: String) = safeApiCall(apiClient.json) {
        apiClient.service.registerDevice(DeviceRegistrationRequest(deviceToken = token))
    }
    suspend fun update(request: NotificationSettingsRequest) = safeApiCall(apiClient.json) { apiClient.service.notificationSettings(request) }
    suspend fun test() = safeApiCall(apiClient.json) { apiClient.service.testNotification() }
}
