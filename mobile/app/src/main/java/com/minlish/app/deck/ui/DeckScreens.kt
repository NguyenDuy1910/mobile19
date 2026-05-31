package com.minlish.app.deck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.minlish.app.app.DeckDetailData
import com.minlish.app.core.model.DeckDto
import com.minlish.app.core.model.VocabItemDto
import com.minlish.app.core.network.UiState
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishTheme

@Composable
fun DeckListScreen(state: UiState<List<DeckDto>>, onRetry: () -> Unit, onCreate: () -> Unit, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Scaffold(
        topBar = { MinLishTopBar("My decks", subtitle = "Organize words your way") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Default.Add, "Create deck") },
                text = { Text("New deck") },
            )
        },
    ) { padding ->
        when (state) {
            UiState.Idle, UiState.Loading -> LoadingState("Loading decks...")
            is UiState.Error -> ErrorState(state.message, onRetry)
            UiState.Empty -> EmptyState("Create your first deck", "Group words by goal, topic, or exam.", "Create deck", onCreate, emoji = "🗂️")
            is UiState.Success -> {
                val filtered = state.data.filter { it.name.contains(query, ignoreCase = true) }
                Column(Modifier.padding(padding).padding(horizontal = Spacing.lg)) {
                    MinLishTextField(query, { query = it }, "Search decks", leadingIcon = Icons.Default.Search)
                    Spacer(Modifier.height(Spacing.md))
                    if (filtered.isEmpty()) {
                        EmptyState("No matches", "No decks match \"$query\".", emoji = "🔍")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                            items(filtered, key = DeckDto::id) { deck -> DeckRow(deck) { onOpen(deck.id) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckRow(deck: DeckDto, onOpen: () -> Unit) {
    val theme = deckThemeFor(deck.name, deck.tags)
    MinLishCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(theme.icon, color = theme.color, size = 48)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(deck.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                deck.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                if (deck.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        deck.tags.take(3).forEach { MinLishChip(it, color = theme.color) }
                    }
                }
            }
            Text(theme.emoji, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun DeckEditorScreen(initial: DeckDto? = null, error: String? = null, onSave: (String, String, String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    Scaffold(topBar = { MinLishTopBar(if (initial == null) "Create deck" else "Edit deck", onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (name.isNotBlank()) {
                val theme = deckThemeFor(name, tags.split(",").map(String::trim))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    IconBadge(theme.icon, color = theme.color)
                    Text("Preview: ${theme.emoji} $name", style = MaterialTheme.typography.titleMedium)
                }
            }
            MinLishTextField(name, { name = it }, "Deck name")
            MinLishTextField(description, { description = it }, "Description", singleLine = false)
            MinLishTextField(tags, { tags = it }, "Tags separated by commas")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(if (initial == null) "Create deck" else "Save changes", onClick = { onSave(name, description, tags) }, enabled = name.isNotBlank())
        }
    }
}

@Composable
fun DeckDetailScreen(
    state: UiState<DeckDetailData>,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddWord: () -> Unit,
    onWord: (String) -> Unit,
    onLearn: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) ConfirmationDialog("Delete deck?", "This permanently removes this deck and its words.", { confirmDelete = false; onDelete() }, { confirmDelete = false })
    Scaffold(
        topBar = {
            MinLishTopBar("Deck details", onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit deck") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete deck") }
            }
        },
    ) { padding ->
        when (state) {
            UiState.Idle, UiState.Loading -> LoadingState("Loading deck...")
            UiState.Empty -> EmptyState("No words yet", "Add your first word to start learning.", "Add word", onAddWord, emoji = "✏️")
            is UiState.Error -> ErrorState(state.message, onRetry)
            is UiState.Success -> {
                val theme = deckThemeFor(state.data.deck.name, state.data.deck.tags)
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item {
                        GradientHeroCard(theme.run { listOf(color, color.copy(alpha = 0.78f)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                Text(theme.emoji, style = MaterialTheme.typography.displaySmall)
                                Column {
                                    Text(state.data.deck.name, style = MaterialTheme.typography.headlineSmall, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.ExtraBold)
                                    Text("${state.data.words.size} words", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            state.data.deck.description?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Spacing.sm))
                                Text(it, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item {
                        PrimaryButton("Start learning", onLearn, enabled = state.data.words.isNotEmpty(), icon = Icons.Default.PlayArrow)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SecondaryButton("Add word", onAddWord, Modifier.weight(1f), icon = Icons.Default.Add)
                            SecondaryButton("Import", onImport, Modifier.weight(1f), icon = Icons.Default.Upload)
                            SecondaryButton("Export", onExport, Modifier.weight(1f), icon = Icons.Default.Download)
                        }
                    }
                    item { MinLishSectionHeader("${state.data.words.size} words", icon = Icons.AutoMirrored.Filled.MenuBook) }
                    if (state.data.words.isEmpty()) {
                        item { EmptyState("No words yet", "Use manual entry or dictionary lookup.", "Add word", onAddWord, emoji = "✏️") }
                    } else {
                        items(state.data.words, key = { it.id }) { word -> WordRow(word) { onWord(word.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordRow(word: VocabItemDto, onClick: () -> Unit) {
    MinLishCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(word.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    word.partOfSpeech?.takeIf { it.isNotBlank() }?.let { PartOfSpeechBadge(it) }
                }
                word.phonetic?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(word.meaning, maxLines = 2, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!word.audioUrl.isNullOrBlank()) SpeakerButton()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeckListPreview() {
    MinLishTheme {
        DeckListScreen(
            UiState.Success(
                listOf(
                    DeckDto("1", "IELTS Basics", "Core exam vocabulary", listOf("IELTS")),
                    DeckDto("2", "Business English", "Meetings & emails", listOf("Business")),
                ),
            ), {}, {}, {},
        )
    }
}

@Preview(showBackground = true, name = "Deck card")
@Composable
private fun DeckCardPreview() {
    MinLishTheme {
        Column(Modifier.padding(Spacing.md)) {
            DeckRow(DeckDto("1", "IELTS Writing", "Academic essay words", listOf("IELTS", "Writing"))) {}
        }
    }
}

@Preview(showBackground = true, name = "Word card")
@Composable
private fun WordCardPreview() {
    MinLishTheme {
        Column(Modifier.padding(Spacing.md)) {
            WordRow(
                VocabItemDto(
                    "1", "d", "resilient",
                    meaning = "Able to recover quickly from difficulties.",
                    partOfSpeech = "adjective", phonetic = "/rɪˈzɪl.i.ənt/", audioUrl = "x",
                ),
            ) {}
        }
    }
}

