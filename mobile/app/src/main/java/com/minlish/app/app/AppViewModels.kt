package com.minlish.app.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minlish.app.core.model.*
import com.minlish.app.core.network.ApiResult
import com.minlish.app.core.network.UiState
import java.util.TimeZone
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class NeedsProfile(val me: MeDto) : SessionState
    data class Ready(val me: MeDto) : SessionState
}

class AppStateViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.authRepository.initialize()
            container.authRepository.tokens.collect { tokens ->
                if (tokens.isAuthenticated) refresh() else _state.value = SessionState.SignedOut
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = SessionState.Loading
            when (val result = container.authRepository.me()) {
                is ApiResult.Error -> _state.value = SessionState.SignedOut
                is ApiResult.Success -> {
                    _state.value = if (result.data.profile.isComplete) {
                        SessionState.Ready(result.data)
                    } else {
                        SessionState.NeedsProfile(result.data)
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            container.authRepository.logout()
            _state.value = SessionState.SignedOut
        }
    }
}

data class AuthFormState(val loading: Boolean = false, val error: String? = null)

class AuthViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AuthFormState())
    val state = _state.asStateFlow()

    fun login(email: String, password: String) = authenticate { container.authRepository.login(email, password) }
    fun register(email: String, password: String) = authenticate { container.authRepository.register(email, password) }

    private fun authenticate(block: suspend () -> ApiResult<MeDto>) {
        viewModelScope.launch {
            _state.value = AuthFormState(loading = true)
            _state.value = when (val result = block()) {
                is ApiResult.Error -> AuthFormState(error = result.message)
                is ApiResult.Success -> AuthFormState()
            }
        }
    }
}

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ProfileDto>>(UiState.Idle)
    val state = _state.asStateFlow()

    fun save(name: String, goal: String, level: String, newWords: Int, reviewLimit: Int, notificationTime: String?) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = container.authRepository.updateProfile(
                ProfileUpdateRequest(
                    name = name.trim(),
                    learningGoal = goal,
                    englishLevel = level,
                    dailyNewWords = newWords,
                    dailyReviewLimit = reviewLimit,
                    notificationTime = notificationTime?.takeIf { it.isNotBlank() },
                    timezone = TimeZone.getDefault().id,
                ),
            ).toUiState()
        }
    }
}

data class HomeData(val me: MeDto, val plan: DailyPlanDto, val progress: ProgressSummaryDto, val decks: List<DeckDto>)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Idle)
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val me = async { container.authRepository.me() }
            val plan = async { container.learningRepository.plan() }
            val progress = async { container.progressRepository.summary() }
            val decks = async { container.deckRepository.list() }
            val meResult = me.await()
            val planResult = plan.await()
            val progressResult = progress.await()
            val decksResult = decks.await()
            val error = listOf(meResult, planResult, progressResult, decksResult).filterIsInstance<ApiResult.Error>().firstOrNull()
            _state.value = if (error != null) {
                UiState.Error(error.message)
            } else {
                UiState.Success(
                    HomeData(
                        (meResult as ApiResult.Success).data,
                        (planResult as ApiResult.Success).data,
                        (progressResult as ApiResult.Success).data,
                        (decksResult as ApiResult.Success).data,
                    ),
                )
            }
        }
    }
}

data class DeckDetailData(val deck: DeckDto, val words: List<VocabItemDto>)
data class WordEditorState(val loading: Boolean = false, val lookup: DictionaryWordDto? = null, val error: String? = null, val saved: Boolean = false)

class DeckViewModel(private val container: AppContainer) : ViewModel() {
    private val _decks = MutableStateFlow<UiState<List<DeckDto>>>(UiState.Idle)
    val decks = _decks.asStateFlow()
    private val _detail = MutableStateFlow<UiState<DeckDetailData>>(UiState.Idle)
    val detail = _detail.asStateFlow()
    private val _editor = MutableStateFlow(WordEditorState())
    val editor = _editor.asStateFlow()
    private val _deckEditorError = MutableStateFlow<String?>(null)
    val deckEditorError = _deckEditorError.asStateFlow()

    fun loadDecks() = viewModelScope.launch {
        _decks.value = UiState.Loading
        _decks.value = container.deckRepository.list().toUiState(emptyIf = { it.isEmpty() })
    }

