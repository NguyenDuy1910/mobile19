package com.example.minlish

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SyncManager {

    private const val TAG = "SyncManager"

    // SharedPreferences key để ghi nhớ lần sync cuối
    private const val PREFS_NAME   = "sync_prefs"
    private const val KEY_LAST_SYNC = "last_sync_time"

    /**
     * Gọi ngay sau khi đăng nhập thành công (trong LoginActivity.goToMain).
     *
     * Logic:
     *  1. Fetch tất cả decks + vocabulary từ Firestore của user.
     *  2. Upsert vào Room (REPLACE nếu đã tồn tại — giữ dữ liệu mới nhất từ cloud).
     *  3. Gọi [onDone] trên main thread khi xong (hoặc khi có lỗi, để app không bị treo).
     *
     * [onSyncStatus]: callback tuỳ chọn để cập nhật UI loading ("Đang đồng bộ...", "Xong").
     * Truyền null nếu không cần hiển thị trạng thái.
     */
    fun syncOnLogin(
        context: Context,
        onSyncStatus: ((String) -> Unit)? = null,
        onDone: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val uid = AuthManager.currentUser?.uid
            if (uid == null) {
                Log.w(TAG, "syncOnLogin: uid null — bỏ qua sync")
                withContext(Dispatchers.Main) { onDone() }
                return@launch
            }

            val db       = AppDatabase.getInstance(context)
            val vocabDao = db.vocabularyDao()
            val deckDao  = db.deckDao()
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            // ── 1. SYNC DECKS ──────────────────────────────────────────────
            withContext(Dispatchers.Main) {
                onSyncStatus?.invoke("⏳ Đang đồng bộ bộ thẻ...")
            }
            try {
                val deckSnapshot = firestore
                    .collection("users").document(uid)
                    .collection("decks")
                    .get().await()

                val decks = deckSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(DeckEntity::class.java)
                }

                // Upsert: insert nếu chưa có, REPLACE nếu đã có (dữ liệu cloud ưu tiên)
                for (deck in decks) {
                    deckDao.upsertDeck(deck)
                }

                Log.d(TAG, "Synced ${decks.size} decks from Firestore")

            } catch (e: Exception) {
                Log.e(TAG, "Deck sync failed: ${e.message}")
                // Không return — tiếp tục sync vocabulary dù deck lỗi
            }

            // ── 2. SYNC VOCABULARY ─────────────────────────────────────────
            withContext(Dispatchers.Main) {
                onSyncStatus?.invoke("⏳ Đang đồng bộ từ vựng...")
            }
            try {
                val vocabSnapshot = firestore
                    .collection("users").document(uid)
                    .collection("vocabulary")
                    .get().await()

                val words = vocabSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(VocabularyEntity::class.java)
                }

                // Upsert: insert nếu chưa có, REPLACE nếu đã có
                for (word in words) {
                    vocabDao.upsertVocabulary(word)
                }

                Log.d(TAG, "Synced ${words.size} words from Firestore")

                // Ghi lại thời điểm sync thành công
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    .apply()

            } catch (e: Exception) {
                Log.e(TAG, "Vocab sync failed: ${e.message}")
            }

            // ── 3. XONG ────────────────────────────────────────────────────
            withContext(Dispatchers.Main) {
                onSyncStatus?.invoke("✅ Đồng bộ hoàn tất")
                onDone()
            }
        }
    }

    /**
     * Trả về thời điểm lần sync cuối (epoch ms), hoặc 0 nếu chưa bao giờ sync.
     */
    fun getLastSyncTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC, 0L)
    }
}