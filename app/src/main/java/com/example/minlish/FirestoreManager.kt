package com.example.minlish

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

object FirestoreManager {

    private val db = FirebaseFirestore.getInstance()

    private val uid: String?
        get() = AuthManager.currentUser?.uid

    // ── VOCABULARY ─────────────────────────────────────────

    fun syncVocabulary(vocab: VocabularyEntity) {
        val uid = uid ?: run {
            android.util.Log.e("FIRESTORE", "uid null — chưa đăng nhập")
            return
        }
        android.util.Log.d("FIRESTORE", "Syncing vocab: ${vocab.word}, uid=$uid")

        db.collection("users")
            .document(uid)
            .collection("vocabulary")
            .document(vocab.id.toString())
            .set(vocab)
            .addOnSuccessListener {
                android.util.Log.d("FIRESTORE", "Sync thành công: ${vocab.word}")
            }
            .addOnFailureListener {
                android.util.Log.e("FIRESTORE", "Sync thất bại: ${it.message}")
            }
    }
    fun deleteVocabulary(vocab: VocabularyEntity) {
        val uid = uid ?: return
        db.collection("users")
            .document(uid)
            .collection("vocabulary")
            .document(vocab.id.toString())
            .delete()
    }

    fun fetchAllVocabulary(onResult: (List<VocabularyEntity>) -> Unit) {
        val uid = uid ?: return
        db.collection("users")
            .document(uid)
            .collection("vocabulary")
            .get()
            .addOnSuccessListener { snapshot: QuerySnapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(VocabularyEntity::class.java)
                }
                onResult(list)
            }
    }

    // ── DECK ───────────────────────────────────────────────

    fun syncDeck(deck: DeckEntity) {
        val uid = uid ?: return
        db.collection("users")
            .document(uid)
            .collection("decks")
            .document(deck.id.toString())
            .set(deck)
    }

    fun deleteDeck(deck: DeckEntity) {
        val uid = uid ?: return
        db.collection("users")
            .document(uid)
            .collection("decks")
            .document(deck.id.toString())
            .delete()
    }

    fun fetchAllDecks(onResult: (List<DeckEntity>) -> Unit) {
        val uid = uid ?: return
        db.collection("users")
            .document(uid)
            .collection("decks")
            .get()
            .addOnSuccessListener { snapshot: QuerySnapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DeckEntity::class.java)
                }
                onResult(list)
            }
    }
}