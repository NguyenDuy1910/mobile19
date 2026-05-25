package com.example.minlish

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DeckDao {

    // Insert thường — dùng khi tạo deck mới (autoGenerate id)
    @Insert
    suspend fun insertDeck(deck: DeckEntity)

    // Upsert — dùng khi sync từ Firestore (giữ nguyên id, update nếu đã tồn tại)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeck(deck: DeckEntity)

    // Cập nhật deck (sửa tên, mô tả...)
    @Update
    suspend fun updateDeck(deck: DeckEntity)

    // Xóa deck
    // Lưu ý: xóa deck KHÔNG tự xóa từ trong deck
    // → cần xử lý riêng ở tầng trên (Repository)
    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    // Lấy toàn bộ deck, trả về LiveData để UI tự cập nhật khi có thay đổi
    @Query("SELECT * FROM deck_table ORDER BY createdAt DESC")
    fun getAllDecks(): LiveData<List<DeckEntity>>

    // Lấy 1 deck theo id — dùng khi cần xem chi tiết hoặc edit
    @Query("SELECT * FROM deck_table WHERE id = :deckId")
    suspend fun getDeckById(deckId: Int): DeckEntity?

    // Đếm số từ trong 1 deck — dùng để hiển thị "12 từ" trên card deck
    @Query("SELECT COUNT(*) FROM vocabulary_table WHERE deckId = :deckId")
    suspend fun countWordsInDeck(deckId: Int): Int

    @Query("SELECT * FROM deck_table")
    suspend fun getAllDecksDirect(): List<DeckEntity>
}