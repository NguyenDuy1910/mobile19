package com.example.minlish.repository

import com.example.minlish.VocabularyDao
import com.example.minlish.VocabularyEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class VocabularyRepository(
    private val vocabularyDao: VocabularyDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lấy ID người dùng hiện tại từ Firebase Auth
    private val userId: String?
        get() = auth.currentUser?.uid

    /**
     * THÊM TỪ VỰNG: Lưu vào Local trước, sau đó đẩy lên Cloud
     */
    suspend fun insertVocabulary(vocabulary: VocabularyEntity) {
        withContext(Dispatchers.IO) {
            // 1. Lưu vào SQLite (Room)
            val newId = vocabularyDao.insertVocabulary(vocabulary)

            // 2. Tạo bản sao với ID vừa sinh ra để đồng bộ Firestore
            val vocabWithId = vocabulary.copy(id = newId.toInt())
            syncSingleToFirestore(vocabWithId)
        }
    }

    /**
     * CẬP NHẬT: Sửa ở Local và cập nhật trên Cloud
     */
    suspend fun updateVocabulary(vocabulary: VocabularyEntity) {
        withContext(Dispatchers.IO) {
            vocabularyDao.updateVocabulary(vocabulary)
            syncSingleToFirestore(vocabulary)
        }
    }

    /**
     * XÓA: Xóa ở Local và xóa trên Cloud
     */
    suspend fun deleteVocabulary(vocabulary: VocabularyEntity) {
        withContext(Dispatchers.IO) {
            vocabularyDao.deleteVocabulary(vocabulary)

            userId?.let { uid ->
                try {
                    firestore.collection("users").document(uid)
                        .collection("vocabulary").document(vocabulary.id.toString())
                        .delete()
                        .await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * LẤY DANH SÁCH: Trả về Flow/LiveData từ Room
     */
    fun getAllVocabulary() = vocabularyDao.getAllVocabulary()

    /**
     * ĐỒNG BỘ TẤT CẢ: Đẩy toàn bộ dữ liệu từ Local lên Firestore (Dùng cho nút Sync)
     */
    suspend fun syncAllToFirestore() {
        val uid = userId ?: return
        withContext(Dispatchers.IO) {
            val allVocabs = vocabularyDao.getAllVocabularySync() // Cần thêm hàm này vào DAO
            for (vocab in allVocabs) {
                syncSingleToFirestore(vocab)
            }
        }
    }

    /**
     * Hàm phụ trợ: Đẩy một Document lên Firestore
     */
    private suspend fun syncSingleToFirestore(vocab: VocabularyEntity) {
        val uid = userId ?: return
        try {
            // Chuyển Entity sang Map (đảm bảo dùng đúng tên biến: nextReviewTime)
            val data = hashMapOf(
                "id" to vocab.id,
                "word" to vocab.word,
                "pronunciation" to vocab.pronunciation,
                "meaning" to vocab.meaning,
                "descriptionEn" to vocab.descriptionEn,
                "example" to vocab.example,
                "collocation" to vocab.collocation,
                "relatedWords" to vocab.relatedWords,
                "note" to vocab.note,
                "deckId" to vocab.deckId,
                "easeFactor" to vocab.easeFactor,
                "interval" to vocab.interval,
                "nextReviewTime" to vocab.nextReviewTime, // Đã sửa đúng tên biến của bạn
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(uid)
                .collection("vocabularies").document(vocab.id.toString())
                .set(data)
                .await()
        } catch (e: Exception) {
            // Nếu lỗi (ví dụ offline), lệnh await sẽ ném ngoại lệ.
            // Ở đây có thể xử lý đánh dấu 'isSynced = false' nếu cần.
            e.printStackTrace()
        }
    }
}