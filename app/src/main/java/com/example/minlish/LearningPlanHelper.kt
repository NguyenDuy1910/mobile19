package com.example.minlish

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import java.util.*

object LearningPlanHelper {

    suspend fun getDailyPlan(
        context: Context,
        vocabDao: VocabularyDao,
        sessionDao: StudySessionDao
    ): List<VocabularyEntity> {
        val prefs = context.getSharedPreferences("learning_plan", Context.MODE_PRIVATE)
        val newWordsPerDay = prefs.getInt("new_words_per_day", 10)

        val now = System.currentTimeMillis()

        // 1. Lấy từ cần ôn (due)
        val dueWords = vocabDao.getDueVocabulary(now)

        // 2. Lấy từ mới (chưa có session review nào)
        val reviewedIds = sessionDao.getAllVocabularyIdsReviewed()
        val newWords = vocabDao.getAllVocabularyDirect()
            .filter { it.id !in reviewedIds }
            .take(newWordsPerDay)

        // 3. Gộp: ưu tiên từ due trước, sau đó đến từ mới
        val plan = mutableListOf<VocabularyEntity>()
        plan.addAll(dueWords)
        val remaining = newWordsPerDay - dueWords.size
        if (remaining > 0) {
            plan.addAll(newWords.take(remaining))
        }
        return plan
    }
}