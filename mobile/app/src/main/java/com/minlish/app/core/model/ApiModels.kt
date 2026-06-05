package com.minlish.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class GoogleLoginRequest(
    @SerialName("id_token") val idToken: String,
)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class UserDto(val id: String, val email: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserDto,
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    @SerialName("learning_goal") val learningGoal: String? = null,
    @SerialName("english_level") val englishLevel: String? = null,
    @SerialName("daily_new_words") val dailyNewWords: Int = 10,
    @SerialName("daily_review_limit") val dailyReviewLimit: Int = 50,
    @SerialName("notification_time") val notificationTime: String? = null,
    val timezone: String = "UTC",
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
) {
    val isComplete: Boolean get() = name.isNotBlank() && !learningGoal.isNullOrBlank() && !englishLevel.isNullOrBlank()
}

@Serializable
data class MeDto(val id: String, val email: String, val profile: ProfileDto)

@Serializable
data class ProfileUpdateRequest(
    val name: String? = null,
    @SerialName("learning_goal") val learningGoal: String? = null,
    @SerialName("english_level") val englishLevel: String? = null,
    @SerialName("daily_new_words") val dailyNewWords: Int? = null,
    @SerialName("daily_review_limit") val dailyReviewLimit: Int? = null,
    @SerialName("notification_time") val notificationTime: String? = null,
    val timezone: String? = null,
)

@Serializable
data class DeckDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class DeckRequest(
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class DeckUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
)

@Serializable
data class DictionaryWordDto(
    val word: String,
    val phonetic: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("part_of_speech") val partOfSpeech: String? = null,
    val meaning: String,
    @SerialName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    val collocations: List<String> = emptyList(),        // ← thêm
    @SerialName("related_words") val relatedWords: List<String> = emptyList(), // ← thêm
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val source: String = "dictionaryapi.dev",
)

@Serializable
data class WordLookupRequest(val word: String)

@Serializable
data class VocabItemDto(
    val id: String,
    @SerialName("deck_id") val deckId: String,
    val word: String,
    val pronunciation: String? = null,
    val meaning: String,
    @SerialName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    val collocations: List<String> = emptyList(),
    @SerialName("related_words") val relatedWords: List<String> = emptyList(),
    val note: String? = null,
    @SerialName("part_of_speech") val partOfSpeech: String? = null,
    val phonetic: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val source: String = "manual",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class VocabItemRequest(
    val word: String,
    val pronunciation: String? = null,
    val meaning: String,
    @SerialName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    val collocations: List<String> = emptyList(),
    @SerialName("related_words") val relatedWords: List<String> = emptyList(),
    val note: String? = null,
    @SerialName("part_of_speech") val partOfSpeech: String? = null,
    val phonetic: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val source: String = "manual",
)

@Serializable
data class VocabItemUpdateRequest(
    val word: String? = null,
    val pronunciation: String? = null,
    val meaning: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    val collocations: List<String>? = null,
    @SerialName("related_words") val relatedWords: List<String>? = null,
    val note: String? = null,
    @SerialName("part_of_speech") val partOfSpeech: String? = null,
    val phonetic: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val synonyms: List<String>? = null,
    val antonyms: List<String>? = null,
)

@Serializable
data class EnrichWordRequest(
    val word: String,
    val pronunciation: String? = null,
    val meaning: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    val collocations: List<String>? = null,
    @SerialName("related_words") val relatedWords: List<String>? = null,
    val note: String? = null,
    @SerialName("part_of_speech") val partOfSpeech: String? = null,
    val phonetic: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val synonyms: List<String>? = null,
    val antonyms: List<String>? = null,
)

@Serializable
data class ImportRowErrorDto(val row: Int, val message: String)

@Serializable
data class ImportReportDto(
    val imported: Int,
    val skipped: Int,
    val errors: List<ImportRowErrorDto> = emptyList(),
)

@Serializable
data class DailyPlanDto(
    @SerialName("new_words_target") val newWordsTarget: Int,
    @SerialName("review_limit") val reviewLimit: Int,
    @SerialName("new_words_due") val newWordsDue: Int,
    @SerialName("reviews_due") val reviewsDue: Int,
    @SerialName("total_due") val totalDue: Int,
)

@Serializable
data class DueWordDto(
    @SerialName("vocab_item") val vocabItem: VocabItemDto,
    val status: String,
    val repetitions: Int,
    @SerialName("interval_days") val intervalDays: Int,
    @SerialName("ease_factor") val easeFactor: Double,
    @SerialName("due_at") val dueAt: String,
)

@Serializable
data class ReviewRequest(@SerialName("vocab_item_id") val vocabItemId: String, val rating: String)

@Serializable
data class ReviewResponse(
    val status: String,
    val repetitions: Int,
    @SerialName("interval_days") val intervalDays: Int,
    @SerialName("ease_factor") val easeFactor: Double,
    @SerialName("due_at") val dueAt: String,
)

@Serializable
data class LearningSessionDto(
    val id: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("new_words_count") val newWordsCount: Int = 0,
    @SerialName("review_words_count") val reviewWordsCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
)

@Serializable
data class SessionEndRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("new_words_count") val newWordsCount: Int = 0,
    @SerialName("review_words_count") val reviewWordsCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
)

