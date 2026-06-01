package com.minlish.app.app.navigation

sealed class AppRoute(
    val route: String,
    val title: String,
) {
    data object Login : AppRoute("login", "Log in")
    data object Register : AppRoute("register", "Register")
    data object Onboarding : AppRoute("onboarding", "Profile setup")
    data object Home : AppRoute("home", "Home")
    data object Decks : AppRoute("decks", "Decks")
    data object Learning : AppRoute("learning", "Learning")
    data object Progress : AppRoute("progress", "Progress")
    data object Settings : AppRoute("settings", "Settings")
    data object Profile : AppRoute("profile", "Profile settings")
    data object CreateDeck : AppRoute("decks/create", "Create deck")
    data object DeckDetail : AppRoute("decks/{deckId}", "Deck detail") {
        fun route(deckId: String) = "decks/$deckId"
    }
    data object EditDeck : AppRoute("decks/{deckId}/edit", "Edit deck") {
        fun route(deckId: String) = "decks/$deckId/edit"
    }
    data object AddWord : AppRoute("decks/{deckId}/add-word", "Add word") {
        fun route(deckId: String) = "decks/$deckId/add-word"
    }
    data object WordDetail : AppRoute("decks/{deckId}/words/{wordId}", "Word detail") {
        fun route(deckId: String, wordId: String) = "decks/$deckId/words/$wordId"
    }
    data object EditWord : AppRoute("decks/{deckId}/words/{wordId}/edit", "Edit word") {
        fun route(deckId: String, wordId: String) = "decks/$deckId/words/$wordId/edit"
    }
    data object Practice : AppRoute("practice", "Practice")
    data object PracticeHistory : AppRoute("practice/history", "Practice history")
    data object Notifications : AppRoute("notifications", "Notifications")
    data object WordHelp : AppRoute("agent/help?word={word}", "Word help") {
        fun route(word: String = "") = "agent/help?word=${android.net.Uri.encode(word)}"
    }
    data object GenerateExamples : AppRoute("agent/examples", "Generate examples")
    data object GenerateDeck : AppRoute("agent/deck", "Generate deck")
}
