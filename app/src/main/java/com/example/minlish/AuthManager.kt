package com.example.minlish

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

object AuthManager {

    private val auth = FirebaseAuth.getInstance()

    // Web Client ID từ Firebase Console
    private const val WEB_CLIENT_ID = "511181274296-fumbnrcanka77rhatia01akkqnt51tme.apps.googleusercontent.com"

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // ── EMAIL/PASSWORD ─────────────────────────────────────

    fun register(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Lỗi đăng ký") }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Lỗi đăng nhập") }
    }

    // ── GOOGLE LOGIN ───────────────────────────────────────

    // Tạo GoogleSignInClient — cần Activity context
    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID) // dùng để xác thực với Firebase
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    // Xử lý kết quả sau khi người dùng chọn tài khoản Google
    fun handleGoogleSignInResult(
        data: Intent?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            // Dùng credential để đăng nhập với Firebase
            auth.signInWithCredential(credential)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Lỗi đăng nhập Google") }

        } catch (e: Exception) {
            onError(e.message ?: "Lỗi Google Sign-In")
        }
    }

    fun logout() {
        auth.signOut()
    }
}