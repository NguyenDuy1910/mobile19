package com.example.minlish

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface VocabularyDao {

    // Insert thường — dùng khi thêm từ mới (autoGenerate id)
    @Insert
    suspend fun insertVocabulary(vocabulary: VocabularyEntity): Long

    // Upsert — dùng khi sync từ Firestore (giữ nguyên id, update nếu đã tồn tại)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVocabulary(vocabulary: VocabularyEntity): Long

    @Update
    suspend fun updateVocabulary(vocabulary: VocabularyEntity)

    @Delete
    suspend fun deleteVocabulary(vocabulary: VocabularyEntity)

    // Lấy toàn bộ từ
    @Query("SELECT * FROM vocabulary_table ORDER BY id DESC")
    fun getAllVocabulary(): LiveData<List<VocabularyEntity>>

    // Lấy 1 từ theo ID — dùng cho EditVocabularyActivity
    @Query("SELECT * FROM vocabulary_table WHERE id = :id LIMIT 1")
    suspend fun getVocabularyById(id: Int): VocabularyEntity?

    // Kiểm tra từ đã tồn tại chưa — dùng trong AddVocabularyActivity để tránh trùng
    @Query("SELECT * FROM vocabulary_table WHERE word = :word LIMIT 1")
    suspend fun getVocabularyByWord(word: String): VocabularyEntity?

    // Tìm kiếm theo từ hoặc nghĩa
    @Query("SELECT * FROM vocabulary_table WHERE word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%'")
    fun searchVocabulary(query: String): LiveData<List<VocabularyEntity>>

    // Lấy từ theo deck
    @Query("SELECT * FROM vocabulary_table WHERE deckId = :deckId ORDER BY id DESC")
    fun getVocabularyByDeck(deckId: Int): LiveData<List<VocabularyEntity>>

    // Lấy từ theo deck — dùng cho Export CSV (suspend)
    @Query("SELECT * FROM vocabulary_table WHERE deckId = :deckId ORDER BY id DESC")
    suspend fun getVocabularyByDeckDirect(deckId: Int): List<VocabularyEntity>

    // Từ due toàn bộ
    @Query("SELECT * FROM vocabulary_table WHERE nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC")
    suspend fun getDueVocabulary(currentTime: Long): List<VocabularyEntity>

    // Từ due theo deck
    @Query("SELECT * FROM vocabulary_table WHERE deckId = :deckId AND nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC")
    suspend fun getDueVocabularyByDeck(deckId: Int, currentTime: Long): List<VocabularyEntity>

    // Tổng số từ
    @Query("SELECT COUNT(*) FROM vocabulary_table")
    suspend fun getTotalCount(): Int

    // Debug — lấy thẳng không qua LiveData
    @Query("SELECT * FROM vocabulary_table")
    suspend fun getAllVocabularyDirect(): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary_table")
    fun getAllVocabularySync(): List<VocabularyEntity>

    @Query("DELETE FROM vocabulary_table WHERE deckId = :deckId")
    suspend fun deleteVocabularyByDeck(deckId: Int)

    /**
     * Lấy nextReviewTime nhỏ nhất trong toàn bộ từ vựng.
     * Dùng bởi SmartReminderScheduler để biết từ nào due sớm nhất.
     * Trả về null nếu bảng rỗng.
     */
    @Query("SELECT MIN(nextReviewTime) FROM vocabulary_table WHERE nextReviewTime > 0")
    suspend fun getEarliestNextReviewTime(): Long?
}