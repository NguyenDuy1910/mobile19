package com.minlish.app.core.network

import com.minlish.app.core.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): TokenResponse
    @POST("auth/register") suspend fun register(@Body request: RegisterRequest): TokenResponse
    @POST("auth/login") suspend fun login(@Body request: LoginRequest): TokenResponse
    @POST("auth/logout") suspend fun logout(): MessageResponse
    @GET("me") suspend fun me(): MeDto
    @PATCH("me/profile") suspend fun updateProfile(@Body request: ProfileUpdateRequest): ProfileDto

    @GET("decks") suspend fun decks(): List<DeckDto>
    @POST("decks") suspend fun createDeck(@Body request: DeckRequest): DeckDto
    @GET("decks/{deckId}") suspend fun deck(@Path("deckId") deckId: String): DeckDto
    @PATCH("decks/{deckId}") suspend fun updateDeck(@Path("deckId") deckId: String, @Body request: DeckUpdateRequest): DeckDto
    @DELETE("decks/{deckId}") suspend fun deleteDeck(@Path("deckId") deckId: String)

    @GET("decks/{deckId}/words") suspend fun words(@Path("deckId") deckId: String): List<VocabItemDto>
    @POST("decks/{deckId}/words") suspend fun createWord(@Path("deckId") deckId: String, @Body request: VocabItemRequest): VocabItemDto
    @PATCH("words/{wordId}") suspend fun updateWord(@Path("wordId") wordId: String, @Body request: VocabItemUpdateRequest): VocabItemDto
    @DELETE("words/{wordId}") suspend fun deleteWord(@Path("wordId") wordId: String)
    @POST("words/lookup") suspend fun lookupWord(@Body request: WordLookupRequest): DictionaryWordDto
    @POST("decks/{deckId}/words/enrich") suspend fun enrichWord(@Path("deckId") deckId: String, @Body request: EnrichWordRequest): VocabItemDto
    @Multipart @POST("decks/{deckId}/import") suspend fun importWords(@Path("deckId") deckId: String, @Part file: MultipartBody.Part): ImportReportDto
    @Streaming @GET("decks/{deckId}/export") suspend fun exportWords(@Path("deckId") deckId: String): ResponseBody

    @GET("learning/daily-plan") suspend fun dailyPlan(): DailyPlanDto
    @GET("learning/due-words") suspend fun dueWords(): List<DueWordDto>
    @POST("learning/review") suspend fun review(@Body request: ReviewRequest): ReviewResponse
    @POST("learning/session/start") suspend fun startSession(): LearningSessionDto
    @POST("learning/session/end") suspend fun endSession(@Body request: SessionEndRequest): LearningSessionDto

    @GET("progress/summary") suspend fun progressSummary(): ProgressSummaryDto
    @GET("progress/daily-activity") suspend fun dailyActivity(): List<DailyActivityDto>
    @GET("progress/retention") suspend fun retention(): RetentionDto
    @GET("progress/decks/{deckId}") suspend fun deckProgress(@Path("deckId") deckId: String): DeckProgressDto

    @POST("practice/generate") suspend fun generatePractice(@Body request: PracticeGenerateRequest): List<PracticeQuestionDto>
    @POST("practice/submit") suspend fun submitPractice(@Body request: PracticeSubmitRequest): PracticeAnswerDto
    @GET("practice/history") suspend fun practiceHistory(): List<PracticeAnswerDto>

    @POST("notifications/register-device") suspend fun registerDevice(@Body request: DeviceRegistrationRequest): DeviceDto
    @PATCH("notifications/settings") suspend fun notificationSettings(@Body request: NotificationSettingsRequest): NotificationSettingsDto
    @POST("notifications/test") suspend fun testNotification(): NotificationDto

    @POST("agent/explain-word") suspend fun explainWord(@Body request: ExplainWordRequest): ExplainWordDto
    @POST("agent/generate-examples") suspend fun generateExamples(@Body request: GenerateExamplesRequest): GenerateExamplesDto
    @POST("agent/generate-deck") suspend fun generateDeck(@Body request: GenerateDeckRequest): GenerateDeckDto
}

internal interface RefreshApiService {
    @POST("auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<TokenResponse>
}
