package com.minlish.app.app

import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.minlish.app.core.utils.MinLishLog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.minlish.app.agent.ui.*
import com.minlish.app.app.navigation.AppRoute
import com.minlish.app.app.navigation.BottomNavItem
import com.minlish.app.auth.ui.LoginScreen
import com.minlish.app.auth.ui.RegisterScreen
import com.minlish.app.core.network.UiState
import com.minlish.app.core.ui.components.LoadingState
import com.minlish.app.deck.ui.*
import com.minlish.app.home.ui.HomeScreen
import com.minlish.app.learning.ui.FlashcardScreen
import com.minlish.app.notifications.ui.NotificationSettingsScreen
import com.minlish.app.onboarding.ui.ProfileSetupScreen
import com.minlish.app.practice.ui.PracticeHistoryScreen
import com.minlish.app.practice.ui.PracticeScreen
import com.minlish.app.progress.ui.ProgressDashboardScreen
import com.minlish.app.settings.ui.SettingsScreen
import com.minlish.app.vocabulary.ui.WordDetailScreen
import com.minlish.app.vocabulary.ui.WordEditorScreen
import kotlinx.coroutines.launch

@Composable
fun MinLishApp(container: AppContainer) {
    val factory = remember(container) { MinLishViewModelFactory(container) }
    val appState: AppStateViewModel = viewModel(factory = factory)
    when (val session = appState.state.collectAsStateWithLifecycle().value) {
        SessionState.Loading -> {
            com.minlish.app.core.utils.MinLishLog.d("App", "Session state: Loading")
            LoadingState("Opening MinLish...")
        }
        SessionState.SignedOut -> {
            com.minlish.app.core.utils.MinLishLog.d("App", "Session state: SignedOut → showing Auth")
            AuthNavigation(factory)
        }
        is SessionState.NeedsProfile -> {
            com.minlish.app.core.utils.MinLishLog.d("App", "Session state: NeedsProfile for ${session.me.email}")
            val profile: ProfileViewModel = viewModel(factory = factory)
            val state by profile.state.collectAsStateWithLifecycle()
            LaunchedEffect(state) { if (state is UiState.Success) appState.refresh() }
            // ── CHANGED: truyền thêm onLogout = appState::logout ──
            ProfileSetupScreen(state, session.me.profile, profile::save, onLogout = appState::logout)
        }
        is SessionState.Ready -> {
            com.minlish.app.core.utils.MinLishLog.d("App", "Session state: Ready for ${session.me.profile.name} (${session.me.email})")
            MainNavigation(session.me, factory, appState::refresh, appState::logout)
        }
    }
}

