package com.example.minlish

// ════════════════════════════════════════════════════════════════
//  MainActivity.kt — MinLish
//  Thêm mới: nút "Thêm Deck" với dialog tạo deck inline
//  Toolbar chính không có nút back (màn hình gốc)
// ════════════════════════════════════════════════════════════════

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.minlish.repository.VocabularyRepository
import com.example.minlish.viewmodel.VocabularyViewModel
import com.example.minlish.viewmodel.VocabularyViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private enum class Filter { ALL, DUE, LEARNED, NEW }
    private var currentFilter = Filter.ALL
    private var searchQuery   = ""
    private val fullList      = mutableListOf<Vocabulary>()
    private lateinit var adapter: WordAdapter

    // ── CSV file picker ───────────────────────────────────────────
    private val csvPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val db  = AppDatabase.getInstance(applicationContext)
                val dao = db.vocabularyDao()
                val content = contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: run {
                        runOnUiThread { Toast.makeText(this@MainActivity, "Không đọc được file", Toast.LENGTH_SHORT).show() }
                        return@launch
                    }
                val lines = content.lines().drop(1).filter { it.isNotBlank() }
                var imported = 0
                for (line in lines) {
                    val parts = line.split(",").map { it.trim() }
                    if (parts.size < 2) continue
                    val word    = parts.getOrElse(0) { "" }
                    val meaning = if (parts.size == 2) parts[1] else parts.getOrElse(2) { "" }
                    if (word.isEmpty() || meaning.isEmpty()) continue
                    val entity = VocabularyEntity(
                        word           = word,
                        pronunciation  = if (parts.size > 2) parts.getOrElse(1) { "" } else "",
                        meaning        = meaning,
                        example        = parts.getOrElse(3) { "" },
                        collocation    = parts.getOrElse(4) { "" },
                        relatedWords   = parts.getOrElse(5) { "" },
                        note           = parts.getOrElse(6) { "" },
                        nextReviewTime = System.currentTimeMillis()
                    )
                    val newId = dao.insertVocabulary(entity)
                    FirestoreManager.syncVocabulary(entity.copy(id = newId.toInt()))
                    imported++
                }
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        if (imported > 0) "✅ Đã import $imported từ!" else "⚠️ Không tìm thấy từ nào",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Lỗi import: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        setContentView(R.layout.activity_main)

        // ── Toolbar: màn hình chính — KHÔNG có nút back ───────────
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)   // ← ẩn nút back
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "MinLish"

        // ── Bind UI ───────────────────────────────────────────────
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val tvEmpty      = findViewById<TextView>(R.id.tvEmpty)
        val tvDueCount   = findViewById<TextView>(R.id.tvDueCount)
        val tvTotalWords = findViewById<TextView>(R.id.tvTotalWords)
        val tvTotalDecks = findViewById<TextView>(R.id.tvTotalDecks)
        val tvStreak     = findViewById<TextView>(R.id.tvStreak)
        val btnAdd       = findViewById<MaterialButton>(R.id.btnAdd)
        val btnFlashcard = findViewById<MaterialButton>(R.id.btnFlashcard)
        val bottomNav    = findViewById<BottomNavigationView>(R.id.bottomNav)
        val btnImportCsv = findViewById<MaterialButton?>(R.id.btnImportCsv)
        val btnQuiz      = findViewById<MaterialButton?>(R.id.btnQuiz)
        val btnAddDeck   = findViewById<MaterialButton?>(R.id.btnAddDeck)   // ← MỚI

        val etSearch    = findViewById<TextInputEditText?>(R.id.etSearch)
        val chipAll     = findViewById<Chip?>(R.id.chipAll)
        val chipDue     = findViewById<Chip?>(R.id.chipDue)
        val chipLearned = findViewById<Chip?>(R.id.chipLearned)
        val chipNew     = findViewById<Chip?>(R.id.chipNew)

        // ── DB + ViewModel ────────────────────────────────────────
        val db         = AppDatabase.getInstance(applicationContext)
        val repository = VocabularyRepository(db.vocabularyDao())
        val factory    = VocabularyViewModelFactory(repository)
        val viewModel  = ViewModelProvider(this, factory)[VocabularyViewModel::class.java]

        // ── RecyclerView ──────────────────────────────────────────
        recyclerView.layoutManager = LinearLayoutManager(this)
        val vocabularyList = mutableListOf<Vocabulary>()
        adapter = WordAdapter(vocabularyList)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener { item ->
            startActivity(Intent(this, EditVocabularyActivity::class.java)
                .putExtra("vocabularyId", item.id))
        }

        // ── Observe từ vựng ───────────────────────────────────────
        viewModel.allVocabulary.observe(this) { data ->
            fullList.clear()
            fullList.addAll(data.map { Vocabulary(it.id, it.word, it.meaning) })
            tvTotalWords.text = fullList.size.toString()
            applyFilterAndSearch(vocabularyList, tvEmpty)
        }

        // ── Search listener ───────────────────────────────────────
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilterAndSearch(vocabularyList, tvEmpty)
            }
        })

        // ── Filter chips ──────────────────────────────────────────
        chipAll?.setOnClickListener     { currentFilter = Filter.ALL;     applyFilterAndSearch(vocabularyList, tvEmpty) }
        chipDue?.setOnClickListener     { currentFilter = Filter.DUE;     applyFilterAndSearch(vocabularyList, tvEmpty) }
        chipLearned?.setOnClickListener { currentFilter = Filter.LEARNED; applyFilterAndSearch(vocabularyList, tvEmpty) }
        chipNew?.setOnClickListener     { currentFilter = Filter.NEW;     applyFilterAndSearch(vocabularyList, tvEmpty) }

        // ── Stats ─────────────────────────────────────────────────
        lifecycleScope.launch {
            val now       = System.currentTimeMillis()
            val dueCount  = db.studySessionDao().getDueCount(now)
            val deckCount = db.deckDao().getAllDecksDirect().size
            val allTimes  = db.studySessionDao().getAllReviewTimes()
            val streak    = calculateStreak(allTimes)
            runOnUiThread {
                tvDueCount.text   = "$dueCount từ cần ôn"
                tvTotalDecks.text = deckCount.toString()
                tvStreak.text     = "${streak}🔥"
            }
        }

        // ── Buttons ───────────────────────────────────────────────
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddVocabularyActivity::class.java))
        }

        btnFlashcard.setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java).putExtra("plan", true))
        }

        btnImportCsv?.setOnClickListener {
            startActivity(Intent(this, CsvImportActivity::class.java))
        }

        btnQuiz?.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        // ── NÚT THÊM DECK (MỚI) ──────────────────────────────────
        btnAddDeck?.setOnClickListener {
            showCreateDeckDialog(db, tvTotalDecks)
        }

        // ── Bottom Navigation ─────────────────────────────────────
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home      -> true
                R.id.nav_deck      -> { startActivity(Intent(this, DeckActivity::class.java)); true }  // ← thêm dòng này
                R.id.nav_dashboard -> { startActivity(Intent(this, DashboardActivity::class.java)); true }
                R.id.nav_profile   -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }

        NotificationHelper.createChannel(this)
        SmartReminderScheduler.schedule(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Dialog tạo Deck mới — gọn nhẹ, không cần mở Activity riêng
    // ════════════════════════════════════════════════════════════
    private fun showCreateDeckDialog(db: AppDatabase, tvTotalDecks: TextView) {
        // Inflate view tự tạo (2 EditText: tên + mô tả)
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)

        // Dùng AlertDialog với EditText inline
        val etName = EditText(this).apply {
            hint = "Tên deck *"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val etDesc = EditText(this).apply {
            hint = "Mô tả (tùy chọn)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val px16 = (16 * resources.displayMetrics.density).toInt()
            setPadding(px16 * 2, px16, px16 * 2, 0)
            addView(etName)
            addView(etDesc)
        }

        AlertDialog.Builder(this)
            .setTitle("📚 Tạo Deck mới")
            .setView(container)
            .setPositiveButton("Tạo") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên deck", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val newDeck = DeckEntity(name = name, description = desc)
                    db.deckDao().insertDeck(newDeck)
                    FirestoreManager.syncDeck(newDeck)

                    // Cập nhật lại tvTotalDecks
                    val deckCount = db.deckDao().getAllDecksDirect().size
                    runOnUiThread {
                        tvTotalDecks.text = deckCount.toString()
                        Toast.makeText(this@MainActivity, "✅ Đã tạo deck: $name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ── Filter + Search ───────────────────────────────────────────
    private fun applyFilterAndSearch(displayList: MutableList<Vocabulary>, tvEmpty: TextView) {
        val now = System.currentTimeMillis()
        lifecycleScope.launch {
            val db      = AppDatabase.getInstance(applicationContext)
            val allFull = db.vocabularyDao().getAllVocabularyDirect()
            val reviewed = db.studySessionDao().getAllVocabularyIdsReviewed().toSet()
            val filtered = allFull.filter { entity ->
                val passFilter = when (currentFilter) {
                    Filter.ALL     -> true
                    Filter.DUE     -> entity.nextReviewTime <= now
                    Filter.LEARNED -> entity.id in reviewed
                    Filter.NEW     -> entity.id !in reviewed
                }
                val passSearch = if (searchQuery.isEmpty()) true
                else entity.word.contains(searchQuery, ignoreCase = true)
                        || entity.meaning.contains(searchQuery, ignoreCase = true)
                passFilter && passSearch
            }
            runOnUiThread {
                displayList.clear()
                displayList.addAll(filtered.map { Vocabulary(it.id, it.word, it.meaning) })
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (displayList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // ── Streak ────────────────────────────────────────────────────
    private fun calculateStreak(reviewTimes: List<Long>): Int {
        if (reviewTimes.isEmpty()) return 0
        val reviewDays = reviewTimes.map { getStartOfDay(it) }.toSortedSet().toList().reversed()
        val todayStart = getStartOfDay(System.currentTimeMillis())
        if (reviewDays.first() < todayStart) return 0
        var streak = 1
        val oneDayMs = 24 * 60 * 60 * 1000L
        for (i in 1 until reviewDays.size) {
            if (reviewDays[i - 1] - reviewDays[i] == oneDayMs) streak++ else break
        }
        return streak
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}