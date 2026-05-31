package com.minlish.app.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.minlish.app.app.AuthFormState
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors
import com.minlish.app.core.ui.theme.MinLishTheme

@Composable
fun LoginScreen(state: AuthFormState, onLogin: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AuthLayout(
        title = "Welcome back 👋",
        subtitle = "Learn words that stay with you",
    ) {
        MinLishTextField(email, { email = it }, "Email", leadingIcon = Icons.Default.MailOutline)
        MinLishPasswordField(password, { password = it }, leadingIcon = Icons.Outlined.Lock)
        state.error?.let { AuthError(it) }
        Spacer(Modifier.height(Spacing.xs))
        PrimaryButton("Log in", onClick = { onLogin(email, password) }, enabled = !state.loading)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("New to MinLish? Create an account", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun RegisterScreen(state: AuthFormState, onRegister: (String, String) -> Unit, onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    AuthLayout(
        title = "Create your account ✨",
        subtitle = "Start a small daily vocabulary habit",
    ) {
        MinLishTextField(email, { email = it }, "Email", leadingIcon = Icons.Default.MailOutline)
        MinLishPasswordField(password, { password = it }, leadingIcon = Icons.Outlined.Lock)
        MinLishPasswordField(confirmation, { confirmation = it }, label = "Confirm password", leadingIcon = Icons.Outlined.Lock)
        (localError ?: state.error)?.let { AuthError(it) }
        Spacer(Modifier.height(Spacing.xs))
        PrimaryButton("Create account", onClick = {
            localError = when {
                password.length < 8 -> "Password must contain at least 8 characters."
                password != confirmation -> "Passwords do not match."
                else -> null
            }
            if (localError == null) onRegister(email, password)
        }, enabled = !state.loading)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Already have an account? Log in", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AuthError(message: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(Spacing.sm),
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AuthLayout(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(top = 64.dp, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BrandHeader()
            Spacer(Modifier.height(Spacing.md))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(Spacing.sm))
            content()
        }
    }
}

/** Cute MinLish logo: a soft gradient rounded badge with a sprout, plus the wordmark. */
@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(MinLishColors.HeroGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🌱", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        }
        Column {
            Text("MinLish", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Text("Tiny words, big growth", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    MinLishTheme { LoginScreen(AuthFormState(), { _, _ -> }, {}) }
}

@Preview(showBackground = true)
@Composable
private fun RegisterPreview() {
    MinLishTheme { RegisterScreen(AuthFormState(), { _, _ -> }, {}) }
}

