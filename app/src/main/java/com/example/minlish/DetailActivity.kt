package com.example.minlish

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detail)

        val tvWord = findViewById<TextView>(R.id.tvWord)
        val tvMeaning = findViewById<TextView>(R.id.tvMeaning)

        val word = intent.getStringExtra("word")
        val meaning = intent.getStringExtra("meaning")

        tvWord.text = word
        tvMeaning.text = meaning
    }
}