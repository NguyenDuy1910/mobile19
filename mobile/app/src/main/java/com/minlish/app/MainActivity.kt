package com.minlish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.minlish.app.app.AppContainer
import com.minlish.app.app.MinLishApp
import com.minlish.app.core.ui.theme.MinLishTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinLishTheme {
                MinLishApp(container)
            }
        }
    }
}
