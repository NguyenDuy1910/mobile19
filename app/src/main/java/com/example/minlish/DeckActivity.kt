package com.example.minlish

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import android.app.AlertDialog
import com.example.minlish.DeckEntity
import com.example.minlish.FirestoreManager

class DeckActivity : AppCompatActivity() {

    private lateinit var deckDao: DeckDao
    private lateinit var vocabularyDao: VocabularyDao

    private val deckList = mutableListOf<DeckItem>()
    private lateinit var adapter: DeckAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck)

        // Bind UI
        val recyclerView    = findViewById<RecyclerView>(R.id.rvDecks)
        val etDeckName      = findViewById<EditText>(R.id.etDeckName)
        val etDeckDesc      = findViewById<EditText>(R.id.etDeckDescription)
        val btnCreateDeck   = findViewById<Button>(R.id.btnCreateDeck)
        val tvEmpty         = findViewById<TextView>(R.id.tvEmpty)

        // Lấy DAO từ Singleton DB
        val db = AppDatabase.getInstance(applicationContext)
        deckDao      = db.deckDao()
        vocabularyDao = db.vocabularyDao()

        // Setup RecyclerView
        adapter = DeckAdapter(deckList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Nhấn vào deck → mở FlashcardActivity với deckId được chọn
        adapter.setOnItemClickListener { deckItem ->
            val options = arrayOf("Học Flashcard", "Thêm từ vào deck", "Export CSV")

            AlertDialog.Builder(this)
                .setTitle(deckItem.name)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(this, FlashcardActivity::class.java)
                            intent.putExtra("deckId", deckItem.id)
                            intent.putExtra("deckName", deckItem.name)
                            startActivity(intent)
                        }
                        1 -> {
                            val intent = Intent(this, AddVocabularyActivity::class.java)
                            intent.putExtra("deckId", deckItem.id)
                            startActivity(intent)
                        }
                        2 -> {
                            lifecycleScope.launch {
                                val words = vocabularyDao.getVocabularyByDeckDirect(deckItem.id)
                                runOnUiThread {
                                    CsvExporter.exportDeck(this@DeckActivity, deckItem.name, words)
                                }
                            }
                        }
                    }
                }
                .show()
        }

        adapter.setOnItemLongClickListener { deckItem ->
            AlertDialog.Builder(this)
                .setTitle("Xóa bộ từ")
                .setMessage("Bạn có chắc muốn xóa bộ từ \"${deckItem.name}\"?\nTất cả từ trong bộ này sẽ bị xóa.")
                .setPositiveButton("Xóa") { _, _ ->
                    lifecycleScope.launch {
                        // Xóa tất cả từ trong deck trước
                        vocabularyDao.deleteVocabularyByDeck(deckItem.id)
                        // Sau đó xóa deck
                        deckDao.deleteDeck(DeckEntity(deckItem.id, deckItem.name, deckItem.description))
                        FirestoreManager.deleteDeck(DeckEntity(deckItem.id, deckItem.name, deckItem.description))
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
            true
        }
        // Tạo deck mới
        btnCreateDeck.setOnClickListener {
            val name = etDeckName.text.toString().trim()
            val desc = etDeckDesc.text.toString().trim()

            if (name.isEmpty()) {
                etDeckName.error = "Vui lòng nhập tên deck"
                return@setOnClickListener
            }

            val newDeck = DeckEntity(
                name        = name,
                description = desc
            )

            lifecycleScope.launch {
                deckDao.insertDeck(newDeck)
                FirestoreManager.syncDeck(newDeck)
                etDeckName.text.clear()
                etDeckDesc.text.clear()
                // loadDecks() sẽ tự chạy lại vì observe LiveData
            }
        }

        // Observe danh sách deck — tự cập nhật khi có deck mới
        // LiveData giống như "kênh phát sóng": mỗi khi DB thay đổi
        // → observer này chạy lại → UI tự refresh mà không cần gọi thủ công
        deckDao.getAllDecks().observe(this) { decks ->
            lifecycleScope.launch {
                // Với mỗi deck, đếm số từ bên trong
                val items = decks.map { deck ->
                    DeckItem(
                        id          = deck.id,
                        name        = deck.name,
                        description = deck.description,
                        wordCount   = deckDao.countWordsInDeck(deck.id)
                    )
                }

                runOnUiThread {
                    deckList.clear()
                    deckList.addAll(items)
                    adapter.notifyDataSetChanged()

                    // Hiện chữ "Chưa có deck nào" nếu list trống
                    tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}