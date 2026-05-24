package com.minlish.app.app.navigation

sealed class AppRoute(
    val route: String,
    val title: String,
) {
    data object Auth : AppRoute("auth", "Auth")
    data object Onboarding : AppRoute("onboarding", "Onboarding")
    data object Home : AppRoute("home", "Home")
    data object Decks : AppRoute("decks", "Decks")
    data object Learning : AppRoute("learning", "Learning")
    data object Progress : AppRoute("progress", "Progress")
    data object Settings : AppRoute("settings", "Settings")
}
