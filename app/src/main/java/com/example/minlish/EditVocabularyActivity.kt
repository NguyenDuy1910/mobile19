package com.example.minlish

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class EditVocabularyActivity : AppCompatActivity() {

    private var deckEntityList = listOf<DeckEntity>()
    private var selectedDeckId = 0
    private var vocabularyId   = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_vocabulary)

        supportActionBar?.title = "Chỉnh sửa từ"

        // Bind UI
        val spinnerDeck     = findViewById<Spinner>(R.id.spinnerDeck)
        val etWord          = findViewById<EditText>(R.id.etWord)
        val etPronunciation = findViewById<EditText>(R.id.etPronunciation)
        val etMeaning       = findViewById<EditText>(R.id.etMeaning)
        val etDescriptionEn = findViewById<EditText>(R.id.etDescriptionEn)
        val etExample       = findViewById<EditText>(R.id.etExample)
        val etCollocation   = findViewById<EditText>(R.id.etCollocation)
        val etRelatedWords  = findViewById<EditText>(R.id.etRelatedWords)
        val etNote          = findViewById<EditText>(R.id.etNote)
        val btnSave         = findViewById<Button>(R.id.btnSave)
        val btnDelete       = findViewById<Button>(R.id.btnDelete)
        val btnCancel       = findViewById<Button>(R.id.btnCancel)

        val db  = AppDatabase.getInstance(applicationContext)
        val dao = db.vocabularyDao()

        // Lấy vocabularyId được truyền từ màn hình trước
        vocabularyId = intent.getIntExtra("vocabularyId", 0)
        if (vocabularyId == 0) {
            finish()
            return
        }

        // ── LOAD DỮ LIỆU TỪ ──────────────────────────────────
        // Load từ từ DB rồi điền vào các field
        lifecycleScope.launch {
            val word = dao.getVocabularyById(vocabularyId)
            if (word == null) {
                finish()
                return@launch
            }

            runOnUiThread {
                etWord.setText(word.word)
                etPronunciation.setText(word.pronunciation)
                etMeaning.setText(word.meaning)
                etDescriptionEn.setText(word.descriptionEn)
                etExample.setText(word.example)
                etCollocation.setText(word.collocation)
                etRelatedWords.setText(word.relatedWords)
                etNote.setText(word.note)
                selectedDeckId = word.deckId
            }
        }

        // ── SPINNER DECK ──────────────────────────────────────
        db.deckDao().getAllDecks().observe(this) { decks ->
            deckEntityList = decks
            val deckNames = mutableListOf("Chưa chọn deck")
            deckNames.addAll(decks.map { it.name })

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deckNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDeck.adapter = adapter

            // Tự động chọn deck hiện tại của từ
            val index = decks.indexOfFirst { it.id == selectedDeckId }
            if (index != -1) spinnerDeck.setSelection(index + 1)
        }

        spinnerDeck.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedDeckId = if (position == 0) 0 else deckEntityList[position - 1].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) { selectedDeckId = 0 }
        }

        // ── LƯU THAY ĐỔI ─────────────────────────────────────
        btnSave.setOnClickListener {
            val word    = etWord.text.toString().trim()
            val meaning = etMeaning.text.toString().trim()

            if (word.isEmpty()) {
                etWord.error = "Vui lòng nhập từ vựng"
                return@setOnClickListener
            }
            if (meaning.isEmpty()) {
                etMeaning.error = "Vui lòng nhập nghĩa"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Lấy từ cũ để giữ lại interval, easeFactor, nextReviewTime
                val oldWord = dao.getVocabularyById(vocabularyId) ?: return@launch

                val updated = oldWord.copy(
                    word          = word,
                    pronunciation = etPronunciation.text.toString().trim(),
                    meaning       = meaning,
                    descriptionEn = etDescriptionEn.text.toString().trim(),
                    example       = etExample.text.toString().trim(),
                    collocation   = etCollocation.text.toString().trim(),
                    relatedWords  = etRelatedWords.text.toString().trim(),
                    note          = etNote.text.toString().trim(),
                    deckId        = selectedDeckId
                )

                dao.updateVocabulary(updated)
                FirestoreManager.syncVocabulary(updated)

                runOnUiThread {
                    Toast.makeText(this@EditVocabularyActivity, "Đã lưu!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        // ── XÓA TỪ ───────────────────────────────────────────
        // Hiện dialog xác nhận trước khi xóa
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa từ")
                .setMessage("Bạn có chắc muốn xóa từ này không?")
                .setPositiveButton("Xóa") { _, _ ->
                    lifecycleScope.launch {
                        val word = dao.getVocabularyById(vocabularyId) ?: return@launch
                        dao.deleteVocabulary(word)
                        FirestoreManager.deleteVocabulary(word)
                        runOnUiThread {
                            Toast.makeText(this@EditVocabularyActivity, "Đã xóa!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        btnCancel.setOnClickListener { finish() }
    }
}