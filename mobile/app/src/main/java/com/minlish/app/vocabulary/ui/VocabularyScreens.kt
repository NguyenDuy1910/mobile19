package com.minlish.app.vocabulary.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minlish.app.app.WordEditorState
import com.minlish.app.core.model.DictionaryWordDto
import com.minlish.app.core.model.VocabItemDto
import com.minlish.app.core.model.VocabItemRequest
import com.minlish.app.core.model.VocabItemUpdateRequest
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors

@Composable
fun WordDetailScreen(
    word: VocabItemDto,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHelp: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) ConfirmationDialog(
        "Delete word?",
        "This removes '${word.word}' and its review progress.",
        { confirmDelete = false; onDelete() },
        { confirmDelete = false },
    )
    Scaffold(
        topBar = {
            MinLishTopBar(word.word, onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit word") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete word") }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MinLishCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(word.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    if (!word.audioUrl.isNullOrBlank()) SpeakerButton()
                }
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    word.partOfSpeech?.takeIf { it.isNotBlank() }?.let { PartOfSpeechBadge(it) }
                    (word.pronunciation ?: word.phonetic)?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            WordSection("Meaning", word.meaning, Icons.AutoMirrored.Filled.MenuBook)
            word.example?.takeIf { it.isNotBlank() }?.let { QuoteCard(it) }
            WordSection("Description", word.descriptionEn)
            ChipFlowSection("Collocations", word.collocations, MinLishColors.Info)
            ChipFlowSection("Synonyms", word.synonyms, MinLishColors.Success)
            ChipFlowSection("Antonyms", word.antonyms, MinLishColors.Error)
            ChipFlowSection("Related words", word.relatedWords, MinLishColors.Accent)
            word.note?.takeIf { it.isNotBlank() }?.let {
                MinLishCard {
                    MinLishSectionHeader("Your note")
                    Spacer(Modifier.height(Spacing.xs))
                    Text(it)
                }
            }
            Text("Source: ${word.source}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton("Ask AI to explain this word", onHelp, icon = Icons.Default.AutoAwesome)
        }
    }
}

