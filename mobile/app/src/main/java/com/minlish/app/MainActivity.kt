package com.minlish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.minlish.app.app.MinLishApp
import com.minlish.app.core.ui.theme.MinLishTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinLishTheme {
                MinLishApp()
            }
        }
    }
}
