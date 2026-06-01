package com.minlish.app.app

import android.content.Context
import com.minlish.app.agent.data.AgentRepository
import com.minlish.app.auth.data.AuthRepository
import com.minlish.app.core.datastore.AuthTokenStore
import com.minlish.app.core.network.ApiClient
import com.minlish.app.deck.data.DeckRepository
import com.minlish.app.home.data.HomeRepository
import com.minlish.app.learning.data.LearningRepository
import com.minlish.app.notifications.data.NotificationRepository
import com.minlish.app.practice.data.PracticeRepository
import com.minlish.app.progress.data.ProgressRepository
import com.minlish.app.vocabulary.data.VocabularyRepository

class AppContainer(context: Context) {
    private val tokenStore = AuthTokenStore(context.applicationContext)
    private val apiClient = ApiClient(tokenStore)

    val authRepository = AuthRepository(apiClient, tokenStore)
    val deckRepository = DeckRepository(apiClient)
    val vocabularyRepository = VocabularyRepository(apiClient)
    val learningRepository = LearningRepository(apiClient)
    val progressRepository = ProgressRepository(apiClient)
    val practiceRepository = PracticeRepository(apiClient)
    val notificationRepository = NotificationRepository(apiClient)
    val agentRepository = AgentRepository(apiClient)
    val homeRepository = HomeRepository(authRepository, deckRepository, learningRepository, progressRepository)
}