    fun loadDetail(deckId: String) = viewModelScope.launch {
        _detail.value = UiState.Loading
        val deck = container.deckRepository.get(deckId)
        val words = container.vocabularyRepository.list(deckId)
        _detail.value = when {
            deck is ApiResult.Error -> UiState.Error(deck.message)
            words is ApiResult.Error -> UiState.Error(words.message)
            else -> UiState.Success(DeckDetailData((deck as ApiResult.Success).data, (words as ApiResult.Success).data))
        }
    }

    fun saveDeck(id: String?, name: String, description: String, tags: String, onSaved: () -> Unit) = viewModelScope.launch {
        _deckEditorError.value = null
        val normalizedTags = tags.split(",").map(String::trim).filter(String::isNotBlank)
        val result = if (id == null) {
            container.deckRepository.create(DeckRequest(name.trim(), description.trim().ifBlank { null }, normalizedTags))
        } else {
            container.deckRepository.update(id, DeckUpdateRequest(name.trim(), description.trim().ifBlank { null }, normalizedTags))
        }
        if (result is ApiResult.Success) onSaved() else _deckEditorError.value = (result as ApiResult.Error).message
    }

    fun deleteDeck(id: String, onDeleted: () -> Unit) = viewModelScope.launch {
        when (val result = container.deckRepository.delete(id)) {
            is ApiResult.Error -> _detail.value = UiState.Error(result.message)
            is ApiResult.Success -> onDeleted()
        }
    }

    fun lookup(word: String) = viewModelScope.launch {
        _editor.value = WordEditorState(loading = true)
        _editor.value = when (val result = container.vocabularyRepository.lookup(word.trim())) {
            is ApiResult.Error -> WordEditorState(error = result.message)
            is ApiResult.Success -> WordEditorState(lookup = result.data)
        }
    }

    fun clearEditor() { _editor.value = WordEditorState() }

    fun saveWord(deckId: String, request: VocabItemRequest, enriched: Boolean) = viewModelScope.launch {
        _editor.value = _editor.value.copy(loading = true, error = null)
        val result = if (enriched) {
            container.vocabularyRepository.enrich(
                deckId,
                EnrichWordRequest(
                    request.word, request.pronunciation, request.meaning, request.descriptionEn,
                    request.example, request.collocations, request.relatedWords, request.note,
                    request.partOfSpeech, request.phonetic, request.audioUrl, request.synonyms, request.antonyms,
                ),
            )
        } else {
            container.vocabularyRepository.create(deckId, request)
        }
        _editor.value = when (result) {
            is ApiResult.Error -> WordEditorState(error = result.message)
            is ApiResult.Success -> WordEditorState(saved = true)
        }
    }

    fun updateWord(wordId: String, request: VocabItemUpdateRequest, onSaved: () -> Unit) = viewModelScope.launch {
        when (val result = container.vocabularyRepository.update(wordId, request)) {
            is ApiResult.Error -> _editor.value = WordEditorState(error = result.message)
            is ApiResult.Success -> onSaved()
        }
    }

    fun deleteWord(wordId: String, onDeleted: () -> Unit) = viewModelScope.launch {
        when (val result = container.vocabularyRepository.delete(wordId)) {
            is ApiResult.Error -> _detail.value = UiState.Error(result.message)
            is ApiResult.Success -> onDeleted()
        }
    }

    fun exportDeck(deckId: String, onExported: (String) -> Unit) = viewModelScope.launch {
        when (val result = container.vocabularyRepository.export(deckId)) {
            is ApiResult.Error -> _detail.value = UiState.Error(result.message)
            is ApiResult.Success -> onExported(result.data)
        }
    }

    fun importDeck(deckId: String, fileName: String, content: ByteArray, onImported: (String) -> Unit) = viewModelScope.launch {
        when (val result = container.vocabularyRepository.import(deckId, fileName, content)) {
            is ApiResult.Error -> _detail.value = UiState.Error(result.message)
            is ApiResult.Success -> {
                val rowErrors = result.data.errors.take(3).joinToString(" ") { "Row ${it.row}: ${it.message}" }
                onImported("Imported ${result.data.imported} words; skipped ${result.data.skipped} rows. $rowErrors".trim())
                loadDetail(deckId)
            }
        }
    }
}

