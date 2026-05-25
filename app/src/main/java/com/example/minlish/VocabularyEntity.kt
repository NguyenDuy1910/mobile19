package com.example.minlish

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
@ColumnInfo(name = "synced") var synced: Boolean = false
@Entity(tableName = "vocabulary_table")
data class VocabularyEntity(


    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ── THÔNG TIN TỪ ──────────────────────────────────────

    // Firestore cần no-arg constructor → tất cả field phải có default value
    val word: String = "",

    val meaning: String = "",

    val pronunciation: String = "",

    val descriptionEn: String = "",

    val example: String = "",

    val collocation: String = "",

    val relatedWords: String = "",

    val note: String = "",

    // ── THUỘC VỀ DECK NÀO ─────────────────────────────────
    val deckId: Int = 0,

    // ── DỮ LIỆU SM-2 (SPACED REPETITION) ─────────────────
    val interval: Int = 1,

    val easeFactor: Double = 2.5,

    val nextReviewTime: Long = 0L,

    val isSynced: Boolean = false

)