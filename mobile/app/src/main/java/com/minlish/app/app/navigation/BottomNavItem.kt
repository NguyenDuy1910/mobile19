package com.minlish.app.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
) {
    Home("Home", Icons.Default.Home, "home"),
    Decks("Decks", Icons.AutoMirrored.Filled.List, "decks"),
    Progress("Progress", Icons.Default.Star, "progress"),
    Settings("Settings", Icons.Default.Settings, "settings"),
}