@Composable
private fun WordSection(title: String, content: String?, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    if (!content.isNullOrBlank()) MinLishCard {
        MinLishSectionHeader(title, icon = icon)
        Spacer(Modifier.height(Spacing.xs))
        Text(content, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun QuoteCard(example: String) {
    MinLishCard {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(example, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun WordEditorScreen(
    editor: WordEditorState,
    initial: VocabItemDto? = null,
    onBack: () -> Unit,
    onLookup: (String) -> Unit,
    onCreate: (VocabItemRequest, Boolean) -> Unit,
    onUpdate: (String, VocabItemUpdateRequest) -> Unit,
) {
    val lookup = editor.lookup

    // Tự động điền tất cả fields từ lookup result
    var word by remember(initial, lookup) {
        mutableStateOf(lookup?.word ?: initial?.word.orEmpty())
    }
    var meaning by remember(initial, lookup) {
        mutableStateOf(lookup?.meaning ?: initial?.meaning.orEmpty())
    }
    var pronunciation by remember(initial, lookup) {
        mutableStateOf(lookup?.phonetic ?: initial?.pronunciation.orEmpty())
    }
    var description by remember(initial, lookup) {
        mutableStateOf(lookup?.descriptionEn ?: initial?.descriptionEn.orEmpty())
    }
    var example by remember(initial, lookup) {
        mutableStateOf(lookup?.example ?: initial?.example.orEmpty())
    }
    var collocations by remember(initial, lookup) {
        mutableStateOf(
            lookup?.collocations?.joinToString("; ")
                ?: initial?.collocations?.joinToString("; ").orEmpty()
        )
    }
    var related by remember(initial, lookup) {
        mutableStateOf(
            lookup?.relatedWords?.joinToString("; ")
                ?: initial?.relatedWords?.joinToString("; ").orEmpty()
        )
    }
    var note by remember(initial) {
        mutableStateOf(initial?.note.orEmpty())
    }

    Scaffold(topBar = { MinLishTopBar(if (initial == null) "Add word" else "Edit word", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (initial == null) {
                MinLishCard {
                    MinLishSectionHeader("Look up a word", icon = Icons.Default.Search)
                    Spacer(Modifier.height(Spacing.sm))
                    MinLishTextField(word, { word = it }, "Word to look up")
                    Spacer(Modifier.height(Spacing.sm))
                    PrimaryButton(
                        "Look up word",
                        { onLookup(word) },
                        enabled = word.isNotBlank() && !editor.loading,
                        icon = Icons.Default.Search,
                    )
                    AnimatedVisibility(editor.loading) {
                        Column { Spacer(Modifier.height(Spacing.md)); BouncingDots() }
                    }
                }
            } else {
                MinLishTextField(word, { word = it }, "Word")
            }

            lookup?.let { LookupSummary(it) }

            MinLishTextField(meaning, { meaning = it }, "Meaning (tiếng Việt)", singleLine = false)
            MinLishTextField(pronunciation, { pronunciation = it }, "Pronunciation")
            MinLishTextField(description, { description = it }, "English description", singleLine = false)
            MinLishTextField(example, { example = it }, "Example sentence", singleLine = false)
            MinLishTextField(collocations, { collocations = it }, "Collocations separated by ;")
            MinLishTextField(related, { related = it }, "Related words separated by ;")
            MinLishTextField(note, { note = it }, "Personal note", singleLine = false)

            editor.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (editor.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            PrimaryButton(
                if (initial == null) "Save word" else "Save changes",
                onClick = {
                    val list: (String) -> List<String> = { value ->
                        value.split(";").map(String::trim).filter(String::isNotBlank)
                    }
                    if (initial == null) {
                        onCreate(
                            VocabItemRequest(
                                word = word,
                                meaning = meaning,
                                pronunciation = pronunciation.ifBlank { null },
                                descriptionEn = description.ifBlank { null },
                                example = example.ifBlank { null },
                                collocations = list(collocations),
                                relatedWords = list(related),
                                note = note.ifBlank { null },
                                partOfSpeech = lookup?.partOfSpeech,
                                phonetic = lookup?.phonetic,
                                audioUrl = lookup?.audioUrl,
                                synonyms = lookup?.synonyms.orEmpty(),
                                antonyms = lookup?.antonyms.orEmpty(),
                                source = if (lookup == null) "manual" else lookup.source,
                            ),
                            lookup != null,
                        )
                    } else {
                        onUpdate(
                            initial.id,
                            VocabItemUpdateRequest(
                                word,
                                pronunciation.ifBlank { null },
                                meaning,
                                description.ifBlank { null },
                                example.ifBlank { null },
                                list(collocations),
                                list(related),
                                note.ifBlank { null },
                            ),
                        )
                    }
                },
                enabled = word.isNotBlank() && meaning.isNotBlank() && !editor.loading,
            )
        }
    }
}

@Composable
private fun LookupSummary(word: DictionaryWordDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MinLishColors.PrimaryContainer),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "✅ Dictionary result",
                    style = MaterialTheme.typography.titleMedium,
                    color = MinLishColors.OnPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                if (!word.audioUrl.isNullOrBlank()) SpeakerButton()
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                word.partOfSpeech?.let { PartOfSpeechBadge(it) }
                word.phonetic?.let { Text(it, color = MinLishColors.OnPrimaryContainer) }
            }
            Text(word.meaning, color = MinLishColors.OnPrimaryContainer)
            word.descriptionEn?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MinLishColors.OnPrimaryContainer.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            ChipFlowSection("Synonyms", word.synonyms, MinLishColors.Success)
            ChipFlowSection("Antonyms", word.antonyms, MinLishColors.Error)
            Text(
                "Fields below are pre-filled — edit anything before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MinLishColors.OnPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}
