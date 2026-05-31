package com.minlish.app.agent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.minlish.app.app.AgentData
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors

@Composable
fun WordHelpScreen(state: AgentData, initialWord: String = "", onExplain: (String) -> Unit, onExamples: () -> Unit, onDeck: () -> Unit, onBack: () -> Unit) {
    var word by remember(initialWord) { mutableStateOf(initialWord) }
    Scaffold(topBar = { MinLishTopBar("AI word helper", onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                IconBadge(Icons.Default.AutoAwesome, color = MinLishColors.Accent, size = 52)
                Column {
                    Text("Ask MinLish about a word", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    AiBadge()
                }
            }
            MinLishTextField(word, { word = it }, "Word", leadingIcon = Icons.Default.Search)
            PrimaryButton("Explain word", { onExplain(word) }, enabled = word.isNotBlank() && !state.loading, icon = Icons.Default.AutoAwesome)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SecondaryButton("Examples", onExamples, Modifier.weight(1f))
                SecondaryButton("Deck ideas", onDeck, Modifier.weight(1f))
            }
            if (state.loading) { BouncingDots(color = MinLishColors.Accent); Text("MinLish is thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.explanation?.let { explanation ->
                MinLishCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(explanation.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        MinLishChip("${explanation.level} · ${explanation.goal}", color = MinLishColors.Accent)
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(explanation.simpleExplanation, style = MaterialTheme.typography.bodyLarge)
                }
                if (explanation.examples.isNotEmpty()) MinLishCard {
                    MinLishSectionHeader("Examples", icon = Icons.Default.FormatQuote)
                    Spacer(Modifier.height(Spacing.xs))
                    explanation.examples.forEach {
                        Text("\"$it\"", fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = Spacing.xxs))
                    }
                }
                ChipFlowSection("Collocations", explanation.collocations, MinLishColors.Info)
            }
        }
    }
}

@Composable
fun GenerateExamplesScreen(state: AgentData, onGenerate: (String) -> Unit, onBack: () -> Unit) {
    var word by remember { mutableStateOf("") }
    AgentToolLayout("Generate examples", onBack) {
        AiBadge()
        MinLishTextField(word, { word = it }, "Word", leadingIcon = Icons.Default.Search)
        PrimaryButton("Generate examples", { onGenerate(word) }, enabled = word.isNotBlank() && !state.loading, icon = Icons.Default.AutoAwesome)
        if (state.loading) BouncingDots(color = MinLishColors.Accent)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.examples?.let { result ->
            MinLishCard {
                MinLishChip("${result.level} · ${result.goal}", color = MinLishColors.Accent)
                Spacer(Modifier.height(Spacing.sm))
                result.examples.forEach {
                    Text("\"$it\"", fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = Spacing.xxs))
                }
            }
        }
    }
}

@Composable
fun GenerateDeckScreen(state: AgentData, onGenerate: (String) -> Unit, onBack: () -> Unit) {
    var topic by remember { mutableStateOf("") }
    AgentToolLayout("Generate deck ideas", onBack) {
        AiBadge()
        MinLishTextField(topic, { topic = it }, "Topic, such as IELTS or Business", leadingIcon = Icons.Default.Search)
        PrimaryButton("Generate suggestions", { onGenerate(topic) }, enabled = topic.isNotBlank() && !state.loading, icon = Icons.Default.AutoAwesome)
        if (state.loading) BouncingDots(color = MinLishColors.Accent)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.deck?.let { result ->
            MinLishCard {
                MinLishSectionHeader("${result.topic} suggestions", icon = Icons.Default.Lightbulb)
                Spacer(Modifier.height(Spacing.sm))
                result.suggestions.forEach {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        MinLishChip(it.word, color = MinLishColors.Primary)
                        Text(it.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(Spacing.xs))
                }
                Text("Saving generated decks is planned for a later backend endpoint.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AgentToolLayout(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = { MinLishTopBar(title, onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md), content = content)
    }
}
