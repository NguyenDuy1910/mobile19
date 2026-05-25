package com.example.minlish

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class DeckDetailActivity : AppCompatActivity() {

    // lateinit để dùng được bên trong lambda onDelete
    private lateinit var adapter: VocabDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_detail)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val deckId   = intent.getIntExtra("deckId", -1)
        val deckName = intent.getStringExtra("deckName") ?: "Chi tiết Deck"
        supportActionBar?.title = deckName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvTotalWords   = findViewById<TextView>(R.id.tvTotalWords)
        val tvDueWords     = findViewById<TextView>(R.id.tvDueWords)
        val tvLearnedWords = findViewById<TextView>(R.id.tvLearnedWords)
        val tvAccuracy     = findViewById<TextView>(R.id.tvAccuracy)
        val recyclerView   = findViewById<RecyclerView>(R.id.recyclerView)
        val tvEmpty        = findViewById<TextView>(R.id.tvEmpty)
        val btnStudy       = findViewById<Button>(R.id.btnStudy)
        val btnQuiz        = findViewById<Button?>(R.id.btnQuiz)

        val db         = AppDatabase.getInstance(applicationContext)
        val vocabDao   = db.vocabularyDao()
        val sessionDao = db.studySessionDao()

        // ── Adapter ───────────────────────────────────────────────
        val wordList = mutableListOf<VocabularyEntity>()
        adapter = VocabDetailAdapter(
            list     = wordList,
            onEdit   = { word: VocabularyEntity ->
                startActivity(
                    Intent(this, EditVocabularyActivity::class.java)
                        .putExtra("vocabularyId", word.id)
                )
            },
            onDelete = { word: VocabularyEntity ->
                lifecycleScope.launch {
                    vocabDao.deleteVocabulary(word)
                    FirestoreManager.deleteVocabulary(word)
                    wordList.remove(word)
                    adapter.notifyDataSetChanged()
                    tvEmpty.visibility = if (wordList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // ── Observe từ trong deck ─────────────────────────────────
        vocabDao.getVocabularyByDeck(deckId).observe(this) { list ->
            wordList.clear()
            wordList.addAll(list)
            adapter.notifyDataSetChanged()
            tvEmpty.visibility = if (wordList.isEmpty()) View.VISIBLE else View.GONE
        }

        // ── Thống kê ──────────────────────────────────────────────
        lifecycleScope.launch {
            val now         = System.currentTimeMillis()
            val total       = vocabDao.getVocabularyByDeckDirect(deckId).size
            val due         = sessionDao.getDueCountByDeck(deckId, now)
            val learned     = sessionDao.getLearnedCountByDeck(deckId)
            val totalReview = sessionDao.getTotalReviewsByDeck(deckId)
            val correct     = sessionDao.getCorrectCountByDeck(deckId)
            val accuracy    = if (totalReview > 0) correct * 100 / totalReview else 0

            runOnUiThread {
                tvTotalWords.text   = "$total từ"
                tvDueWords.text     = "$due cần ôn"
                tvLearnedWords.text = "$learned đã học"
                tvAccuracy.text     = "$accuracy% chính xác"
            }
        }

        // ── Buttons ───────────────────────────────────────────────
        btnStudy.setOnClickListener {
            startActivity(
                Intent(this, FlashcardActivity::class.java)
                    .putExtra("deckId", deckId)
                    .putExtra("deckName", deckName)
            )
        }

        btnQuiz?.setOnClickListener {
            startActivity(
                Intent(this, QuizActivity::class.java)
                    .putExtra("deckId", deckId)
            )
        }
    }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

// ── Adapter (giữ nguyên ở cuối file như cũ) ───────────────────────
class VocabDetailAdapter(
    private val list: MutableList<VocabularyEntity>,
    private val onEdit: (VocabularyEntity) -> Unit,
    private val onDelete: (VocabularyEntity) -> Unit
) : RecyclerView.Adapter<VocabDetailAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvWord:    TextView    = v.findViewById(R.id.tvWord)
        val tvMeaning: TextView    = v.findViewById(R.id.tvMeaning)
        val tvDue:     TextView    = v.findViewById(R.id.tvDueStatus)
        val btnEdit:   ImageButton = v.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vocabulary_detail, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val vocab = list[position]
        holder.tvWord.text    = vocab.word
        holder.tvMeaning.text = vocab.meaning

        val now = System.currentTimeMillis()
        holder.tvDue.text = if (vocab.nextReviewTime <= now) "📅 Cần ôn" else "✅ Đã học"
        holder.tvDue.setTextColor(
            if (vocab.nextReviewTime <= now) 0xFFFF5722.toInt() else 0xFF4CAF50.toInt()
        )

        holder.btnEdit.setOnClickListener   { onEdit(vocab) }
        holder.btnDelete.setOnClickListener { onDelete(vocab) }
    }

    override fun getItemCount() = list.size
}