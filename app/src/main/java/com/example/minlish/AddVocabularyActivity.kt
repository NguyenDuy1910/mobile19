package com.example.minlish

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AddVocabularyActivity : AppCompatActivity() {

    private var deckEntityList   = listOf<DeckEntity>()
    private var selectedDeckId   = 0
    private var autocompleteJob: Job? = null
    private var aiJob:           Job? = null
    private var popupWindow:     PopupWindow? = null
    private val GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions"
    private val GROQ_MODEL = "llama-3.3-70b-versatile"

    private lateinit var etWord:          TextInputEditText
    private lateinit var etPronunciation: TextInputEditText
    private lateinit var etMeaning:       TextInputEditText
    private lateinit var etDescriptionEn: TextInputEditText
    private lateinit var etExample:       TextInputEditText
    private lateinit var etCollocation:   TextInputEditText
    private lateinit var etRelatedWords:  TextInputEditText
    private lateinit var etNote:          TextInputEditText
    private lateinit var layoutAIStatus:  LinearLayout
    private lateinit var progressAI:      ProgressBar
    private lateinit var tvAIStatus:      TextView

    // Flag: ngăn TextWatcher kích hoạt lại khi code tự set text
    private var isSettingWord = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vocabulary)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val spinnerDeck = findViewById<Spinner>(R.id.spinnerDeck)
        etWord          = findViewById(R.id.etWord)
        etPronunciation = findViewById(R.id.etPronunciation)
        etMeaning       = findViewById(R.id.etMeaning)
        etDescriptionEn = findViewById(R.id.etDescriptionEn)
        etExample       = findViewById(R.id.etExample)
        etCollocation   = findViewById(R.id.etCollocation)
        etRelatedWords  = findViewById(R.id.etRelatedWords)
        etNote          = findViewById(R.id.etNote)
        val btnSave     = findViewById<Button>(R.id.btnSave)
        val btnCancel   = findViewById<Button>(R.id.btnCancel)
        layoutAIStatus  = findViewById(R.id.layoutAIStatus)
        progressAI      = findViewById(R.id.progressAI)
        tvAIStatus      = findViewById(R.id.tvAIStatus)

        val db  = AppDatabase.getInstance(applicationContext)
        val dao = db.vocabularyDao()

        // ── SPINNER DECK ──────────────────────────────────
        db.deckDao().getAllDecks().observe(this) { decks ->
            deckEntityList = decks
            val names = mutableListOf("Chưa chọn deck")
            names.addAll(decks.map { it.name })
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDeck.adapter = adapter

            val preId = intent.getIntExtra("deckId", 0)
            if (preId != 0) {
                val idx = decks.indexOfFirst { it.id == preId }
                if (idx != -1) spinnerDeck.setSelection(idx + 1)
            }
        }

        spinnerDeck.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                selectedDeckId = if (pos == 0) 0 else deckEntityList[pos - 1].id
            }
            override fun onNothingSelected(p: AdapterView<*>) { selectedDeckId = 0 }
        }

        // ── TEXT WATCHER ──────────────────────────────────
        etWord.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Bỏ qua khi code tự điền từ (tránh vòng lặp)
                if (isSettingWord) return

                val input = s?.toString()?.trim() ?: return

                if (input.length < 2) {
                    dismissPopup()
                    aiJob?.cancel()
                    hideAIStatus()
                    return
                }

                autocompleteJob?.cancel()
                autocompleteJob = lifecycleScope.launch {
                    delay(200)

                    val suggestions = WordBank.suggest(input)

                    if (suggestions.isNotEmpty()) {
                        showPopup(suggestions)
                        aiJob?.cancel()
                        hideAIStatus()
                    } else {
                        dismissPopup()
                        aiJob?.cancel()
                        aiJob = launch {
                            delay(600)
                            val current = etWord.text?.toString()?.trim() ?: ""
                            if (current.equals(input, ignoreCase = true) && current.isNotEmpty()) {
                                fillWordInfoGroq(current)
                            }
                        }
                    }
                }
            }
        })

        etWord.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) dismissPopup()
        }

        // ── LƯU TỪ ───────────────────────────────────────
        btnSave.setOnClickListener {
            val word    = etWord.text.toString().trim()
            val meaning = etMeaning.text.toString().trim()
            if (word.isEmpty())    { etWord.error = "Vui lòng nhập từ vựng"; return@setOnClickListener }
            if (meaning.isEmpty()) { etMeaning.error = "Vui lòng nhập nghĩa"; return@setOnClickListener }

            val vocab = VocabularyEntity(
                word           = word,
                pronunciation  = etPronunciation.text.toString().trim(),
                meaning        = meaning,
                descriptionEn  = etDescriptionEn.text.toString().trim(),
                example        = etExample.text.toString().trim(),
                collocation    = etCollocation.text.toString().trim(),
                relatedWords   = etRelatedWords.text.toString().trim(),
                note           = etNote.text.toString().trim(),
                deckId         = selectedDeckId,
                nextReviewTime = System.currentTimeMillis()
            )
            lifecycleScope.launch {
                val existing = dao.getVocabularyByWord(word)
                if (existing != null) {
                    Toast.makeText(this@AddVocabularyActivity, "\"$word\" đã tồn tại trong danh sách!", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val newId = dao.insertVocabulary(vocab)
                FirestoreManager.syncVocabulary(vocab.copy(id = newId.toInt()))
                Toast.makeText(this@AddVocabularyActivity, "Đã lưu: $word", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnCancel.setOnClickListener { dismissPopup(); finish() }
    }

    // ═══════════════════════════════════════════════════════
    // POPUP GỢI Ý
    // Fix: dùng setOnTouchListener thay vì setOnItemClickListener
    // để bắt touch TRƯỚC KHI popup bị dismiss
    // ═══════════════════════════════════════════════════════
    private fun showPopup(suggestions: List<String>) {
        dismissPopup()
        if (suggestions.isEmpty()) return

        val listView = ListView(this).apply {
            adapter = ArrayAdapter(
                this@AddVocabularyActivity,
                android.R.layout.simple_list_item_1,
                suggestions
            )
            setBackgroundColor(Color.WHITE)
            setPadding(8, 4, 8, 4)
            divider       = ColorDrawable(Color.LTGRAY)
            dividerHeight = 1
        }

        val popupWidth   = etWord.width.takeIf { it > 0 } ?: 800
        val itemHeightPx = (48 * resources.displayMetrics.density).toInt()
        val popupHeight  = minOf(suggestions.size, 6) * itemHeightPx

        popupWindow = PopupWindow(listView, popupWidth, popupHeight, false).apply {
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            elevation          = 24f
            isOutsideTouchable = true
            isTouchable        = true
            showAsDropDown(etWord, 0, 4)
        }

        // ── FIX CHÍNH: dùng onTouchListener để bắt item trước khi popup dismiss ──
        listView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val index = listView.pointToPosition(event.x.toInt(), event.y.toInt())
                if (index != ListView.INVALID_POSITION) {
                    val selected = suggestions[index]

                    // Điền từ vào field, block TextWatcher
                    isSettingWord = true
                    etWord.setText(selected)
                    etWord.setSelection(selected.length)
                    isSettingWord = false

                    dismissPopup()

                    // Gọi AI điền thông tin
                    aiJob?.cancel()
                    aiJob = lifecycleScope.launch {
                        fillWordInfoGroq(selected)
                    }
                }
            }
            true
        }
    }

    private fun dismissPopup() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun hideAIStatus() {
        layoutAIStatus.visibility = View.GONE
        progressAI.visibility     = View.GONE
        tvAIStatus.text           = ""
    }

    // ═══════════════════════════════════════════════════════
    // AUTO-FILL bằng GROQ
    // ═══════════════════════════════════════════════════════
    private suspend fun fillWordInfoGroq(word: String) {
        withContext(Dispatchers.Main) {
            layoutAIStatus.visibility = View.VISIBLE
            progressAI.visibility     = View.VISIBLE
            tvAIStatus.text           = "⏳ AI đang tra từ \"$word\"..."
        }

        try {
            val prompt = """
                Look up the English word: "$word"
                Reply with ONLY this exact JSON format, no extra text, no markdown backticks:
                {
                  "pronunciation": "/IPA here/",
                  "meaning": "nghĩa tiếng Việt",
                  "descriptionEn": "simple English definition",
                  "example": "One example sentence (Vietnamese translation in parentheses)",
                  "collocation": "3-5 collocations separated by commas",
                  "relatedWords": "5 related words separated by commas",
                  "note": "short usage note or common mistake"
                }
            """.trimIndent()

            val body = JSONObject().apply {
                put("model", GROQ_MODEL)
                put("max_tokens", 600)
                put("temperature", 0.2)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val responseText = withContext(Dispatchers.IO) {
                val url  = URL(GROQ_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $GROQ_KEY")
                    doOutput       = true
                    connectTimeout = 10_000
                    readTimeout    = 15_000
                }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    android.util.Log.e("AI_DEBUG", "Groq error $code: $err")
                    conn.disconnect()
                    throw Exception("Groq lỗi $code")
                }
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                text
            }

            val raw = JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            android.util.Log.d("AI_DEBUG", "Groq raw: $raw")

            val start = raw.indexOf("{")
            val end   = raw.lastIndexOf("}") + 1
            if (start < 0 || end <= start) throw Exception("Không parse được JSON")

            val data = JSONObject(raw.substring(start, end))

            withContext(Dispatchers.Main) {
                etPronunciation.setText(data.optString("pronunciation"))
                etMeaning.setText(data.optString("meaning"))
                etDescriptionEn.setText(data.optString("descriptionEn"))
                etExample.setText(data.optString("example"))
                etCollocation.setText(data.optString("collocation"))
                etRelatedWords.setText(data.optString("relatedWords"))
                etNote.setText(data.optString("note"))

                progressAI.visibility = View.GONE
                tvAIStatus.text       = "✅ Đã điền xong! Kiểm tra và chỉnh sửa nếu cần."
            }
        } catch (e: CancellationException) {
            withContext(Dispatchers.Main) { hideAIStatus() }
        } catch (e: Exception) {
            android.util.Log.e("AI_DEBUG", "Groq exception: ${e.message}", e)
            withContext(Dispatchers.Main) {
                progressAI.visibility = View.GONE
                tvAIStatus.text       = "❌ Lỗi: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissPopup()
    }
}
