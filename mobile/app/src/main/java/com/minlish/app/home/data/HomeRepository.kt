package com.minlish.app.home.data

import com.minlish.app.auth.data.AuthRepository
import com.minlish.app.deck.data.DeckRepository
import com.minlish.app.learning.data.LearningRepository
import com.minlish.app.progress.data.ProgressRepository

class HomeRepository(
    val auth: AuthRepository,
    val decks: DeckRepository,
    val learning: LearningRepository,
    val progress: ProgressRepository,
)