data class LearningData(
    val words: List<DueWordDto> = emptyList(),
    val index: Int = 0,
    val sessionId: String? = null,
    val correct: Int = 0,
    val wrong: Int = 0,
    val completed: Boolean = false,
) {
    val current: DueWordDto? get() = words.getOrNull(index)
}

class LearningViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<UiState<LearningData>>(UiState.Idle)
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        val words = container.learningRepository.dueWords()
        if (words is ApiResult.Error) {
            _state.value = UiState.Error(words.message)
            return@launch
        }
        val due = (words as ApiResult.Success).data
        if (due.isEmpty()) {
            _state.value = UiState.Empty
            return@launch
        }
        val session = container.learningRepository.startSession()
        _state.value = if (session is ApiResult.Error) UiState.Error(session.message) else {
            UiState.Success(LearningData(words = due, sessionId = (session as ApiResult.Success).data.id))
        }
    }

    fun rate(rating: String) = viewModelScope.launch {
        val data = (_state.value as? UiState.Success)?.data ?: return@launch
        val current = data.current ?: return@launch
        when (val result = container.learningRepository.review(current.vocabItem.id, rating)) {
            is ApiResult.Error -> _state.value = UiState.Error(result.message)
            is ApiResult.Success -> {
                val next = data.copy(
                    index = data.index + 1,
                    correct = data.correct + if (rating == "again") 0 else 1,
                    wrong = data.wrong + if (rating == "again") 1 else 0,
                    completed = data.index + 1 >= data.words.size,
                )
                _state.value = UiState.Success(next)
                if (next.completed) end(next)
            }
        }
    }

    fun endIfNeeded() {
        val data = (_state.value as? UiState.Success)?.data ?: return
        if (!data.completed) viewModelScope.launch { end(data) }
    }

    private suspend fun end(data: LearningData) {
        data.sessionId?.let {
            container.learningRepository.endSession(
                SessionEndRequest(it, reviewWordsCount = data.index, correctCount = data.correct, wrongCount = data.wrong),
            )
        }
    }
}

data class ProgressData(val summary: ProgressSummaryDto, val retention: RetentionDto, val activity: List<DailyActivityDto>, val decks: List<Pair<DeckDto, DeckProgressDto>>)

class ProgressViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ProgressData>>(UiState.Idle)
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        val summary = container.progressRepository.summary()
        val retention = container.progressRepository.retention()
        val activity = container.progressRepository.activity()
        val decks = container.deckRepository.list()
        val error = listOf(summary, retention, activity, decks).filterIsInstance<ApiResult.Error>().firstOrNull()
        if (error != null) {
            _state.value = UiState.Error(error.message)
            return@launch
        }
        val deckDtos = (decks as ApiResult.Success).data
        val deckProgress = deckDtos.mapNotNull { deck ->
            when (val result = container.progressRepository.deck(deck.id)) {
                is ApiResult.Error -> null
                is ApiResult.Success -> deck to result.data
            }
        }
        _state.value = UiState.Success(
            ProgressData(
                (summary as ApiResult.Success).data,
                (retention as ApiResult.Success).data,
                (activity as ApiResult.Success).data,
                deckProgress,
            ),
        )
    }
}

data class PracticeData(
    val questions: List<PracticeQuestionDto> = emptyList(),
    val index: Int = 0,
    val answers: List<PracticeAnswerDto> = emptyList(),
    val feedback: PracticeAnswerDto? = null,
) {
    val current get() = questions.getOrNull(index)
    val completed get() = questions.isNotEmpty() && index >= questions.size
}

class PracticeViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<UiState<PracticeData>>(UiState.Idle)
    val state = _state.asStateFlow()
    private val _history = MutableStateFlow<UiState<List<PracticeAnswerDto>>>(UiState.Idle)
    val history = _history.asStateFlow()

    fun generate(limit: Int = 5) = viewModelScope.launch {
        _state.value = UiState.Loading
        _state.value = container.practiceRepository.generate(limit).toUiState(emptyIf = { it.isEmpty() }).map { PracticeData(questions = it) }
    }

    fun submit(answer: String) = viewModelScope.launch {
        val data = (_state.value as? UiState.Success)?.data ?: return@launch
        val question = data.current ?: return@launch
        when (val result = container.practiceRepository.submit(question.vocabItemId, answer)) {
            is ApiResult.Error -> _state.value = UiState.Error(result.message)
            is ApiResult.Success -> _state.value = UiState.Success(data.copy(answers = data.answers + result.data, feedback = result.data))
        }
    }

    fun next() {
        val data = (_state.value as? UiState.Success)?.data ?: return
        _state.value = UiState.Success(data.copy(index = data.index + 1, feedback = null))
    }

    fun loadHistory() = viewModelScope.launch {
        _history.value = UiState.Loading
        _history.value = container.practiceRepository.history().toUiState(emptyIf = { it.isEmpty() })
    }
}

