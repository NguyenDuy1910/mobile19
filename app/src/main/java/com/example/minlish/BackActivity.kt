package com.example.minlish

// ════════════════════════════════════════════════════════════════
//  BackActivity.kt — MinLish
//  Base class cho tất cả Activity con (không phải MainActivity)
//
//  Cách dùng: thay "AppCompatActivity" bằng "BackActivity"
//  trong: DashboardActivity, DeckActivity, FlashcardActivity,
//         AddVocabularyActivity, EditVocabularyActivity,
//         CsvImportActivity, DeckDetailActivity, QuizActivity
//
//  Tác dụng:
//    ✅ Tự thêm nút back ←
//    ✅ Tự ẩn tên Activity (không hiện "Hồ sơ", "Dashboard"...)
//    ✅ Xử lý onSupportNavigateUp() → finish()
// ════════════════════════════════════════════════════════════════

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

abstract class BackActivity : AppCompatActivity() {

    /**
     * Gọi hàm này trong onCreate() của Activity con sau setContentView().
     * Nếu layout có <Toolbar android:id="@+id/toolbar">, sẽ dùng toolbar đó.
     * Nếu không có toolbar trong layout, fallback sang supportActionBar.
     */
    protected fun setupBackToolbar(title: String = "") {
        // Thử tìm Toolbar trong layout
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }
        // Bật nút back
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Ẩn tên nếu không truyền title, hoặc hiện title nếu có
        if (title.isEmpty()) {
            supportActionBar?.setDisplayShowTitleEnabled(false)
        } else {
            supportActionBar?.setDisplayShowTitleEnabled(true)
            supportActionBar?.title = title
        }
    }

    // Nhấn nút back trên toolbar → đóng Activity, quay về màn trước
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}