@Serializable
data class ProgressSummaryDto(
    @SerialName("learned_words") val learnedWords: Int,
    @SerialName("streak_days") val streakDays: Int,
    @SerialName("accuracy_percentage") val accuracyPercentage: Double,
    @SerialName("due_reviews") val dueReviews: Int,
    @SerialName("level_estimation") val levelEstimation: String,
)

@Serializable
data class DailyActivityDto(
    val date: String,
    val reviews: Int,
    val correct: Int,
    val wrong: Int,
)

@Serializable
data class RetentionDto(
    @SerialName("total_reviews") val totalReviews: Int,
    @SerialName("correct_reviews") val correctReviews: Int,
    @SerialName("wrong_reviews") val wrongReviews: Int,
    @SerialName("retention_percentage") val retentionPercentage: Double,
)

@Serializable
data class DeckProgressDto(
    @SerialName("deck_id") val deckId: String,
    @SerialName("total_words") val totalWords: Int,
    @SerialName("learned_words") val learnedWords: Int,
    @SerialName("due_reviews") val dueReviews: Int,
    @SerialName("accuracy_percentage") val accuracyPercentage: Double,
)

@Serializable
data class PracticeGenerateRequest(val limit: Int = 5)

@Serializable
data class PracticeQuestionDto(
    @SerialName("vocab_item_id") val vocabItemId: String,
    @SerialName("question_type") val questionType: String,
    val question: String,
    val options: List<String>,
)

@Serializable
data class PracticeSubmitRequest(
    @SerialName("vocab_item_id") val vocabItemId: String,
    @SerialName("user_answer") val userAnswer: String,
)

@Serializable
data class PracticeAnswerDto(
    val id: String,
    @SerialName("vocab_item_id") val vocabItemId: String,
    @SerialName("question_type") val questionType: String,
    val question: String,
    @SerialName("user_answer") val userAnswer: String,
    @SerialName("correct_answer") val correctAnswer: String,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("ai_feedback") val aiFeedback: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class NotificationSettingsRequest(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    @SerialName("notification_time") val notificationTime: String? = null,
    val timezone: String? = null,
)

@Serializable
data class NotificationSettingsDto(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerialName("notification_time") val notificationTime: String? = null,
    val timezone: String,
)

@Serializable
data class DeviceRegistrationRequest(val platform: String = "android", @SerialName("device_token") val deviceToken: String)

@Serializable
data class DeviceDto(val id: String, val platform: String, @SerialName("device_token") val deviceToken: String, @SerialName("is_active") val isActive: Boolean)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    val status: String,
    val message: String,
)

@Serializable
data class ExplainWordRequest(val word: String, @SerialName("deck_id") val deckId: String? = null)

@Serializable
data class ExplainWordDto(
    val word: String,
    @SerialName("simple_explanation") val simpleExplanation: String,
    val level: String,
    val goal: String,
    val examples: List<String>,
    val collocations: List<String>,
)

@Serializable
data class GenerateExamplesRequest(val word: String, val count: Int = 3, @SerialName("deck_id") val deckId: String? = null)

@Serializable
data class GenerateExamplesDto(val word: String, val examples: List<String>, val level: String, val goal: String)

@Serializable
data class GenerateDeckRequest(val topic: String, val limit: Int = 5, @SerialName("deck_id") val deckId: String? = null)

@Serializable
data class DeckSuggestionDto(val word: String, val reason: String)

@Serializable
data class GenerateDeckDto(val topic: String, val suggestions: List<DeckSuggestionDto>, val level: String, val goal: String)

@Serializable
data class ErrorEnvelope(val detail: ErrorDetail)

@Serializable
data class ErrorDetail(val code: String = "UNKNOWN_ERROR", val message: String = "Something went wrong.")
