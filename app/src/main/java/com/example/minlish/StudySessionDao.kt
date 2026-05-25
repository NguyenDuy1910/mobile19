package com.example.minlish

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StudySessionDao {

    @Insert
    suspend fun insertSession(session: StudySessionEntity)

    @Query("SELECT COUNT(*) FROM study_session_table")
    suspend fun getTotalReviews(): Int

    @Query("SELECT COUNT(DISTINCT vocabularyId) FROM study_session_table WHERE reviewedAt >= :startOfDay AND reviewedAt < :endOfDay")
    suspend fun getWordsReviewedOnDay(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT reviewedAt FROM study_session_table ORDER BY reviewedAt DESC")
    suspend fun getAllReviewTimes(): List<Long>

    @Query("SELECT COUNT(*) FROM study_session_table WHERE result IN ('good', 'easy')")
    suspend fun getCorrectCount(): Int

    @Query("SELECT COUNT(*) FROM vocabulary_table WHERE nextReviewTime <= :currentTime")
    suspend fun getDueCount(currentTime: Long): Int

    @Query("SELECT DISTINCT vocabularyId FROM study_session_table")
    suspend fun getAllVocabularyIdsReviewed(): List<Int>

    @Query("SELECT COUNT(DISTINCT vocabularyId) FROM study_session_table")
    suspend fun getTotalReviewedWordsCount(): Int

    @Query("""
        SELECT COUNT(*) FROM (
            SELECT vocabularyId, MAX(reviewedAt) as lastReview
            FROM study_session_table
            GROUP BY vocabularyId
            HAVING (
                SELECT result FROM study_session_table s2 
                WHERE s2.vocabularyId = vocabularyId AND s2.reviewedAt = lastReview
            ) IN ('good', 'easy')
        )
    """)
    suspend fun getRetainedWordsCount(): Int

    // ── Thống kê theo Deck (feature #7) ───────────────────────────

    /** Số từ due trong một deck cụ thể */
    @Query("SELECT COUNT(*) FROM vocabulary_table WHERE deckId = :deckId AND nextReviewTime <= :currentTime")
    suspend fun getDueCountByDeck(deckId: Int, currentTime: Long): Int

    /** Tổng lượt review của các từ trong deck */
    @Query("""
        SELECT COUNT(*) FROM study_session_table s
        INNER JOIN vocabulary_table v ON s.vocabularyId = v.id
        WHERE v.deckId = :deckId
    """)
    suspend fun getTotalReviewsByDeck(deckId: Int): Int

    /** Số lượt đúng (good/easy) trong deck */
    @Query("""
        SELECT COUNT(*) FROM study_session_table s
        INNER JOIN vocabulary_table v ON s.vocabularyId = v.id
        WHERE v.deckId = :deckId AND s.result IN ('good', 'easy')
    """)
    suspend fun getCorrectCountByDeck(deckId: Int): Int

    /** Số từ trong deck đã được review ít nhất 1 lần */
    @Query("""
        SELECT COUNT(DISTINCT s.vocabularyId) FROM study_session_table s
        INNER JOIN vocabulary_table v ON s.vocabularyId = v.id
        WHERE v.deckId = :deckId
    """)
    suspend fun getLearnedCountByDeck(deckId: Int): Int
}