@Composable
private fun AuthNavigation(factory: MinLishViewModelFactory) {
    val navController = rememberNavController()
    val auth: AuthViewModel = viewModel(factory = factory)
    val state by auth.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── Google Sign-In helper ────────────────────────────────────
    val credentialManager = remember { CredentialManager.create(context) }

    fun launchGoogleSignIn() {
        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    // Thay bằng Web Client ID của Firebase project
                    // Lấy từ: Firebase Console → Project Settings → Web API Key
                    // hoặc google-services.json → oauth_client[type=3].client_id
                    .setServerClientId(context.getString(com.minlish.app.R.string.google_web_client_id))
                    .setFilterByAuthorizedAccounts(false) // cho phép chọn bất kỳ tài khoản Google
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    auth.loginWithGoogle(credential.idToken)
                } else if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    // Parse từ CustomCredential
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    auth.loginWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    MinLishLog.e("AuthNav", "Unexpected credential type: ${credential::class.simpleName}")
                }
            } catch (e: GetCredentialException) {
                MinLishLog.e("AuthNav", "Google Sign-In cancelled or failed: ${e.message}")
                // Người dùng huỷ → không cần báo lỗi
            } catch (e: Exception) {
                MinLishLog.e("AuthNav", "Google Sign-In error: ${e.message}")
            }
        }
    }

    NavHost(navController, startDestination = AppRoute.Login.route) {
        composable(AppRoute.Login.route) {
            LoginScreen(
                state = state,
                onLogin = auth::login,
                onGoogleSignIn = ::launchGoogleSignIn,
                onRegister = { navController.navigate(AppRoute.Register.route) },
            )
        }
        composable(AppRoute.Register.route) {
            RegisterScreen(
                state = state,
                onRegister = auth::register,
                onGoogleSignIn = ::launchGoogleSignIn,
                onLogin = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainNavigation(me: com.minlish.app.core.model.MeDto, factory: MinLishViewModelFactory, refreshSession: () -> Unit, logout: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val bottomRoutes = BottomNavItem.entries.map { it.route }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (currentRoute in bottomRoutes) NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                BottomNavItem.entries.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { outerPadding ->
        NavHost(navController, startDestination = AppRoute.Home.route, modifier = Modifier.padding(outerPadding)) {
            composable(AppRoute.Home.route) {
                com.minlish.app.core.utils.MinLishLog.nav("Home")
                val vm: HomeViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.load() }

                // ── CSV import launcher ───────────────────────────
                val deckVm: DeckViewModel = viewModel(factory = factory)
                val csvLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        val firstDeckId = (state as? UiState.Success)?.data?.decks?.firstOrNull()?.id
                        if (firstDeckId != null) {
                            val content = context.contentResolver
                                .openInputStream(uri)?.use { it.readBytes() }
                            if (content != null) {
                                deckVm.importDeck(
                                    firstDeckId,
                                    uri.lastPathSegment ?: "import.csv",
                                    content,
                                ) { message ->
                                    coroutineScope.launch {
                                        snackbar.showSnackbar(message)
                                        vm.load()
                                    }
                                }
                            } else {
                                coroutineScope.launch { snackbar.showSnackbar("Could not read that CSV file.") }
                            }
                        } else {
                            coroutineScope.launch { snackbar.showSnackbar("Please create a deck first before importing.") }
                        }
                    }
                }

                HomeScreen(
                    state, vm::load,
                    { navController.navigate(AppRoute.CreateDeck.route) },
                    {
                        val firstDeck = (state as? UiState.Success)?.data?.decks?.firstOrNull()
                        if (firstDeck != null) navController.navigate(AppRoute.AddWord.route(firstDeck.id))
                    },
                    { navController.navigate(AppRoute.Learning.route) },
                    { navController.navigate(AppRoute.WordHelp.route()) },
                    onImportCsv = { csvLauncher.launch(arrayOf("text/csv", "text/*", "application/csv")) },
                )
            }
            composable(AppRoute.Decks.route) {
                com.minlish.app.core.utils.MinLishLog.nav("Decks")
                val vm: DeckViewModel = viewModel(factory = factory)
                val state by vm.decks.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.loadDecks() }
                DeckListScreen(state, vm::loadDecks, { navController.navigate(AppRoute.CreateDeck.route) }) { navController.navigate(AppRoute.DeckDetail.route(it)) }
            }
            composable(AppRoute.CreateDeck.route) {
                com.minlish.app.core.utils.MinLishLog.nav("CreateDeck")
                val vm: DeckViewModel = viewModel(factory = factory)
                val error by vm.deckEditorError.collectAsStateWithLifecycle()
                DeckEditorScreen(error = error, onSave = { name, description, tags -> vm.saveDeck(null, name, description, tags) { navController.popBackStack() } }, onBack = navController::popBackStack)
            }
            composable(AppRoute.DeckDetail.route, arguments = listOf(navArgument("deckId") { type = NavType.StringType })) { entry ->
                val deckId = entry.arguments?.getString("deckId").orEmpty()
                com.minlish.app.core.utils.MinLishLog.nav("DeckDetail(deckId=$deckId)")
                val vm: DeckViewModel = viewModel(factory = factory)
                val state by vm.detail.collectAsStateWithLifecycle()
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        val content = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (content != null) {
                            vm.importDeck(deckId, uri.lastPathSegment ?: "minlish-import.csv", content) { message ->
                                coroutineScope.launch { snackbar.showSnackbar(message) }
                            }
                        } else {
                            coroutineScope.launch { snackbar.showSnackbar("Could not read that CSV file.") }
                        }
                    }
                }
                LaunchedEffect(deckId) { vm.loadDetail(deckId) }
                DeckDetailScreen(
                    state, { vm.loadDetail(deckId) }, navController::popBackStack,
                    { navController.navigate(AppRoute.EditDeck.route(deckId)) },
                    { vm.deleteDeck(deckId) { navController.popBackStack() } },
                    { navController.navigate(AppRoute.AddWord.route(deckId)) },
                    { navController.navigate(AppRoute.WordDetail.route(deckId, it)) },
                    { navController.navigate(AppRoute.Learning.route) },
                    { importLauncher.launch(arrayOf("text/csv", "text/*", "application/csv")) },
                    {
                        vm.exportDeck(deckId) { csv ->
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }, "Share MinLish CSV"))
                        }
                    },
                )
            }
            composable(AppRoute.EditDeck.route, arguments = listOf(navArgument("deckId") { type = NavType.StringType })) { entry ->
                val deckId = entry.arguments?.getString("deckId").orEmpty()
                val vm: DeckViewModel = viewModel(factory = factory)
                val state by vm.detail.collectAsStateWithLifecycle()
                val error by vm.deckEditorError.collectAsStateWithLifecycle()
                LaunchedEffect(deckId) { vm.loadDetail(deckId) }
                when (state) {
                    is UiState.Error   -> com.minlish.app.core.ui.components.ErrorState((state as UiState.Error).message) { vm.loadDetail(deckId) }
                    is UiState.Success -> DeckEditorScreen((state as UiState.Success).data.deck, error, { name, description, tags -> vm.saveDeck(deckId, name, description, tags) { navController.popBackStack() } }, navController::popBackStack)
                    else               -> LoadingState("Loading deck...")
                }
            }
            composable(AppRoute.AddWord.route, arguments = listOf(navArgument("deckId") { type = NavType.StringType })) { entry ->
                val deckId = entry.arguments?.getString("deckId").orEmpty()
                val vm: DeckViewModel = viewModel(factory = factory)
                val editor by vm.editor.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.clearEditor() }
                LaunchedEffect(editor.saved) { if (editor.saved) navController.popBackStack() }
                WordEditorScreen(editor, onBack = navController::popBackStack, onLookup = vm::lookup, onCreate = { request, enriched -> vm.saveWord(deckId, request, enriched) }, onUpdate = { _, _ -> })
            }
            composable(AppRoute.WordDetail.route, arguments = listOf(navArgument("deckId") { type = NavType.StringType }, navArgument("wordId") { type = NavType.StringType })) { entry ->
                val deckId = entry.arguments?.getString("deckId").orEmpty()
                val wordId = entry.arguments?.getString("wordId").orEmpty()
                val vm: DeckViewModel = viewModel(factory = factory)
                val state by vm.detail.collectAsStateWithLifecycle()
                LaunchedEffect(deckId) { vm.loadDetail(deckId) }
                val word = (state as? UiState.Success)?.data?.words?.firstOrNull { it.id == wordId }
                when {
                    state is UiState.Error -> com.minlish.app.core.ui.components.ErrorState((state as UiState.Error).message) { vm.loadDetail(deckId) }
                    word != null           -> WordDetailScreen(word, navController::popBackStack, { navController.navigate(AppRoute.EditWord.route(deckId, wordId)) }, { vm.deleteWord(wordId) { navController.popBackStack() } }, { navController.navigate(AppRoute.WordHelp.route(word.word)) })
                    else                   -> LoadingState("Loading word...")
                }
            }
            composable(AppRoute.EditWord.route, arguments = listOf(navArgument("deckId") { type = NavType.StringType }, navArgument("wordId") { type = NavType.StringType })) { entry ->
                val deckId = entry.arguments?.getString("deckId").orEmpty()
                val wordId = entry.arguments?.getString("wordId").orEmpty()
                val vm: DeckViewModel = viewModel(factory = factory)
                val state by vm.detail.collectAsStateWithLifecycle()
                val editor by vm.editor.collectAsStateWithLifecycle()
                LaunchedEffect(deckId) { vm.loadDetail(deckId) }
                val word = (state as? UiState.Success)?.data?.words?.firstOrNull { it.id == wordId }
                when {
                    state is UiState.Error -> com.minlish.app.core.ui.components.ErrorState((state as UiState.Error).message) { vm.loadDetail(deckId) }
                    word != null           -> WordEditorScreen(editor, word, navController::popBackStack, {}, { _, _ -> }, { id, request -> vm.updateWord(id, request) { navController.popBackStack() } })
                    else                   -> LoadingState("Loading word...")
                }
            }
            composable(AppRoute.Learning.route) {
                com.minlish.app.core.utils.MinLishLog.nav("Learning/Flashcard")
                val vm: LearningViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.load() }
                DisposableEffect(Unit) { onDispose(vm::endIfNeeded) }
                FlashcardScreen(state, vm::load, navController::popBackStack, vm::rate)
            }
            composable(AppRoute.Progress.route) {
                com.minlish.app.core.utils.MinLishLog.nav("Progress")
                val vm: ProgressViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.load() }
                ProgressDashboardScreen(state, vm::load)
            }
            composable(AppRoute.Settings.route) {
                com.minlish.app.core.utils.MinLishLog.nav("Settings")
                SettingsScreen(me, { navController.navigate(AppRoute.Profile.route) }, { navController.navigate(AppRoute.Notifications.route) }, { navController.navigate(AppRoute.Practice.route) }, { navController.navigate(AppRoute.WordHelp.route()) }, logout)
            }
            composable(AppRoute.Profile.route) {
                val vm: ProfileViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(state) {
                    if (state is UiState.Success) {
                        refreshSession()
                        navController.popBackStack()
                    }
                }
                // ── CHANGED: truyền thêm onLogout = logout ──
                ProfileSetupScreen(state, me.profile, vm::save, onLogout = logout)
            }
            composable(AppRoute.Practice.route) {
                com.minlish.app.core.utils.MinLishLog.nav("Practice")
                val vm: PracticeViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                PracticeScreen(state, vm::generate, vm::submit, vm::next, { navController.navigate(AppRoute.PracticeHistory.route) }, navController::popBackStack)
            }
            composable(AppRoute.PracticeHistory.route) {
                val vm: PracticeViewModel = viewModel(factory = factory)
                val state by vm.history.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.loadHistory() }
                PracticeHistoryScreen(state, vm::loadHistory, navController::popBackStack)
            }
            composable(AppRoute.Notifications.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                NotificationSettingsScreen(me.profile, state, vm::saveNotifications, vm::registerPlaceholderDevice, vm::testNotification, navController::popBackStack)
            }
            composable(AppRoute.WordHelp.route, arguments = listOf(navArgument("word") { defaultValue = "" })) { entry ->
                val vm: AgentViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                WordHelpScreen(state, entry.arguments?.getString("word").orEmpty(), vm::explain, { navController.navigate(AppRoute.GenerateExamples.route) }, { navController.navigate(AppRoute.GenerateDeck.route) }, navController::popBackStack)
            }
            composable(AppRoute.GenerateExamples.route) {
                val vm: AgentViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                GenerateExamplesScreen(state, vm::examples, navController::popBackStack)
            }
            composable(AppRoute.GenerateDeck.route) {
                val vm: AgentViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                GenerateDeckScreen(state, vm::deck, navController::popBackStack)
            }
        }
    }
}
