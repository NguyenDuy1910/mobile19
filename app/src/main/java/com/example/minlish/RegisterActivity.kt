package com.example.minlish

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.title = "Đăng ký"

        val etEmail           = findViewById<EditText>(R.id.etEmail)
        val etPassword        = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister       = findViewById<Button>(R.id.btnRegister)
        val tvError           = findViewById<TextView>(R.id.tvError)
        val tvLogin           = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val email           = etEmail.text.toString().trim()
            val password        = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validate
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                tvError.text = "Vui lòng nhập đầy đủ thông tin"
                return@setOnClickListener
            }

            if (password.length < 6) {
                tvError.text = "Mật khẩu tối thiểu 6 ký tự"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                tvError.text = "Mật khẩu xác nhận không khớp"
                return@setOnClickListener
            }

            // Đăng ký với Firebase
            AuthManager.register(
                email     = email,
                password  = password,
                onSuccess = {
                    // Đăng ký thành công → vào MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity() // đóng cả LoginActivity và RegisterActivity
                },
                onError = { error ->
                    tvError.text = error
                }
            )
        }

        // Nhấn "Đã có tài khoản" → quay lại LoginActivity
        tvLogin.setOnClickListener {
            finish()
        }
    }
}