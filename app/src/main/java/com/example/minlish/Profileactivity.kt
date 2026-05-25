package com.example.minlish

// ════════════════════════════════════════════════════════════════
//  ProfileActivity.kt — MinLish
//  Fix: toolbar có nút back, không hiện tên activity ở đầu
// ════════════════════════════════════════════════════════════════

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid get() = AuthManager.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // ── Toolbar: hiện nút back, KHÔNG hiện tên ──────────────
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)    // ← nút back ←
        supportActionBar?.setDisplayShowTitleEnabled(false)  // ← ẩn tên "Hồ sơ"

        val tvEmail        = findViewById<TextView>(R.id.tvEmail)
        val etName         = findViewById<EditText>(R.id.etName)
        val spinnerGoal    = findViewById<Spinner>(R.id.spinnerGoal)
        val spinnerLevel   = findViewById<Spinner>(R.id.spinnerLevel)
        val tvTotalWords   = findViewById<TextView>(R.id.tvTotalWords)
        val tvTotalDecks   = findViewById<TextView>(R.id.tvTotalDecks)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)
        val btnLogout      = findViewById<Button>(R.id.btnLogout)
        val spinnerNewWords = findViewById<Spinner>(R.id.spinnerNewWords)
        val btnSavePlan     = findViewById<Button>(R.id.btnSavePlan)

        // ── Daily learning plan ───────────────────────────────────
        val newWordsOptions = listOf(5, 10, 15, 20, 25, 30)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, newWordsOptions.map { "$it từ/ngày" })
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNewWords.adapter = spinnerAdapter
        val prefs = getSharedPreferences("learning_plan", MODE_PRIVATE)
        val savedValue = prefs.getInt("new_words_per_day", 10)
        spinnerNewWords.setSelection(newWordsOptions.indexOf(savedValue))
        btnSavePlan.setOnClickListener {
            val selected = newWordsOptions[spinnerNewWords.selectedItemPosition]
            prefs.edit().putInt("new_words_per_day", selected).apply()
            Toast.makeText(this, "Đã lưu: $selected từ mới mỗi ngày", Toast.LENGTH_SHORT).show()
        }

        // ── Email ─────────────────────────────────────────────────
        tvEmail.text = AuthManager.currentUser?.email ?: ""

        // ── Spinner mục tiêu ──────────────────────────────────────
        val goals = listOf("IELTS", "TOEIC", "Giao tiếp", "Du học", "Công việc", "Khác")
        spinnerGoal.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, goals).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // ── Spinner level ─────────────────────────────────────────
        val levels = listOf("A1 - Beginner", "A2 - Elementary", "B1 - Intermediate",
            "B2 - Upper Intermediate", "C1 - Advanced", "C2 - Proficient")
        spinnerLevel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // ── Load stats từ DB local ────────────────────────────────
        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            val totalWords = db.vocabularyDao().getTotalCount()
            val allDecks   = db.deckDao().getAllDecksDirect()
            runOnUiThread {
                tvTotalWords.text = totalWords.toString()
                tvTotalDecks.text = allDecks.size.toString()
            }
        }

        // ── Load profile từ Firestore ─────────────────────────────
        uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        etName.setText(doc.getString("name") ?: "")
                        val goal  = doc.getString("goal") ?: ""
                        val level = doc.getString("level") ?: ""
                        val gi = goals.indexOf(goal)
                        val li = levels.indexOf(level)
                        if (gi != -1) spinnerGoal.setSelection(gi)
                        if (li != -1) spinnerLevel.setSelection(li)
                    }
                }
        }

        // ── Lưu profile ──────────────────────────────────────────
        btnSaveProfile.setOnClickListener {
            val name  = etName.text.toString().trim()
            val goal  = goals[spinnerGoal.selectedItemPosition]
            val level = levels[spinnerLevel.selectedItemPosition]

            uid?.let { userId ->
                firestore.collection("users").document(userId)
                    .set(mapOf("name" to name, "goal" to goal, "level" to level))
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Đã lưu hồ sơ", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi lưu: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // ── Đăng xuất ────────────────────────────────────────────
        btnLogout.setOnClickListener {
            AuthManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    // Xử lý nút back trên toolbar
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}