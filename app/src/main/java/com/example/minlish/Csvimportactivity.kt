package com.example.minlish

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class CsvImportActivity : AppCompatActivity() {

    private var selectedFileUri: Uri? = null
    private var parsedWords    = mutableListOf<VocabularyEntity>()
    private var deckEntityList = listOf<DeckEntity>()
    private var selectedDeckId = 0

    private val PICK_CSV_REQUEST = 3001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_csv_import)

        // ── Toolbar nút back ← không hiện tên ────────────────────
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val spinnerDeck = findViewById<Spinner>(R.id.spinnerDeck)
        val btnPickFile = findViewById<Button>(R.id.btnPickFile)
        val tvFileName  = findViewById<TextView>(R.id.tvFileName)
        val tvPreview   = findViewById<TextView>(R.id.tvPreview)
        val tvResult    = findViewById<TextView>(R.id.tvResult)
        val btnImport   = findViewById<Button>(R.id.btnImport)

        val db  = AppDatabase.getInstance(applicationContext)
        val dao = db.vocabularyDao()

        // ── Spinner Deck ──────────────────────────────────────────
        db.deckDao().getAllDecks().observe(this) { decks ->
            deckEntityList = decks
            val deckNames = mutableListOf("Chưa chọn deck")
            deckNames.addAll(decks.map { it.name })
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deckNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDeck.adapter = adapter
        }

        spinnerDeck.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedDeckId = if (position == 0) 0 else deckEntityList[position - 1].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) { selectedDeckId = 0 }
        }

        // ── Chọn file ─────────────────────────────────────────────
        btnPickFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
            startActivityForResult(intent, PICK_CSV_REQUEST)
        }

        // ── Import ────────────────────────────────────────────────
        btnImport.setOnClickListener {
            if (parsedWords.isEmpty()) {
                tvResult.text = "Không có từ nào để import"
                return@setOnClickListener
            }
            btnImport.isEnabled = false
            tvResult.text = "⏳ Đang import..."

            lifecycleScope.launch {
                var successCount = 0
                val failedWords  = mutableListOf<String>()
                val wordsToInsert = parsedWords.map { it.copy(deckId = selectedDeckId) }

                for (word in wordsToInsert) {
                    try {
                        val newId = dao.insertVocabulary(word)
                        val savedWord = word.copy(id = newId.toInt())
                        FirestoreManager.syncVocabulary(savedWord)
                        successCount++
                    } catch (e: Exception) {
                        failedWords.add(word.word)
                    }
                }

                val resultMsg = buildString {
                    append("✅ Đã import $successCount/${parsedWords.size} từ thành công!")
                    if (failedWords.isNotEmpty()) {
                        append("\n⚠️ Lỗi ${failedWords.size} từ: ${failedWords.take(3).joinToString(", ")}")
                        if (failedWords.size > 3) append("...")
                    }
                    if (AuthManager.currentUser == null) {
                        append("\n(Chưa đăng nhập — dữ liệu chỉ lưu local)")
                    }
                }

                runOnUiThread {
                    tvResult.text = resultMsg
                    parsedWords.clear()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CSV_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            selectedFileUri = uri

            val tvFileName = findViewById<TextView>(R.id.tvFileName)
            val tvPreview  = findViewById<TextView>(R.id.tvPreview)
            val btnImport  = findViewById<Button>(R.id.btnImport)

            tvFileName.text = uri.lastPathSegment ?: "file đã chọn"

            try {
                val content = contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("Không mở được file")

                parsedWords = parseCsv(content)

                val preview = parsedWords.take(5).joinToString("\n\n") { word ->
                    "• ${word.word} — ${word.meaning}" +
                            (if (word.example.isNotEmpty()) "\n  💬 ${word.example}" else "")
                }

                tvPreview.text = if (parsedWords.isEmpty()) {
                    "Không đọc được từ nào. Kiểm tra lại format CSV."
                } else {
                    "Tìm thấy ${parsedWords.size} từ:\n\n$preview" +
                            if (parsedWords.size > 5) "\n\n...và ${parsedWords.size - 5} từ nữa" else ""
                }

                btnImport.isEnabled = parsedWords.isNotEmpty()

            } catch (e: Exception) {
                tvPreview.text = "Lỗi đọc file: ${e.message}"
                btnImport.isEnabled = false
            }
        }
    }

    private fun parseCsv(content: String): MutableList<VocabularyEntity> {
        val result = mutableListOf<VocabularyEntity>()
        val lines  = content.lines().drop(1).filter { it.isNotBlank() }

        for (line in lines) {
            val parts = line.split(",").map { it.trim() }
            if (parts.size < 2) continue
            val word    = parts.getOrElse(0) { "" }
            val meaning = if (parts.size == 2) parts[1] else parts.getOrElse(2) { "" }
            if (word.isEmpty() || meaning.isEmpty()) continue

            result.add(VocabularyEntity(
                word           = word,
                pronunciation  = if (parts.size > 2) parts.getOrElse(1) { "" } else "",
                meaning        = meaning,
                example        = parts.getOrElse(3) { "" },
                collocation    = parts.getOrElse(4) { "" },
                relatedWords   = parts.getOrElse(5) { "" },
                note           = parts.getOrElse(6) { "" },
                nextReviewTime = System.currentTimeMillis()
            ))
        }
        return result
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}