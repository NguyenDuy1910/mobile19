package com.example.minlish

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.minlish.util.SrsAlgorithm
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

class FlashcardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val flashcardList = mutableListOf<VocabularyEntity>()
    private var isShowingBack = false
    private var correctCount  = 0
    private var totalCount    = 0

    // History để nút ← biết quay lại từ nào
    private val history    = mutableListOf<Int>()
    private var historyPos = -1
    private val currentIndex get() = history.getOrElse(historyPos) { 0 }

    private lateinit var dao:        VocabularyDao
    private lateinit var sessionDao: StudySessionDao

    private var tts:      TextToSpeech? = null
    private var ttsReady = false

    private lateinit var toolbar:          MaterialToolbar
    private lateinit var tvProgress:       TextView
    private lateinit var tvFinished:       TextView
    private lateinit var layoutFront:      LinearLayout
    private lateinit var layoutBack:       ScrollView
    private lateinit var layoutBackInner:  LinearLayout
    private lateinit var layoutSrsButtons: LinearLayout
    private lateinit var tvWord:           TextView
    private lateinit var tvPronunciation:  TextView
    private lateinit var tvMeaning:        TextView
    private lateinit var tvExample:        TextView
    private lateinit var tvCollocation:    TextView
    private lateinit var tvRelatedWords:   TextView
    private lateinit var tvNote:           TextView
    private lateinit var btnShowAnswer:    Button
    private lateinit var btnAgain:         Button
    private lateinit var btnHard:          Button
    private lateinit var btnGood:          Button
    private lateinit var btnEasy:          Button
    private lateinit var btnSpeak:         ImageButton
    private lateinit var btnExit:          MaterialButton

    private val FLIP_HALF = 200L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard)

        toolbar          = findViewById(R.id.toolbar)
        tvProgress       = findViewById(R.id.tvProgress)
        tvFinished       = findViewById(R.id.tvFinished)
        layoutFront      = findViewById(R.id.layoutFront)
        layoutBack       = findViewById(R.id.layoutBack)
        layoutBackInner  = findViewById(R.id.layoutBackInner)
        layoutSrsButtons = findViewById(R.id.layoutSrsButtons)
        tvWord           = findViewById(R.id.tvWord)
        tvPronunciation  = findViewById(R.id.tvPronunciation)
        tvMeaning        = findViewById(R.id.tvMeaning)
        tvExample        = findViewById(R.id.tvExample)
        tvCollocation    = findViewById(R.id.tvCollocation)
        tvRelatedWords   = findViewById(R.id.tvRelatedWords)
        tvNote           = findViewById(R.id.tvNote)
        btnShowAnswer    = findViewById(R.id.btnShowAnswer)
        btnAgain         = findViewById(R.id.btnAgain)
        btnHard          = findViewById(R.id.btnHard)
        btnGood          = findViewById(R.id.btnGood)
        btnEasy          = findViewById(R.id.btnEasy)
        btnSpeak         = findViewById(R.id.btnSpeak)
        btnExit          = findViewById(R.id.btnExit)

        // Toolbar: mũi tên ← xử lý back/prev
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Camera distance để flip 3D trông đẹp
        val camDist = 8000 * resources.displayMetrics.density
        layoutFront.cameraDistance     = camDist
        layoutBackInner.cameraDistance = camDist

        // State ban đầu
        resetUI()
        tvProgress.text = "Đang tải..."

        tts = TextToSpeech(this, this)

        val db = AppDatabase.getInstance(applicationContext)
        dao        = db.vocabularyDao()
        sessionDao = db.studySessionDao()

        val deckId = intent.getIntExtra("deckId", -1)
        val isPlan = intent.getBooleanExtra("plan", false)

        lifecycleScope.launch {
            val cards = if (isPlan) {
                LearningPlanHelper.getDailyPlan(applicationContext, dao, sessionDao)
            } else {
                val now = System.currentTimeMillis()
                if (deckId == -1) dao.getDueVocabulary(now)
                else dao.getDueVocabularyByDeck(deckId, now)
            }
            flashcardList.addAll(cards)
            totalCount = flashcardList.size
            runOnUiThread {
                if (flashcardList.isEmpty()) showFinished()
                else navigateTo(0)
            }
        }

        // ── Listeners ────────────────────────────────────────────

        // Loa — toolbar góc phải
        btnSpeak.setOnClickListener {
            flashcardList.getOrNull(currentIndex)?.word?.let { speakWord(it) }
        }

        // Thoát về MainActivity
        btnExit.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        btnShowAnswer.setOnClickListener { if (!isShowingBack) flipToBack() }
        layoutFront.setOnClickListener   { if (!isShowingBack) flipToBack() }

        btnAgain.setOnClickListener { handleRating(0) }
        btnHard.setOnClickListener  { handleRating(1) }
        btnGood.setOnClickListener  { handleRating(2) }
        btnEasy.setOnClickListener  { handleRating(3) }
    }

    // ── Mũi tên ← trên toolbar ───────────────────────────────────
    // Nếu còn từ phía trước → quay lại từ đó
    // Nếu đang ở từ đầu tiên → finish() (về màn trước)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (historyPos > 0) {
            historyPos--
            showCard()
        } else {
            super.onBackPressed()
        }
    }

    // ── TTS ───────────────────────────────────────────────────────
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
        btnSpeak.isEnabled = ttsReady
    }

    private fun speakWord(word: String) {
        if (ttsReady && word.isNotBlank())
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "tts_word")
    }

    // ── Navigation ────────────────────────────────────────────────
    private fun navigateTo(index: Int) {
        if (index >= flashcardList.size) { showFinished(); return }
        while (history.size > historyPos + 1) history.removeAt(history.lastIndex)
        history.add(index)
        historyPos = history.lastIndex
        showCard()
    }

    private fun showCard() {
        isShowingBack = false
        val index = currentIndex
        if (index >= flashcardList.size) { showFinished(); return }

        val card = flashcardList[index]
        tvProgress.text      = "${index + 1} / $totalCount"
        tvWord.text          = card.word
        tvPronunciation.text = card.pronunciation
        tvPronunciation.visibility = if (card.pronunciation.isNotEmpty()) View.VISIBLE else View.GONE

        layoutFront.rotationY     = 0f
        layoutBackInner.rotationY = 0f

        layoutFront.visibility      = View.VISIBLE
        layoutBack.visibility       = View.GONE
        tvFinished.visibility       = View.GONE
        layoutSrsButtons.visibility = View.GONE
        btnShowAnswer.visibility    = View.VISIBLE

        speakWord(card.word)
    }

    // ── Flip 3D ───────────────────────────────────────────────────
    private fun flipToBack() {
        if (isShowingBack) return
        isShowingBack = true
        val card = flashcardList.getOrNull(currentIndex) ?: return

        tvMeaning.text      = card.meaning
        tvExample.text      = if (card.example.isNotEmpty())      "💬 ${card.example}"      else ""
        tvCollocation.text  = if (card.collocation.isNotEmpty())  "🔗 ${card.collocation}"  else ""
        tvRelatedWords.text = if (card.relatedWords.isNotEmpty()) "🔄 ${card.relatedWords}" else ""
        tvNote.text         = if (card.note.isNotEmpty())         "📝 ${card.note}"         else ""

        listOf(
            tvExample to card.example, tvCollocation to card.collocation,
            tvRelatedWords to card.relatedWords, tvNote to card.note
        ).forEach { (tv, txt) -> tv.visibility = if (txt.isNotEmpty()) View.VISIBLE else View.GONE }

        // Giai đoạn 1: mặt trước quay ra 0→90°
        ObjectAnimator.ofFloat(layoutFront, "rotationY", 0f, 90f).apply {
            duration = FLIP_HALF; interpolator = AccelerateInterpolator(); start()
        }

        // Giai đoạn 2: mặt sau quay vào -90→0°
        layoutFront.postDelayed({
            layoutFront.visibility    = View.INVISIBLE
            layoutBackInner.rotationY = -90f
            layoutBack.visibility     = View.VISIBLE
            ObjectAnimator.ofFloat(layoutBackInner, "rotationY", -90f, 0f).apply {
                duration = FLIP_HALF; interpolator = DecelerateInterpolator(); start()
            }
            layoutFront.postDelayed({
                layoutFront.visibility      = View.GONE
                btnShowAnswer.visibility    = View.GONE
                layoutSrsButtons.visibility = View.VISIBLE
            }, FLIP_HALF + 50)
        }, FLIP_HALF)
    }

    // ── SRS ───────────────────────────────────────────────────────
    private fun handleRating(rating: Int) {
        val card = flashcardList.getOrNull(currentIndex) ?: return
        listOf(btnAgain, btnHard, btnGood, btnEasy).forEach { it.isEnabled = false }

        lifecycleScope.launch {
            val updated = SrsAlgorithm.calculateNextReview(card, rating)
            dao.updateVocabulary(updated)
            val label = when (rating) { 0 -> "again"; 1 -> "hard"; 2 -> "good"; else -> "easy" }
            sessionDao.insertSession(StudySessionEntity(vocabularyId = card.id, result = label))
            FirestoreManager.syncVocabulary(updated)
            if (rating == 0) flashcardList.add(updated)
            if (rating >= 2) correctCount++

            runOnUiThread {
                listOf(btnAgain, btnHard, btnGood, btnEasy).forEach { it.isEnabled = true }
                navigateTo(currentIndex + 1)
            }
        }
    }

    private fun showFinished() {
        layoutFront.visibility      = View.GONE
        layoutBack.visibility       = View.GONE
        btnShowAnswer.visibility    = View.GONE
        layoutSrsButtons.visibility = View.GONE
        val accuracy = if (totalCount > 0) correctCount * 100 / totalCount else 0
        tvFinished.text = "Hoàn thành! 🎉\n\nĐộ chính xác: $accuracy%\nTổng: $totalCount từ · Đúng: $correctCount từ"
        tvFinished.visibility = View.VISIBLE
        tvProgress.text = "Hoàn thành! 🎉"
    }

    private fun resetUI() {
        layoutFront.visibility      = View.VISIBLE
        layoutBack.visibility       = View.GONE
        tvFinished.visibility       = View.GONE
        btnShowAnswer.visibility    = View.VISIBLE
        layoutSrsButtons.visibility = View.GONE
        layoutFront.rotationY       = 0f
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }
}