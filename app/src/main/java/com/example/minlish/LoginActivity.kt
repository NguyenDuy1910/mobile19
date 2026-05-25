package com.example.minlish

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN_REQUEST = 4001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nếu đã đăng nhập rồi → sync rồi vào MainActivity
        if (AuthManager.isLoggedIn) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail         = findViewById<EditText>(R.id.etEmail)
        val etPassword      = findViewById<EditText>(R.id.etPassword)
        val btnLogin        = findViewById<Button>(R.id.btnLogin)
        val btnRegister     = findViewById<Button>(R.id.btnRegister)
        val btnGoogle       = findViewById<Button>(R.id.btnGoogle)
        val tvError         = findViewById<TextView>(R.id.tvError)

        // ── Views mới cho trạng thái sync ─────────────────────
        // Thêm 2 view này vào activity_login.xml (xem hướng dẫn bên dưới)
        val layoutSyncing   = findViewById<LinearLayout?>(R.id.layoutSyncing)
        val tvSyncStatus    = findViewById<TextView?>(R.id.tvSyncStatus)

        // ── ĐĂNG NHẬP EMAIL ───────────────────────────────────
        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Vui lòng nhập đầy đủ email và mật khẩu"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            setFormEnabled(false, btnLogin, btnRegister, btnGoogle, etEmail, etPassword)

            AuthManager.login(
                email     = email,
                password  = password,
                onSuccess = { goToMain(layoutSyncing, tvSyncStatus, btnLogin, btnRegister, btnGoogle, etEmail, etPassword) },
                onError   = { error ->
                    tvError.text = error
                    tvError.visibility = View.VISIBLE
                    setFormEnabled(true, btnLogin, btnRegister, btnGoogle, etEmail, etPassword)
                }
            )
        }

        // ── MỞ MÀN HÌNH ĐĂNG KÝ ──────────────────────────────
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // ── ĐĂNG NHẬP GOOGLE ──────────────────────────────────
        btnGoogle.setOnClickListener {
            val client = AuthManager.getGoogleSignInClient(this)
            client.signOut().addOnCompleteListener {
                startActivityForResult(client.signInIntent, GOOGLE_SIGN_IN_REQUEST)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
            val layoutSyncing = findViewById<LinearLayout?>(R.id.layoutSyncing)
            val tvSyncStatus  = findViewById<TextView?>(R.id.tvSyncStatus)
            val btnLogin      = findViewById<Button>(R.id.btnLogin)
            val btnRegister   = findViewById<Button>(R.id.btnRegister)
            val btnGoogle     = findViewById<Button>(R.id.btnGoogle)
            val etEmail       = findViewById<EditText>(R.id.etEmail)
            val etPassword    = findViewById<EditText>(R.id.etPassword)

            AuthManager.handleGoogleSignInResult(
                data      = data,
                onSuccess = { goToMain(layoutSyncing, tvSyncStatus, btnLogin, btnRegister, btnGoogle, etEmail, etPassword) },
                onError   = { error ->
                    findViewById<TextView>(R.id.tvError).apply {
                        text = error
                        visibility = View.VISIBLE
                    }
                }
            )
        }
    }

    /**
     * Hiện loading UI → gọi SyncManager → vào MainActivity.
     *
     * layoutSyncing và tvSyncStatus là optional (null nếu chưa thêm vào layout).
     * App vẫn hoạt động đúng — chỉ thiếu hiển thị trạng thái.
     */
    private fun goToMain(
        layoutSyncing: LinearLayout? = null,
        tvSyncStatus:  TextView?     = null,
        vararg formViews: View
    ) {
        // Hiện loading panel, ẩn form
        layoutSyncing?.visibility = View.VISIBLE
        formViews.forEach { it.visibility = View.GONE }

        SyncManager.syncOnLogin(
            context       = applicationContext,
            onSyncStatus  = { status ->
                // Cập nhật text trạng thái sync (chạy trên main thread do SyncManager đảm bảo)
                tvSyncStatus?.text = status
            },
            onDone = {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        )
    }

    // Bật/tắt toàn bộ form khi đang xử lý login
    private fun setFormEnabled(enabled: Boolean, vararg views: View) {
        views.forEach { it.isEnabled = enabled }
    }
}