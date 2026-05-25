package com.example.minlish

import androidx.room.Entity
import androidx.room.PrimaryKey

// Deck = bộ từ vựng, ví dụ: "IELTS Academic", "Giao tiếp cơ bản"
// Mỗi VocabularyEntity có deckId trỏ về id của bảng này
@Entity(tableName = "deck_table")
data class DeckEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Tên deck, ví dụ: "IELTS Academic"
    val name: String,

    // Mô tả ngắn, ví dụ: "Từ vựng học thuật cho IELTS 7.0+"
    val description: String = "",

    // Tags phân loại, lưu dạng chuỗi ngăn cách bằng dấu phẩy
    // ví dụ: "IELTS,Academic,Writing"
    // Lý do lưu String thay vì List: Room không lưu được List trực tiếp
    val tags: String = "",

    // Thời điểm tạo deck (Unix timestamp)
    // dùng để sort theo ngày tạo
    val createdAt: Long = System.currentTimeMillis()
)