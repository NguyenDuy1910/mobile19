package com.minlish.app.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String,
) {
    Home("Home", Icons.Outlined.Home, Icons.Default.Home, "home"),
    Decks("Decks", Icons.Outlined.Style, Icons.Default.Style, "decks"),
    Learn("Learn", Icons.Outlined.School, Icons.Default.School, "learning"),
    Progress("Progress", Icons.AutoMirrored.Outlined.TrendingUp, Icons.AutoMirrored.Filled.TrendingUp, "progress"),
    Settings("Settings", Icons.Outlined.Settings, Icons.Default.Settings, "settings"),
}
