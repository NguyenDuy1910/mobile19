package com.example.minlish

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.minlish.util.SrsAlgorithm
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.util.Locale

class QuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val quizList   = mutableListOf<VocabularyEntity>()
    private var currentIdx = 0
    private var score      = 0
    private var answered   = false

    private lateinit var dao:        VocabularyDao
    private lateinit var sessionDao: StudySessionDao

    private var tts:          TextToSpeech? = null
    private var ttsReady      = false
    private var pendingSpeak: String? = null

    private lateinit var tvProgress:   TextView
    private lateinit var tvScore:      TextView
    private lateinit var tvWord:       TextView
    private lateinit var tvHint:       TextView
    private lateinit var btnA:         Button
    private lateinit var btnB:         Button
    private lateinit var btnC:         Button
    private lateinit var btnD:         Button
    private lateinit var tvFeedback:   TextView
    private lateinit var btnNext:      Button
    private lateinit var layoutFinish: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnRetry:     Button
    private var correctIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // ── Toolbar nút back ← không hiện tên ────────────────────
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        tvProgress   = findViewById(R.id.tvProgress)
        tvScore      = findViewById(R.id.tvScore)
        tvWord       = findViewById(R.id.tvWord)
        tvHint       = findViewById(R.id.tvHint)
        btnA         = findViewById(R.id.btnA)
        btnB         = findViewById(R.id.btnB)
        btnC         = findViewById(R.id.btnC)
        btnD         = findViewById(R.id.btnD)
        tvFeedback   = findViewById(R.id.tvFeedback)
        btnNext      = findViewById(R.id.btnNext)
        layoutFinish = findViewById(R.id.layoutFinish)
        tvFinalScore = findViewById(R.id.tvFinalScore)
        btnRetry     = findViewById(R.id.btnRetry)

        tts = TextToSpeech(this, this)

        val db     = AppDatabase.getInstance(applicationContext)
        dao        = db.vocabularyDao()
        sessionDao = db.studySessionDao()

        val deckId = intent.getIntExtra("deckId", -1)

        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val all = if (deckId == -1) dao.getAllVocabularyDirect()
            else dao.getVocabularyByDeckDirect(deckId)

            if (all.size < 4) {
                runOnUiThread {
                    tvWord.text     = "Cần ít nhất 4 từ để chơi Quiz!\nHãy thêm thêm từ vựng."
                    tvWord.textSize = 18f
                }
                return@launch
            }

            val due = dao.getDueVocabulary(now).toMutableList()
            if (due.size < 10) {
                val extra = all.filter { it !in due }.shuffled().take(10 - due.size)
                due.addAll(extra)
            }

            quizList.addAll(due.shuffled().take(10))
            runOnUiThread { showQuestion() }
        }

        btnNext.setOnClickListener { nextQuestion() }
        btnRetry.setOnClickListener {
            layoutFinish.visibility = View.GONE
            currentIdx = 0
            score      = 0
            quizList.shuffle()
            showQuestion()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (ttsReady) {
                pendingSpeak?.let { word ->
                    tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "auto")
                    pendingSpeak = null
                }
            }
        }
    }

    private fun speakWord(word: String) {
        if (word.isBlank()) return
        if (ttsReady) tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "quiz_speak")
        else pendingSpeak = word
    }

    private fun showQuestion() {
        if (currentIdx >= quizList.size) { showFinish(); return }

        answered = false
        val card = quizList[currentIdx]

        tvProgress.text   = "${currentIdx + 1} / ${quizList.size}"
        tvScore.text      = "✅ $score"
        tvWord.text       = card.word
        tvHint.text       = card.pronunciation
        tvHint.visibility = if (card.pronunciation.isNotEmpty()) View.VISIBLE else View.GONE

        tvFeedback.visibility = View.GONE
        btnNext.visibility    = View.GONE

        val wrongOptions = quizList
            .filter { it.id != card.id && it.meaning.isNotEmpty() }
            .shuffled().take(3).map { it.meaning }

        val options = (wrongOptions + card.meaning).shuffled()
        correctIndex = options.indexOf(card.meaning)

        val buttons = listOf(btnA, btnB, btnC, btnD)
        val labels  = listOf("A", "B", "C", "D")
        buttons.forEachIndexed { i, btn ->
            btn.text = "${labels[i]}. ${options.getOrElse(i) { "" }}"
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.surface))
            btn.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
            btn.isEnabled  = true
            btn.visibility = View.VISIBLE
            btn.setOnClickListener { onAnswerSelected(i, correctIndex, buttons) }
        }

        speakWord(card.word)
    }

    private fun onAnswerSelected(chosen: Int, correct: Int, buttons: List<Button>) {
        if (answered) return
        answered = true

        val card    = quizList[currentIdx]
        val isRight = chosen == correct

        buttons.forEachIndexed { i, btn ->
            btn.isEnabled = false
            when {
                i == correct            -> btn.setBackgroundColor(ContextCompat.getColor(this, R.color.srs_good))
                i == chosen && !isRight -> btn.setBackgroundColor(ContextCompat.getColor(this, R.color.srs_again))
            }
            btn.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        if (isRight) {
            tvFeedback.text = "✅ Đúng rồi!"
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.srs_good))
        } else {
            tvFeedback.text = "❌ Sai! Đáp án: ${card.meaning}"
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.srs_again))
        }
        tvFeedback.visibility = View.VISIBLE
        btnNext.visibility    = View.VISIBLE

        val rating = if (isRight) 2 else 0
        lifecycleScope.launch {
            val updated = SrsAlgorithm.calculateNextReview(card, rating)
            dao.updateVocabulary(updated)
            sessionDao.insertSession(
                StudySessionEntity(vocabularyId = card.id, result = if (isRight) "good" else "again")
            )
        }
    }

    private fun nextQuestion() { currentIdx++; showQuestion() }

    private fun showFinish() {
        tvWord.text           = ""
        tvHint.visibility     = View.GONE
        tvFeedback.visibility = View.GONE
        btnNext.visibility    = View.GONE
        listOf(btnA, btnB, btnC, btnD).forEach { it.visibility = View.GONE }

        val pct = score * 100 / quizList.size
        val msg = when {
            pct >= 90 -> "Xuất sắc! 🏆"
            pct >= 70 -> "Tốt lắm! 🎉"
            pct >= 50 -> "Cần cố gắng hơn 💪"
            else      -> "Ôn lại nhiều hơn nhé 📖"
        }

        tvFinalScore.text       = "$msg\n\nKết quả: $score / ${quizList.size} câu đúng ($pct%)"
        layoutFinish.visibility = View.VISIBLE
        tvProgress.text         = "Hoàn thành!"
        tvScore.text            = "✅ $score / ${quizList.size}"
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}