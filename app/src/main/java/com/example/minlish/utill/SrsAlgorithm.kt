// PATH: app/src/main/java/com/example/minlish/util/SrsAlgorithm.kt
package com.example.minlish.util

import com.example.minlish.VocabularyEntity

/**
 * Thuật toán SM-2 (SuperMemo 2)
 * Input: từ hiện tại + rating người dùng (0–4)
 * Output: VocabularyEntity mới với interval/easeFactor/nextReviewTime đã cập nhật
 *
 * Rating mapping:
 *   0 = Again (blackout)
 *   1 = Hard
 *   2 = Good
 *   3 = Easy
 */
object SrsAlgorithm {

    private const val MIN_EASE = 1.3

    /**
     * Tính toán lịch ôn tiếp theo.
     * @param vocab Từ cần cập nhật
     * @param rating 0=Again, 1=Hard, 2=Good, 3=Easy
     * @return VocabularyEntity mới đã cập nhật SRS fields
     */
    fun calculateNextReview(vocab: VocabularyEntity, rating: Int): VocabularyEntity {
        val q = rating // q = quality (0-3)

        // Tính ease factor mới theo công thức SM-2
        val newEase = maxOf(
            MIN_EASE,
            vocab.easeFactor + (0.1 - (3 - q) * (0.08 + (3 - q) * 0.02))
        )

        // Tính interval mới
        val newInterval = when {
            q == 0 -> 1          // Again → ôn lại sau 1 phút (trong session) hoặc 1 ngày
            q == 1 -> maxOf(1, (vocab.interval * 1.2).toInt()) // Hard → tăng ít
            vocab.interval == 1 -> 6       // Good lần 1 → 6 ngày
            vocab.interval == 6 -> (6 * newEase).toInt() // Good lần 2
            else -> (vocab.interval * newEase).toInt()   // Good/Easy tiếp theo
        }

        // nextReviewTime = bây giờ + interval ngày
        val nextTime = System.currentTimeMillis() + newInterval * 24L * 60 * 60 * 1000

        return vocab.copy(
            interval        = newInterval,
            easeFactor      = newEase,
            nextReviewTime  = nextTime
        )
    }

    /** Tên hiển thị cho rating */
    fun ratingLabel(rating: Int): String = when (rating) {
        0 -> "Again"
        1 -> "Hard"
        2 -> "Good"
        3 -> "Easy"
        else -> "?"
    }

    /** Màu cho nút rating */
    fun ratingColor(rating: Int): String = when (rating) {
        0 -> "#F44336"  // đỏ
        1 -> "#FF9800"  // cam
        2 -> "#4CAF50"  // xanh lá
        3 -> "#2196F3"  // xanh dương
        else -> "#9E9E9E"
    }
}