data class SettingsData(val loading: Boolean = false, val message: String? = null, val error: String? = null)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SettingsData())
    val state = _state.asStateFlow()

    fun saveNotifications(enabled: Boolean, time: String, timezone: String) = viewModelScope.launch {
        _state.value = SettingsData(loading = true)
        _state.value = when (val result = container.notificationRepository.update(NotificationSettingsRequest(enabled, time.ifBlank { null }, timezone))) {
            is ApiResult.Error -> SettingsData(error = result.message)
            is ApiResult.Success -> SettingsData(message = "Reminder settings saved.")
        }
    }

    fun registerPlaceholderDevice() = viewModelScope.launch {
        val token = "minlish-placeholder-${System.currentTimeMillis()}"
        _state.value = when (val result = container.notificationRepository.registerPlaceholderDevice(token)) {
            is ApiResult.Error -> SettingsData(error = result.message)
            is ApiResult.Success -> SettingsData(message = "Placeholder Android device registered.")
        }
    }

    fun testNotification() = viewModelScope.launch {
        _state.value = when (val result = container.notificationRepository.test()) {
            is ApiResult.Error -> SettingsData(error = result.message)
            is ApiResult.Success -> SettingsData(message = result.data.message)
        }
    }
}

data class AgentData(
    val loading: Boolean = false,
    val explanation: ExplainWordDto? = null,
    val examples: GenerateExamplesDto? = null,
    val deck: GenerateDeckDto? = null,
    val error: String? = null,
)

class AgentViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AgentData())
    val state = _state.asStateFlow()

    fun explain(word: String) = viewModelScope.launch {
        _state.value = AgentData(loading = true)
        _state.value = when (val result = container.agentRepository.explain(word.trim())) {
            is ApiResult.Error -> AgentData(error = result.message)
            is ApiResult.Success -> AgentData(explanation = result.data)
        }
    }

    fun examples(word: String) = viewModelScope.launch {
        _state.value = AgentData(loading = true)
        _state.value = when (val result = container.agentRepository.examples(word.trim())) {
            is ApiResult.Error -> AgentData(error = result.message)
            is ApiResult.Success -> AgentData(examples = result.data)
        }
    }

    fun deck(topic: String) = viewModelScope.launch {
        _state.value = AgentData(loading = true)
        _state.value = when (val result = container.agentRepository.deck(topic.trim())) {
            is ApiResult.Error -> AgentData(error = result.message)
            is ApiResult.Success -> AgentData(deck = result.data)
        }
    }
}

class MinLishViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AppStateViewModel::class.java -> AppStateViewModel(container)
        AuthViewModel::class.java -> AuthViewModel(container)
        ProfileViewModel::class.java -> ProfileViewModel(container)
        HomeViewModel::class.java -> HomeViewModel(container)
        DeckViewModel::class.java -> DeckViewModel(container)
        LearningViewModel::class.java -> LearningViewModel(container)
        ProgressViewModel::class.java -> ProgressViewModel(container)
        PracticeViewModel::class.java -> PracticeViewModel(container)
        SettingsViewModel::class.java -> SettingsViewModel(container)
        AgentViewModel::class.java -> AgentViewModel(container)
        else -> error("Unknown ViewModel: ${modelClass.simpleName}")
    } as T
}

private fun <T> ApiResult<T>.toUiState(emptyIf: ((T) -> Boolean)? = null): UiState<T> = when (this) {
    is ApiResult.Error -> UiState.Error(message)
    is ApiResult.Success -> if (emptyIf?.invoke(data) == true) UiState.Empty else UiState.Success(data)
}

private fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    UiState.Empty -> UiState.Empty
    is UiState.Error -> this
    UiState.Idle -> UiState.Idle
    UiState.Loading -> UiState.Loading
    is UiState.Success -> UiState.Success(transform(data))
}
