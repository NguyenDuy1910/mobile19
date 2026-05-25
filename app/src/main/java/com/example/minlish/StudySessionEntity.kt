package com.example.minlish

import androidx.room.Entity
import androidx.room.PrimaryKey

// Mỗi lần người dùng đánh giá 1 từ (Again/Good/Easy) → lưu 1 record vào bảng này
// Dùng để tính: streak, số từ đã học, accuracy
@Entity(tableName = "study_session_table")
data class StudySessionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID của từ vừa được review
    val vocabularyId: Int,

    // Kết quả đánh giá: "again", "good", "easy"
    val result: String,

    // Thời điểm review (Unix timestamp)
    // Dùng để group theo ngày → tính streak
    val reviewedAt: Long = System.currentTimeMillis()
)